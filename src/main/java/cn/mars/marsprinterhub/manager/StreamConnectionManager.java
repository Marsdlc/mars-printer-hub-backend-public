package cn.mars.marsprinterhub.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 流媒体连接管理器
 * 用于管理活跃的流媒体连接，支持主动断开
 */
@Slf4j
@Service
public class StreamConnectionManager {

    // 存储活跃的流媒体连接
    // Key: sessionId ( userId + "_" + deviceId)
    // Value: 流媒体控制标志
    private final ConcurrentHashMap<String, AtomicBoolean> activeStreams = new ConcurrentHashMap<>();

    /**
     * 创建新的流媒体会话
     *
     * @param deviceId 设备ID
     * @param userId   用户ID
     * @return 会话ID
     */
    public String createStreamSession(String deviceId, Long userId) {
        String sessionId = generateSessionId(deviceId, userId);
        AtomicBoolean streamingFlag = new AtomicBoolean(true);
        activeStreams.put(sessionId, streamingFlag);
        return sessionId;
    }

    /**
     * 获取流媒体控制标志
     *
     * @param sessionId 会话ID
     * @return 流媒体控制标志，如果会话不存在返回null
     */
    public AtomicBoolean getStreamingFlag(String sessionId) {
        return activeStreams.get(sessionId);
    }

    /**
     * 断开流媒体连接
     *
     * @param sessionId 会话ID
     */
    public void disconnectStream(String sessionId) {
        AtomicBoolean streamingFlag = activeStreams.get(sessionId);
        if (streamingFlag != null) {
            streamingFlag.set(false);
            activeStreams.remove(sessionId);
        }
    }

    /**
     * 清理已结束的流媒体会话
     *
     * @param sessionId 会话ID
     */
    public void cleanupSession(String sessionId) {
        activeStreams.remove(sessionId);
    }

    /**
     * 生成会话ID
     *
     * @param deviceId 设备ID
     * @param userId   用户ID
     * @return 会话ID
     */
    private String generateSessionId(String deviceId, Long userId) {
        return userId + "_" + deviceId;
    }
}