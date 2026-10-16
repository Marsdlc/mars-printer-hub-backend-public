package cn.mars.marsprinterhub.bambu.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Image服务 - 用于获取Bambu Lab打印机的截图
 * 支持设备：P1P, P1S, A1, A1MINI
 */
@Slf4j
@Service
public class BambuImageService {

    private static final List<String> PROXY_ENV_KEYS = List.of(
            "HTTP_PROXY", "HTTPS_PROXY", "ALL_PROXY",
            "http_proxy", "https_proxy", "all_proxy"
    );
    private static final String DEFAULT_USERNAME = "bblp";
    private static final int CHAMBER_IMAGE_PORT = 6000;
    private static final int READ_CHUNK_SIZE = 4096;

    // JPEG文件标识
    private static final byte[] JPEG_START = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0};
    private static final byte[] JPEG_END = {(byte) 0xff, (byte) 0xd9};

    /**
     * 从 H2D 摄像头抓取单帧 JPEG，返回 byte[]
     *
     * @param printerIp  摄像头 IP
     * @param accessCode 摄像头动态密码
     * @return 截图文件
     */
    public File captureImageByH2D(String printerIp, String accessCode) {
        String dirPath = "/app/tmp";
        File dir = new File(dirPath);
        if (!dir.exists() && !dir.mkdirs()) {
            log.error("无法创建临时目录: {}", dirPath);
            return null;
        }

        String filePath = dirPath + "/snapshot-" + System.currentTimeMillis() + ".png";
        File snapshotFile = new File(filePath);

        String rtspUrl = String.format("rtsps://%s:%s@%s:322/streaming/live/1",
                DEFAULT_USERNAME, accessCode, printerIp);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-rtsp_transport", "tcp",   // TCP 连接更稳定
                    "-i", rtspUrl,              // RTSP 地址
                    "-frames:v", "1",           // 抓取一帧
                    "-vcodec", "png",   // 输出 JPEG
                    "-y",                 // 自动覆盖
                    filePath
            );
            clearProxyEnvironment(pb);
            pb.redirectErrorStream(true);    // 错误信息输出到 stdout
            Process process = pb.start();

            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.error("FFmpeg 截图超时: {}", rtspUrl);
                return null;
            }

            if (!snapshotFile.exists() || snapshotFile.length() == 0) {
                log.error("FFmpeg 未生成有效图片: {}", rtspUrl);
                return null;
            }

            return snapshotFile;

        } catch (Exception e) {
            log.error("抓取 RTSP 截图失败: {}", rtspUrl, e);
            return null;
        }
    }

    /**
     * 获取打印机截图
     *
     * @param printerIp  打印机IP地址
     * @param accessCode 访问码
     * @return 图像byte数据
     */
    public byte[] captureImage(String printerIp, String accessCode) {
        log.info("开始获取打印机图像: IP={}", printerIp);

        try {
            // 创建SSL上下文，忽略证书验证
            SSLContext sslContext = createTrustAllSSLContext();
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();

            // 建立SSL连接
            try (SSLSocket sslSocket = (SSLSocket) socketFactory.createSocket(printerIp, CHAMBER_IMAGE_PORT)) {
                sslSocket.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
                sslSocket.startHandshake();

                // 发送认证数据
                sendAuthData(sslSocket, accessCode);

                // 接收图像数据
                return receiveImageData(sslSocket);

            }
        } catch (Exception e) {
            log.error("获取图像失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取图像失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建信任所有证书的SSL上下文
     */
    private SSLContext createTrustAllSSLContext() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        return sslContext;
    }

    /**
     * 发送认证数据
     */
    private void sendAuthData(SSLSocket socket, String accessCode) throws IOException {
        ByteBuffer authBuffer = ByteBuffer.allocate(80);
        authBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // 认证数据包格式
        authBuffer.putInt(0x40);    // '@'\0\0\0
        authBuffer.putInt(0x3000);  // \0'0'\0\0
        authBuffer.putInt(0);       // \0\0\0\0
        authBuffer.putInt(0);       // \0\0\0\0

        // 用户名 (32byte)
        byte[] usernameBytes = DEFAULT_USERNAME.getBytes();
        authBuffer.put(usernameBytes);
        authBuffer.put(new byte[32 - usernameBytes.length]); // 填充到32byte

        // 访问码 (32byte)
        byte[] accessCodeBytes = accessCode.getBytes();
        authBuffer.put(accessCodeBytes);
        authBuffer.put(new byte[32 - accessCodeBytes.length]); // 填充到32byte

        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(authBuffer.array());
        outputStream.flush();
    }

    /**
     * 接收图像数据
     */
    private byte[] receiveImageData(SSLSocket socket) throws IOException {
        InputStream inputStream = socket.getInputStream();
        socket.setSoTimeout(10000); // 10秒超时

        // 首先接收16byte头部信息
        byte[] header = new byte[16];
        int bytesRead = 0;
        while (bytesRead < 16) {
            int read = inputStream.read(header, bytesRead, 16 - bytesRead);
            if (read == -1) {
                throw new IOException("连接被打印机关闭，可能是访问码错误");
            }
            bytesRead += read;
        }

        // 解析图像大小（前3个byte，小端序）
        int imageSize = ByteBuffer.wrap(header, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFF;

        if (imageSize == 0 || imageSize > 10 * 1024 * 1024) { // 最大10MB
            throw new IOException("无效的图像大小: " + imageSize);
        }

        // 接收图像数据
        byte[] imageData = new byte[imageSize];
        bytesRead = 0;
        while (bytesRead < imageSize) {
            int read = inputStream.read(imageData, bytesRead, Math.min(READ_CHUNK_SIZE, imageSize - bytesRead));
            if (read == -1) {
                throw new IOException("图像数据接收不完整");
            }
            bytesRead += read;
        }

        // 验证JPEG格式
        if (!isValidJpeg(imageData)) {
            throw new IOException("接收到的不是有效的JPEG图像");
        }

        return imageData;
    }

    /**
     * 验证JPEG格式
     */
    private boolean isValidJpeg(byte[] data) {
        if (data.length < 4) {
            return false;
        }

        // 检查JPEG开始标记
        boolean hasJpegStart = Arrays.equals(Arrays.copyOfRange(data, 0, 4), JPEG_START);

        // 检查JPEG结束标记
        boolean hasJpegEnd =
                Arrays.equals(Arrays.copyOfRange(data, data.length - 2, data.length), JPEG_END);

        return hasJpegStart && hasJpegEnd;
    }

    // ==================== 流媒体相关方法 ====================

    // MJPEG流媒体边界标识
    private static final String BOUNDARY = "frame";

    // 固定帧率：每秒0.5帧（2秒间隔）
    private static final long FRAME_INTERVAL = 2000;

    /**
     * 流式传输MJPEG帧 - 完全复用现有的captureImage方法
     *
     * @param outputStream 输出流
     * @param printerIp    打印机IP地址
     * @param accessCode   访问码
     * @param isStreaming  流媒体状态控制
     */
    public void streamMjpegFrames(OutputStream outputStream, String printerIp, String accessCode, AtomicBoolean isStreaming) {
        int frameCount = 0;
        int errorCount = 0;
        final int maxErrors = 5; // 最大连续错误次数

        while (isStreaming.get() && errorCount < maxErrors) {
            try {
                // 在获取图像前先检测连接状态，避免无意义的图像获取
                if (!isConnectionAlive(outputStream)) {
                    log.error("检测到客户端连接已断开，停止获取图像");
                    break;
                }

                // 直接复用现有的captureImage方法
                byte[] imageData = captureImage(printerIp, accessCode);

                // 发送MJPEG帧
                sendMjpegFrame(outputStream, imageData);

                if (frameCount > 0) {
                    // 固定2秒间隔
                    Thread.sleep(FRAME_INTERVAL);
                }

                frameCount++;
                errorCount = 0; // 重置错误计数

            } catch (InterruptedException e) {
                log.error("流媒体传输被中断");
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                // IO异常通常表示客户端断开连接，直接退出
                log.error("客户端连接断开，停止流媒体传输: {}", e.getMessage());
                break;
            } catch (Exception e) {
                errorCount++;
                log.warn("获取图像失败 (错误次数: {}/{}): {}", errorCount, maxErrors, e.getMessage());

                if (errorCount < maxErrors) {
                    // 尝试发送错误帧，如果失败说明连接已断开
                    if (!sendErrorFrameSafely(outputStream, "获取图像失败，正在重试...")) {
                        log.error("无法发送错误帧，客户端可能已断开连接");
                        break;
                    }

                    try {
                        Thread.sleep(1000); // 错误后等待1秒再重试
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    /**
     * 根据deviceId流式传输MJPEG帧 - 包装方法，便于扩展
     *
     * @param outputStream 输出流
     * @param printerIp    打印机IP地址
     * @param accessCode   访问码
     * @param isStreaming  流媒体状态控制
     */
    public void streamMjpegFramesByDeviceId(OutputStream outputStream, String printerIp, String accessCode, AtomicBoolean isStreaming) throws IOException {
        // 复用现有的流媒体方法
        streamMjpegFrames(outputStream, printerIp, accessCode, isStreaming);
    }

    /**
     * 发送MJPEG帧
     */
    public void sendMjpegFrame(OutputStream outputStream, byte[] imageData) throws IOException {
        String frameHeader = "\r\n--" + BOUNDARY + "\r\n" +
                "Content-Type: image/jpeg\r\n" +
                "Content-Length: " + imageData.length + "\r\n\r\n";

        outputStream.write(frameHeader.getBytes());
        outputStream.write(imageData);
        outputStream.flush();
    }

    /**
     * 检测客户端连接是否仍然活跃
     * 通过尝试写入一个空的数据来检测连接状态，不破坏MJPEG流格式
     */
    private boolean isConnectionAlive(OutputStream outputStream) {
        try {
            // 只是尝试flush来检测连接状态，不写入任何数据
            outputStream.flush();
            return true;
        } catch (IOException e) {
            log.error("连接检测失败，客户端可能已断开: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 安全地发送错误提示帧，如果失败返回false表示连接已断开
     */
    public boolean sendErrorFrameSafely(OutputStream outputStream, String errorMessage) {
        try {
            String frameHeader = "\r\n--" + BOUNDARY + "\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: " + errorMessage.length() + "\r\n\r\n";

            outputStream.write(frameHeader.getBytes());
            outputStream.write(errorMessage.getBytes());
            outputStream.flush();
            return true;
        } catch (IOException e) {
            log.error("发送错误帧失败，客户端可能已断开连接: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 发送错误提示帧（保持向后兼容）
     */
    public void sendErrorFrame(OutputStream outputStream, String errorMessage) {
        sendErrorFrameSafely(outputStream, errorMessage);
    }

    /**
     * 获取MJPEG内容类型
     */
    public String getMjpegContentType() {
        return "multipart/x-mixed-replace; boundary=" + BOUNDARY;
    }

    private void clearProxyEnvironment(ProcessBuilder processBuilder) {
        Map<String, String> environment = processBuilder.environment();
        PROXY_ENV_KEYS.forEach(environment::remove);
    }
}
