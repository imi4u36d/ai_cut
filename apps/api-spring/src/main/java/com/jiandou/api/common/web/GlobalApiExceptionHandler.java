package com.jiandou.api.common.web;

import com.jiandou.api.common.exception.ApiException;
import com.jiandou.api.generation.exception.GenerationProviderException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局API异常处理器。
 */
@RestControllerAdvice
public class GlobalApiExceptionHandler {

    /**
     * 统一处理业务异常，保证接口返回结构一致。
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(
            ex.code(),
            ex.status().value(),
            ex.status().getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI(),
            OffsetDateTime.now()
        );
        return ResponseEntity.status(ex.status()).body(body);
    }

    /**
     * 生成 Provider 失败需要明确返回给调用方，不能被泛化成 500。
     */
    @ExceptionHandler(GenerationProviderException.class)
    public ResponseEntity<ApiErrorResponse> handleGenerationProviderException(
        GenerationProviderException ex,
        HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_GATEWAY;
        Map<String, Object> details = new LinkedHashMap<>();
        if (ex.httpStatus() > 0) {
            details.put("providerHttpStatus", ex.httpStatus());
        }
        if (ex.providerResponse() != null) {
            details.put("providerResponse", ex.providerResponse());
        }
        ApiErrorResponse body = new ApiErrorResponse(
            "provider_request_failed",
            status.value(),
            status.getReasonPhrase(),
            firstNonBlank(ex.getMessage(), "Provider request failed"),
            details,
            request.getRequestURI(),
            OffsetDateTime.now()
        );
        return ResponseEntity.status(status).body(body);
    }

    /**
     * 统一收口参数校验和业务前置校验失败场景。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(
            "bad_request",
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI(),
            OffsetDateTime.now()
        );
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * 统一处理请求体验证失败。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        String message = ex.getBindingResult().getAllErrors().stream()
            .findFirst()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .filter(value -> value != null && !value.isBlank())
            .orElse("请求参数校验失败");
        ApiErrorResponse body = new ApiErrorResponse(
            "validation_failed",
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            message,
            request.getRequestURI(),
            OffsetDateTime.now()
        );
        return ResponseEntity.badRequest().body(body);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
