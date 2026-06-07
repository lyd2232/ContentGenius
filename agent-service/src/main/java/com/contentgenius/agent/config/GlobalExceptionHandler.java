package com.contentgenius.agent.config;

import com.contentgenius.agent.llm.LlmErrorClassifier;
import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import com.contentgenius.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice//捕获全局异常
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException ex) {
        return jsonResponse(HttpStatus.BAD_REQUEST, Result.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : ErrorCode.BAD_REQUEST.getMessage();
        return jsonResponse(HttpStatus.BAD_REQUEST, Result.fail(ErrorCode.BAD_REQUEST, message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return jsonResponse(HttpStatus.BAD_REQUEST, Result.fail(ErrorCode.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception ex) {
        log.error("未处理异常", ex);
        if (LlmErrorClassifier.findLlmApiException(ex) != null) {
            BusinessException mapped = LlmErrorClassifier.toBusinessException(ex);
            return jsonResponse(HttpStatus.BAD_REQUEST, Result.fail(mapped.getCode(), mapped.getMessage()));
        }
        if (LlmErrorClassifier.shouldFallback(ex)) {
            BusinessException mapped = LlmErrorClassifier.toBusinessException(ex);
            return jsonResponse(HttpStatus.BAD_REQUEST, Result.fail(mapped.getCode(), mapped.getMessage()));
        }
        return jsonResponse(HttpStatus.INTERNAL_SERVER_ERROR, Result.fail(ErrorCode.INTERNAL_ERROR, "服务器内部错误"));
    }

    /** SSE 请求的 Accept 常为 text/event-stream，错误响应须显式返回 JSON，避免 406/500 */
    private static ResponseEntity<Result<Void>> jsonResponse(HttpStatus status, Result<Void> body) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
