package com.contentgenius.agent.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

/**
 * 调用大模型 HTTP 接口失败时抛出的业务异常（非 2xx）。
 * <p>
 * 由 {@link LlmHttpErrorInterceptor} 在 OkHttp 层创建；
 * 上层 {@link LlmErrorClassifier} 根据 {@link #httpStatus} 决定 fallback 或返回 400/503。
 */
@Getter
public class LlmApiException extends RuntimeException {//继承RuntimeException不用抛异常

    // 用于把响应 body 字符串解析成 {@link LlmOpenAiErrorBody}
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // HTTP 状态码，如 401、429、500（判断 fallback 的核心依据）
    private final int httpStatus;

    // 原始响应体字符串，便于日志排查
    private final String rawBody;

    // 从 JSON error.code 解析，可能为 null（body 非标准 JSON 时）
    private final String errorCode;

    // 从 JSON error.type 解析
    private final String errorType;

    // 从 JSON error.message 解析
    private final String errorMessage;

    /**
     * 直接构造；一般通过 {@link #from(int, String)} 工厂方法创建。
     */
    public LlmApiException(int httpStatus, String rawBody, String errorCode, String errorType, String errorMessage) {
        // 父类 RuntimeException 的 message，打日志 / 堆栈时可见
        super(buildMessage(httpStatus, errorCode, errorMessage, rawBody));
        this.httpStatus = httpStatus;
        this.rawBody = rawBody;
        this.errorCode = errorCode;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }

    /**
     * 根据 HTTP 状态码和响应体创建异常，并尝试解析 OpenAI 风格 JSON。
     *
     * @param httpStatus 如 response.code() 得到的 429
     * @param rawBody    响应 body 全文；可能为空字符串
     */
    public static LlmApiException from(int httpStatus, String rawBody) {
        String code = null;
        String type = null;
        String message = null;

        // body 有内容才尝试 JSON 解析
        if (rawBody != null && !rawBody.isBlank()) {
            try {
                // 反序列化为 LlmOpenAiErrorBody
                LlmOpenAiErrorBody parsed = MAPPER.readValue(rawBody, LlmOpenAiErrorBody.class);
                if (parsed.getError() != null) {
                    code = parsed.getError().getCode();
                    type = parsed.getError().getType();
                    message = parsed.getError().getMessage();
                }
            } catch (Exception ignored) {
                // 不是标准 JSON 时，把原文截断放进 message，避免丢信息
                message = rawBody.length() > 512 ? rawBody.substring(0, 512) : rawBody;
            }
        }

        return new LlmApiException(httpStatus, rawBody, code, type, message);
    }

    /**
     * 拼成简短的异常描述，写入 Throwable.getMessage()。
     */
    private static String buildMessage(int status, String code, String message, String rawBody) {
        StringBuilder sb = new StringBuilder("LLM HTTP ").append(status);
        if (code != null) {
            sb.append(" code=").append(code);
        }
        if (message != null) {
            sb.append(" message=").append(message);
        } else if (rawBody != null && !rawBody.isBlank()) {
            // 解析失败时附一段 body 预览
            sb.append(" body=").append(rawBody.length() > 200 ? rawBody.substring(0, 200) + "..." : rawBody);
        }
        return sb.toString();
    }
}
