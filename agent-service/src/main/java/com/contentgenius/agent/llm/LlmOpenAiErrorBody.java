package com.contentgenius.agent.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 映射 DashScope「兼容 OpenAI」接口返回的错误 JSON 结构。
 * <p>
 * 典型响应体示例：
 * <pre>
 * {
 *   "error": {
 *     "message": "Incorrect API key provided",
 *     "type": "invalid_request_error",
 *     "code": "invalid_api_key"
 *   }
 * }
 * </pre>
 * 由 {@link LlmApiException#from(int, String)} 用 Jackson 反序列化。
 */
@Data
// JSON 里多出来的字段（如 param、status）忽略，避免反序列化失败
@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmOpenAiErrorBody {

    // 对应 JSON 根节点的 "error" 对象
    private ErrorDetail error;

    /**
     * error 节点内部的字段，与 OpenAI/DashScope 文档对齐。
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorDetail {

        // 给人看的错误说明，如 "Rate limit exceeded"
        private String message;

        // 错误大类，如 invalid_request_error
        private String type;

        // 机器可读错误码，如 invalid_api_key、Throttling
        private String code;
    }
}
