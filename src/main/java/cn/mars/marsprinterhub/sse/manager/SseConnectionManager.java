package cn.mars.marsprinterhub.sse.manager;

import cn.mars.marsprinterhub.sse.message.SseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * SSE 连接管理器
 * 管理活跃的 SSE 连接，并按用户推送通知事件。
 */
@Slf4j
@Component
public class SseConnectionManager {

    private static final long SSE_TIMEOUT = 30L * 60 * 1000;

    private final Map<String, SseEmitter> connections = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();

    public SseEmitter addConnection(Long userId) {
        String sessionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        connections.put(sessionId, emitter);
        userSessions.computeIfAbsent(userId, key -> new CopyOnWriteArraySet<>()).add(sessionId);

        emitter.onCompletion(() -> removeConnection(userId, sessionId));
        emitter.onTimeout(() -> removeConnection(userId, sessionId));
        emitter.onError(error -> removeConnection(userId, sessionId));

        if (!send(sessionId, emitter, SseMessage.createSystemMessage("connected", "连接成功"))) {
            removeConnection(userId, sessionId);
        }

        return emitter;
    }

    public void sendToUser(Long userId, SseMessage<?> message) {
        Set<String> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        for (String sessionId : sessions) {
            SseEmitter emitter = connections.get(sessionId);
            if (emitter == null || !send(sessionId, emitter, message)) {
                removeConnection(userId, sessionId);
            }
        }
    }

    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        SseMessage<String> heartbeat = SseMessage.createSystemMessage("heartbeat", "ping");
        for (Map.Entry<String, SseEmitter> entry : connections.entrySet()) {
            String sessionId = entry.getKey();
            SseEmitter emitter = entry.getValue();
            if (!send(sessionId, emitter, heartbeat)) {
                removeConnection(sessionId);
            }
        }
    }

    private boolean send(String sessionId, SseEmitter emitter, SseMessage<?> message) {
        try {
            emitter.send(SseEmitter.event().name("message").data(message));
            return true;
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE消息发送失败: sessionId={}, error={}", sessionId, e.getMessage());
            return false;
        }
    }

    private void removeConnection(String sessionId) {
        Long userId = findUserIdBySessionId(sessionId);
        if (userId != null) {
            removeConnection(userId, sessionId);
        } else {
            connections.remove(sessionId);
        }
    }

    private void removeConnection(Long userId, String sessionId) {
        connections.remove(sessionId);

        Set<String> sessions = userSessions.get(userId);
        if (sessions == null) {
            return;
        }

        sessions.remove(sessionId);
        if (sessions.isEmpty()) {
            userSessions.remove(userId);
        }
    }

    private Long findUserIdBySessionId(String sessionId) {
        for (Map.Entry<Long, Set<String>> entry : userSessions.entrySet()) {
            if (entry.getValue().contains(sessionId)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
