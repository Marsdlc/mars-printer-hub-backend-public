package cn.mars.marsprinterhub.bambu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;
import java.time.Duration;

/**
 * 配置 Bambu 视频流转码相关参数
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "bambu.video")
public class BambuVideoConfig {

    /**
     * ffmpeg 可执行文件路径，默认从 PATH 中查找
     */
    private String ffmpegPath = "ffmpeg";

    /**
     * HLS 输出根目录
     */
    private String hlsRootDir = Paths.get(
            System.getProperty("java.io.tmpdir"),
            "mars-printer-hub",
            "hls"
    ).toString();

    /**
     * 片段时长（秒）
     */
    private int segmentDuration = 1;

    /**
     * 播放列表保留的分片数量
     */
    private int playlistSize = 5;

    /**
     * 是否使用 TCP 方式拉取 RTSP 流
     */
    private boolean useTcpTransport = true;

    /**
     * ffmpeg 启动探测超时时间
     */
    private Duration startupProbeTimeout = Duration.ofSeconds(3);

    /**
     * 停止推流后保留文件的时长
     */
    private Duration cleanupDelay = Duration.ofSeconds(2);

    /**
     * 是否在播放列表中删除过期分片
     * 默认开启，防止 HLS 分片文件无限累积占用磁盘空间
     */
    private boolean deleteExpiredSegments = true;

    /**
     * 是否在播放列表末尾追加而不是重写
     */
    private boolean appendList = false;

    /**
     * 是否在播放列表末尾省略 #EXT-X-ENDLIST，降低追帧延迟
     */
    private boolean omitEndList = true;

    /**
     * 是否允许 HLS 客户端缓存分片
     */
    private boolean allowCache = false;

    /**
     * GOP 大小，控制关键帧间隔（仅在转码时生效）
     */
    private int gopSize = 30;

    /**
     * 是否尝试直接拷贝编解码数据而不是转码
     */
    private boolean preferCopyCodec = false;
}
