package com.contentgenius.agent.llm;

import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * LangChain4j 调 DashScope 时使用的 OkHttp 拦截器（在 {@link com.contentgenius.agent.config.LLMConfig} 注册）。
 * <p>
 * 职责：在 HTTP 响应返回后、LangChain4j 继续处理前，检查状态码；
 * 若非 2xx，读取 body 并抛出 {@link LlmApiException}，供上层按状态码决策。
 * <p>
 * 注意：这是 okhttp3.Interceptor，与 Feign 的 {@code feign.RequestInterceptor} 无关。
 */
public class LlmHttpErrorInterceptor implements Interceptor {

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        // 继续执行本次 HTTP 请求（连接 DashScope compatible-mode 端点）
        Response response = chain.proceed(chain.request());

        // 2xx 表示成功，原样交给 LangChain4j 解析 choices / content
        if (response.isSuccessful()) {
            return response;
        }

        // 非 2xx：读出 body（只能读一次），用于 JSON 解析
        String body = response.body() != null ? response.body().string() : "";

        // 关闭响应，释放连接；后面会抛异常，不再把 response 交给调用方
        response.close();

        // 带上真实 HTTP 状态码和 body，抛结构化异常（会被 LangChain4j 包在 cause 链里向上传）
        throw LlmApiException.from(response.code(), body);
    }
}
