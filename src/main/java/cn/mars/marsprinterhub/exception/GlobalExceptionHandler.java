package cn.mars.marsprinterhub.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.mars.marsprinterhub.common.BaseResponse;
import cn.mars.marsprinterhub.common.ResultUtils;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.io.IOException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public void asyncRequestTimeoutHandler(
            AsyncRequestTimeoutException e,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        log.warn("Async request timeout: method={}, uri={}", request.getMethod(), request.getRequestURI());
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
    }

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException: {}", e.getMessage());
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RateLimitException.class)
    public BaseResponse<?> rateLimitExceptionHandler(HttpServletResponse response) {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        return ResultUtils.error(ErrorCode.TOO_MANY_REQUESTS, "操作过于频繁，请稍后再试");
    }


    @ExceptionHandler(NotLoginException.class)
    public Object notLoginException(
            NotLoginException e,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        log.error("NotLoginException: {}", e.getMessage());
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "未登录");
            return null;
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR);
    }

    @ExceptionHandler(NotPermissionException.class)
    public BaseResponse<?> notPermissionExceptionHandler(NotPermissionException e) {
        log.error("NotPermissionException: {}", e.getMessage());
        return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> handleValidException(MethodArgumentNotValidException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, errorMsg);
    }
}
