package cn.mars.marsprinterhub.bambu.service;

import cn.mars.marsprinterhub.bambu.config.BambuVideoConfig;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class BambuVideoService {

    private static final List<String> PROXY_ENV_KEYS = List.of(
            "HTTP_PROXY", "HTTPS_PROXY", "ALL_PROXY",
            "http_proxy", "https_proxy", "all_proxy"
    );

    private static final String DEFAULT_USERNAME = "bblp";
    private static final String PLAYLIST_NAME = "stream.m3u8";
    private static final String SEGMENT_PATTERN = "segment_%05d.ts";
    private static final String ERROR_LOG = "ffmpeg-error.log";
    private static final String OUTPUT_LOG = "ffmpeg-output.log";
    private static final Pattern EXTINF_PATTERN = Pattern.compile("^#EXTINF:", Pattern.MULTILINE);
    private static final int STARTUP_BUFFER_SEGMENTS = 2;

    private final Map<String, LiveStreamContext> liveProcesses = new ConcurrentHashMap<>();
    private final BambuVideoConfig videoConfig;

    /**
     * 启动实时 HLS 推流
     */
    public synchronized boolean startLiveStream(String printerIp, String accessCode, String deviceId) {
        String rtspUrl = String.format("rtsps://%s:%s@%s:322/streaming/live/1",
                DEFAULT_USERNAME, accessCode, printerIp);
        return startLiveStream(rtspUrl, deviceId);
    }

    /**
     * 使用自定义 RTSP URL 启动实时 HLS 推流
     */
    public synchronized boolean startLiveStream(String rtspUrl, String deviceId) {
        if (isLiveRunning(deviceId)) {
            log.info("设备 {} 的流已在运行", deviceId);
            return true;
        }

        Path deviceDir = resolveDeviceOutput(deviceId);
        Path playlistPath = deviceDir.resolve(PLAYLIST_NAME);
        Path segmentPath = deviceDir.resolve(SEGMENT_PATTERN);

        try {
            prepareOutputDirectory(deviceDir);
        } catch (IOException e) {
            log.error("无法创建 HLS 目录: {}, error={}", deviceDir, e.getMessage(), e);
            return false;
        }

        List<String> command = buildFfmpegCommand(rtspUrl, playlistPath, segmentPath);
        Process process;
        try {
            ProcessBuilder builder = new ProcessBuilder(command)
                    .directory(deviceDir.toFile())
                    .redirectError(deviceDir.resolve(ERROR_LOG).toFile())
                    .redirectOutput(deviceDir.resolve(OUTPUT_LOG).toFile());
            clearProxyEnvironment(builder);
            process = builder.start();
        } catch (IOException e) {
            log.error("启动 ffmpeg 失败: deviceId={}, error={}", deviceId, e.getMessage(), e);
            scheduleCleanup(deviceDir);
            return false;
        }

        if (!awaitStartup(deviceId, process, deviceDir, command)) {
            return false;
        }

        LiveStreamContext context = new LiveStreamContext(process, deviceDir);
        liveProcesses.put(deviceId, context);
        monitorProcess(deviceId, context);
        return true;
    }

    /**
     * 停止推流并删除 HLS 文件
     */
    public synchronized boolean stopLiveStream(String deviceId) {
        LiveStreamContext context = liveProcesses.remove(deviceId);
        Path deviceDir = context != null ? context.outputDir() : resolveDeviceOutput(deviceId);

        if (context != null) {
            Process process = context.process();
            if (process.isAlive()) {
                process.destroy();
                try {
                    long waitMillis = Math.max(cleanupDelay().toMillis(), 500L);
                    if (!process.waitFor(waitMillis, TimeUnit.MILLISECONDS)) {
                        process.destroyForcibly();
                        process.waitFor();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("等待设备 {} 的 ffmpeg 退出时被中断", deviceId);
                }
            }
        }

        scheduleCleanup(deviceDir);
        return true;
    }

    /**
     * 流是否正在运行
     */
    public boolean isLiveRunning(String deviceId) {
        LiveStreamContext context = liveProcesses.get(deviceId);
        if (context == null) {
            return false;
        }
        Process process = context.process();
        if (!process.isAlive()) {
            liveProcesses.remove(deviceId);
            scheduleCleanup(context.outputDir());
            return false;
        }
        return true;
    }

    /**
     * 等待 HLS 播放列表至少包含指定数量的可播分片。
     * 这样比“只等首个 ts 文件”更稳，能避免播放器刚播出首帧就因为下一片尚未就绪而顿一下。
     *
     * @param deviceId         设备ID
     * @param timeoutMs        超时时间，单位毫秒
     * @param minimumSegments  播放列表中最少需要出现的分片数
     * @return true = 可播分片已满足要求, false = 超时
     */
    public boolean waitForReadyPlaylist(String deviceId, long timeoutMs, int minimumSegments) {
        Path playlist = resolveDeviceOutput(deviceId).resolve(PLAYLIST_NAME);
        int requiredSegments = Math.max(minimumSegments, 1);
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                if (playlistHasSegments(playlist, requiredSegments)) {
                    return true;
                }
            } catch (IOException e) {
                log.debug("读取设备 {} 播放列表失败，继续等待: {}", deviceId, e.getMessage());
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("等待 HLS 播放列表就绪被中断: {}", deviceId);
                return false;
            }
        }

        log.warn("设备 {} HLS 播放列表在 {}ms 内未达到 {} 个分片", deviceId, timeoutMs, requiredSegments);
        return false;
    }

    /**
     * 前端播放 URL
     */
    public String getStreamUrl(String deviceId) {
        return "/hls/" + deviceId + "/" + PLAYLIST_NAME;
    }

    public int recommendedStartupSegments() {
        return Math.max(1, Math.min(STARTUP_BUFFER_SEGMENTS, sanitizedPlaylistSize()));
    }

    @PreDestroy
    public void shutdownAll() {
        liveProcesses.keySet().forEach(this::stopLiveStream);
    }

    private Path resolveDeviceOutput(String deviceId) {
        return Paths.get(videoConfig.getHlsRootDir(), deviceId).toAbsolutePath().normalize();
    }

    private void prepareOutputDirectory(Path directory) throws IOException {
        Files.createDirectories(directory);
        try (Stream<Path> files = Files.list(directory)) {
            files.forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    log.warn("删除旧分片失败: {}, error={}", path, e.getMessage());
                }
            });
        }
    }

    private List<String> buildFfmpegCommand(String rtspUrl, Path playlistPath, Path segmentPath) {
        List<String> command = new ArrayList<>();
        command.add(videoConfig.getFfmpegPath());
        command.add("-y");
        command.add("-fflags");
        command.add("nobuffer");
        command.add("-flags");
        command.add("low_delay");
        if (videoConfig.isUseTcpTransport()) {
            command.add("-rtsp_transport");
            command.add("tcp");
        }
        command.add("-i");
        command.add(rtspUrl);
        command.add("-an");
        command.add("-fps_mode");
        command.add("passthrough");
        command.add("-flush_packets");
        command.add("1");

        if (videoConfig.isPreferCopyCodec()) {
            command.add("-c:v");
            command.add("copy");
        } else {
            command.add("-c:v");
            command.add("libx264");
            command.add("-preset");
            command.add("veryfast");
            command.add("-tune");
            command.add("zerolatency");

            int gopSize = sanitizedGopSize();
            if (gopSize > 0) {
                String gopValue = String.valueOf(gopSize);
                command.add("-g");
                command.add(gopValue);
                command.add("-keyint_min");
                command.add(gopValue);
                command.add("-sc_threshold");
                command.add("0");
            }
        }

        command.add("-f");
        command.add("hls");
        command.add("-hls_time");
        command.add(String.valueOf(sanitizedSegmentDuration()));
        command.add("-hls_list_size");
        command.add(String.valueOf(sanitizedPlaylistSize()));
        command.add("-hls_allow_cache");
        command.add(videoConfig.isAllowCache() ? "1" : "0");

        String hlsFlags = buildHlsFlags();
        if (!hlsFlags.isEmpty()) {
            command.add("-hls_flags");
            command.add(hlsFlags);
        }
        command.add("-hls_init_time");
        command.add("0");

        command.add("-hls_segment_filename");
        command.add(segmentPath.toString());
        command.add(playlistPath.toString());
        return command;
    }

    private String buildHlsFlags() {
        List<String> flags = new ArrayList<>();
        if (videoConfig.isDeleteExpiredSegments()) {
            flags.add("delete_segments");
        }
        flags.add("independent_segments");
        flags.add("temp_file");
        if (videoConfig.isAppendList()) {
            flags.add("append_list");
        }
        if (videoConfig.isOmitEndList()) {
            flags.add("omit_endlist");
        }
        return String.join("+", flags);
    }

    private boolean awaitStartup(String deviceId, Process process, Path deviceDir, List<String> command) {
        Duration probeTimeout = videoConfig.getStartupProbeTimeout();
        try {
            boolean exited = process.waitFor(probeTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (exited) {
                int exitCode = process.exitValue();
                log.error("ffmpeg 在启动阶段退出: deviceId={}, exitCode={}, command={}",
                        deviceId, exitCode, String.join(" ", command));
                logFfmpegError(deviceDir);
                scheduleCleanup(deviceDir);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("等待 ffmpeg 启动时被中断: deviceId={}", deviceId);
            scheduleCleanup(deviceDir);
            return false;
        }
        return true;
    }

    private void monitorProcess(String deviceId, LiveStreamContext context) {
        CompletableFuture.runAsync(() -> {
            try {
                int exitCode = context.process().waitFor();
                log.info("ffmpeg 进程结束: deviceId={}, exitCode={}", deviceId, exitCode);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("监控设备 {} ffmpeg 进程被中断", deviceId);
            } finally {
                liveProcesses.remove(deviceId, context);
                scheduleCleanup(context.outputDir());
            }
        });
    }

    private void scheduleCleanup(Path directory) {
        Duration delay = cleanupDelay();
        Runnable task = () -> {
            try {
                deleteRecursively(directory);
            } catch (IOException e) {
                log.warn("清理 HLS 目录失败: {}, error={}", directory, e.getMessage());
            }
        };

        if (delay.isZero() || delay.isNegative()) {
            task.run();
        } else {
            CompletableFuture.runAsync(task, CompletableFuture.delayedExecutor(delay.toMillis(), TimeUnit.MILLISECONDS));
        }
    }

    private void deleteRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(directory)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("删除 {} 失败: {}", path, e.getMessage());
                        }
                    });
        }
    }

    private void logFfmpegError(Path deviceDir) {
        Path errorLog = deviceDir.resolve(ERROR_LOG);
        if (!Files.exists(errorLog)) {
            log.error("未找到 ffmpeg 错误日志: {}", errorLog);
            return;
        }
        try {
            List<String> lines = Files.readAllLines(errorLog);
            int from = Math.max(lines.size() - 10, 0);
            List<String> tail = lines.subList(from, lines.size());
            log.error("ffmpeg 错误日志片段 ({}): {}", deviceDir.getFileName(), String.join(" | ", tail));
        } catch (IOException e) {
            log.error("读取 ffmpeg 错误日志失败: {}, error={}", errorLog, e.getMessage());
        }
    }

    private record LiveStreamContext(Process process, Path outputDir) {
    }

    private int sanitizedSegmentDuration() {
        return Math.max(videoConfig.getSegmentDuration(), 1);
    }

    private int sanitizedPlaylistSize() {
        return Math.max(videoConfig.getPlaylistSize(), 1);
    }

    private Duration cleanupDelay() {
        Duration delay = videoConfig.getCleanupDelay();
        if (delay == null || delay.isNegative()) {
            return Duration.ZERO;
        }
        return delay;
    }

    private int sanitizedGopSize() {
        return Math.max(videoConfig.getGopSize(), 0);
    }

    private void clearProxyEnvironment(ProcessBuilder builder) {
        Map<String, String> environment = builder.environment();
        PROXY_ENV_KEYS.forEach(environment::remove);
    }

    private boolean playlistHasSegments(Path playlist, int minimumSegments) throws IOException {
        if (!Files.exists(playlist) || Files.size(playlist) == 0) {
            return false;
        }

        String content = Files.readString(playlist);
        Matcher matcher = EXTINF_PATTERN.matcher(content);
        int extinfCount = 0;
        while (matcher.find()) {
            extinfCount++;
            if (extinfCount >= minimumSegments) {
                return true;
            }
        }
        return false;
    }
}
