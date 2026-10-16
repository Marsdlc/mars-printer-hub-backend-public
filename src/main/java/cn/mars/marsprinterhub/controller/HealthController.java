package cn.mars.marsprinterhub.controller;

import cn.mars.marsprinterhub.common.BaseResponse;
import cn.mars.marsprinterhub.common.ResultUtils;
import cn.mars.marsprinterhub.service.AccessControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "健康检查相关接口")
public class HealthController {

    @Resource
    private AccessControlService accessControlService;

    @Value("${version:0}")
    private String version;

    @Value("${logging.file.path:./logs}")
    private String logPath;

    @Operation(summary = "健康检查")
    @GetMapping("/check")
    public String check() {
        return "ok";
    }

    @Operation(summary = "获取版本信息")
    @GetMapping("/version")
    public String version() {
        return version;
    }

    @Operation(summary = "获取日志文件列表")
    @GetMapping("/logs")
    public BaseResponse<List<Map<String, Object>>> listLogFiles() {
        accessControlService.checkAdminIntranet();
        List<Map<String, Object>> result = new ArrayList<>();
        Path logsDir = Paths.get(logPath);

        if (!Files.exists(logsDir) || !Files.isDirectory(logsDir)) {
            return ResultUtils.success(result);
        }

        try (Stream<Path> paths = Files.list(logsDir)) {
            result = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".log"))
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .map(p -> {
                        Map<String, Object> fileInfo = new HashMap<>();
                        fileInfo.put("name", p.getFileName().toString());
                        try {
                            fileInfo.put("size", Files.size(p));
                            fileInfo.put("lastModified", Files.getLastModifiedTime(p).toMillis());
                        } catch (IOException e) {
                            fileInfo.put("size", 0);
                            fileInfo.put("lastModified", 0);
                        }
                        return fileInfo;
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to list log files", e);
        }

        return ResultUtils.success(result);
    }

    @Operation(summary = "读取日志文件内容")
    @GetMapping("/logs/content")
    public BaseResponse<Map<String, Object>> readLogContent(
            @Parameter(description = "日志文件名") @RequestParam String filename,
            @Parameter(description = "读取行数，默认500行") @RequestParam(defaultValue = "500") int lines,
            @Parameter(description = "关键字过滤") @RequestParam(required = false) String keyword,
            @Parameter(description = "日志级别过滤 (ERROR, WARN, INFO, DEBUG)") @RequestParam(required = false) String level) {

        accessControlService.checkAdminIntranet();
        Map<String, Object> result = new HashMap<>();
        result.put("filename", filename);
        result.put("content", "");
        result.put("totalLines", 0);

        // 安全检查：防止路径遍历攻击
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            result.put("error", "Invalid filename");
            return ResultUtils.success(result);
        }

        Path logFile = Paths.get(logPath, filename);
        if (!Files.exists(logFile) || !Files.isRegularFile(logFile)) {
            result.put("error", "Log file not found");
            return ResultUtils.success(result);
        }

        try {
            // 读取文件最后N行（从文件末尾开始读取）
            List<String> lastLines = readLastLines(logFile, lines * 2); // 读取更多行以便过滤

            // 应用过滤条件
            List<String> filteredLines = lastLines.stream()
                    .filter(line -> {
                        // 日志级别过滤
                        if (level != null && !level.isEmpty()) {
                            String upperLevel = level.toUpperCase();
                            if (!line.contains(" " + upperLevel + " ")) {
                                return false;
                            }
                        }
                        // 关键字过滤
                        if (keyword != null && !keyword.isEmpty()) {
                            return line.toLowerCase().contains(keyword.toLowerCase());
                        }
                        return true;
                    })
                    .limit(lines)
                    .collect(Collectors.toList());

            result.put("content", String.join("\n", filteredLines));
            result.put("totalLines", filteredLines.size());
            result.put("fileSize", Files.size(logFile));

        } catch (IOException e) {
            log.error("Failed to read log file: {}", filename, e);
            result.put("error", "Failed to read log file: " + e.getMessage());
        }

        return ResultUtils.success(result);
    }

    /**
     * 从文件末尾读取最后N行（高效读取大文件）
     */
    private List<String> readLastLines(Path file, int numLines) throws IOException {
        List<String> lines = new ArrayList<>();

        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long fileLength = raf.length();
            if (fileLength == 0) {
                return lines;
            }

            StringBuilder builder = new StringBuilder();
            long pointer = fileLength - 1;
            int lineCount = 0;

            // 从文件末尾向前读取
            for (; pointer >= 0 && lineCount < numLines; pointer--) {
                raf.seek(pointer);
                int readByte = raf.readByte();

                if (readByte == '\n') {
                    if (builder.length() > 0) {
                        lines.add(0, builder.reverse().toString());
                        builder.setLength(0);
                        lineCount++;
                    }
                } else if (readByte != '\r') {
                    builder.append((char) readByte);
                }
            }

            // 添加第一行（如果有）
            if (builder.length() > 0 && lineCount < numLines) {
                lines.add(0, builder.reverse().toString());
            }
        }

        // 处理可能的编码问题，重新用UTF-8读取
        if (!lines.isEmpty()) {
            List<String> properLines;
            long startLine = Math.max(0, Files.lines(file).count() - numLines);
            try (Stream<String> stream = Files.lines(file, StandardCharsets.UTF_8)) {
                properLines = stream.skip(startLine).collect(Collectors.toList());
            } catch (IOException e) {
                // 如果UTF-8失败，返回原始读取结果
                return lines;
            }
            return properLines;
        }

        return lines;
    }
}
