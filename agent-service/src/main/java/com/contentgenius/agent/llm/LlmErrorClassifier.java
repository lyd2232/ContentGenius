package com.contentgenius.agent.llm;

import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import dev.langchain4j.exception.TimeoutException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpConnectTimeoutException;

/**
 * 大模型调用失败后的「决策器」：根据 HTTP 状态码 / 网络异常类型判断：
 * <ul>
 *   <li>是否切换备胎模型 qwen-plus（{@link #shouldFallback}）</li>
 *   <li>若不切换，返回给前端什么 {@link BusinessException}（{@link #toBusinessException}）</li>
 * </ul>
 * 不再依赖异常 message 里的关键词匹配。
 */
public final class LlmErrorClassifier {

    // 工具类，禁止实例化
    private LlmErrorClassifier() {
    }

    /**
     * 沿 cause 链查找 {@link LlmApiException}。
     * LangChain4j 往往会再包一层 RuntimeException，真实信息在 cause 里。
     */
    public static LlmApiException findLlmApiException(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {//获取异常
            if (t instanceof LlmApiException llm) {//如果异常不在llmapi里面
                return llm;//返回这个json响应
            }
        }
        return null;
    }

    /**
     * 主模型（qwen-max）失败后，是否应再试备胎（qwen-plus）。
     *
     * @return true = 可以 fallback；false = 直接失败，不要换模型
     */
    public static boolean shouldFallback(Throwable ex) {
        // 优先：HTTP 层错误（拦截器抛出的 LlmApiException）
        LlmApiException llm = findLlmApiException(ex);
        if (llm != null) {
            return isRetryableHttpStatus(llm.getHttpStatus());//在这种情况下返回可重试
        }
        // 其次：根本没拿到 HTTP 响应（超时、连接失败）也允许重试一次
        return isRetryableNetwork(ex);
    }

    /**
     * 不宜 fallback 时，转成统一 {@link BusinessException}，由 GlobalExceptionHandler 返回 JSON。
     */
    public static BusinessException toBusinessException(Throwable ex) {
        LlmApiException llm = findLlmApiException(ex);
        if (llm != null) {
            return mapHttpError(llm);
        }
        if (isRetryableNetwork(ex)) {
            return new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 模型网络超时，请稍后重试");
        }
        return new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 模型调用失败，请稍后重试");
    }

    /**
     * 这些 HTTP 状态视为「临时故障」，换备胎模型有意义。
     * 401/400 等不在这里，走 false。
     */
    private static boolean isRetryableHttpStatus(int status) {
        return status == 429          // 限流
                || status == 500      // 服务端内部错误
                || status == 502      // 网关坏响应
                || status == 503      // 服务不可用
                || status == 504;     // 网关超时
    }

    /**
     * 请求未正常完成（连接/读超时），也允许 fallback 一次。
     */
    private static boolean isRetryableNetwork(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof TimeoutException              // LangChain4j 超时
                    || t instanceof SocketTimeoutException   // JDK 读超时
                    || t instanceof ConnectException         // 连接被拒绝等
                    || t instanceof HttpConnectTimeoutException) { // JDK 连接超时
                return true;
            }
        }
        return false;
    }

    /**
     * 根据 HTTP 状态码映射对前端的错误码与文案（主模型失败且不应 fallback 时调用）。
     */
    private static BusinessException mapHttpError(LlmApiException llm) {
        int status = llm.getHttpStatus();

        // 鉴权失败：Nacos api-key 错、过期等，换模型无意义
        if (status == 401 || status == 403) {
            return new BusinessException(ErrorCode.BAD_REQUEST,
                    "AI 模型 API Key 或鉴权有误（HTTP " + status + "）");
        }

        // 请求参数问题：模型名错、body 格式错等
        if (status == 400) {
            String detail = llm.getErrorMessage() != null ? llm.getErrorMessage() : "请求参数错误";
            return new BusinessException(ErrorCode.BAD_REQUEST, "AI 模型请求无效（HTTP 400）: " + detail);
        }

        // 限流：若走到 toBusinessException 说明已决定不 fallback（或 plus 也失败后的路径可复用文案）
        if (status == 429) {
            return new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 模型限流（HTTP 429），请稍后重试");
        }

        // 其它 5xx
        if (status >= 500) {
            return new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
                    "AI 模型服务异常（HTTP " + status + "），请稍后重试");
        }

        // 其它 4xx（402、404 等）
        String detail = llm.getErrorMessage() != null ? llm.getErrorMessage() : "未知错误";
        return new BusinessException(ErrorCode.BAD_REQUEST, "AI 模型调用失败（HTTP " + status + "）: " + detail);
    }
}
