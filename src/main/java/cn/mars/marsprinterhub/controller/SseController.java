package cn.mars.marsprinterhub.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.mars.marsprinterhub.sse.service.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 实时通知接口
 */
@Slf4j
@RestController
@RequestMapping("/sse")
@Tag(name = "SSE", description = "实时通知接口")
public class SseController {

    @Resource
    private SseService sseService;

    @GetMapping(path = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "建立 SSE 连接")
    public SseEmitter connect() {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("建立SSE连接: userId={}", userId);
        return sseService.connect(userId);
    }
}
