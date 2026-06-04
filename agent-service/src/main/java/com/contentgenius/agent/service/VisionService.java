package com.contentgenius.agent.service;

import com.contentgenius.agent.client.ContentFileClient;
import com.contentgenius.agent.dto.FileUploadResponse;
import com.contentgenius.agent.dto.VisionAnalyzeRequest;
import com.contentgenius.agent.dto.VisionAnalyzeResponse;
import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import com.contentgenius.common.result.Result;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VisionService {

    private static final String VISION_SYSTEM_PROMPT = """
            你是图文内容风格分析助手。用户会提供一张图片的 URL。
            请只分析该图的写作与版式风格（如平台体裁、标题习惯、分段、语气、emoji 密度、配图与文字关系等）。
            不要编造图中未出现的商品名、价格、品牌、具体事实；不要输出完整营销正文。
            用中文，条理清晰，控制在 400 字以内。
            """;

    private final ContentFileClient contentFileClient;
    private final ChatModel qwenVisionChatModel;

    public VisionService(
            ContentFileClient contentFileClient,
            @Qualifier("qwenVisionChatModel") ChatModel qwenVisionChatModel) {
        this.contentFileClient = contentFileClient;
        this.qwenVisionChatModel = qwenVisionChatModel;
    }

    public VisionAnalyzeResponse analyze(VisionAnalyzeRequest request) {
        FileUploadResponse file = loadOwnedFile(request.getObjectName().trim());//获取文件
        assertImageFile(file);//校验

        String imageUrl = file.getUrl();//拿minio文件地址
        if (!StringUtils.hasText(imageUrl)) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "无法获取图片访问地址");
        }

        String userText = buildUserPrompt(request.getPlatform());//走平台模板
        UserMessage userMessage = UserMessage.from(//构建用户message
                TextContent.from(userText),
                ImageContent.from(imageUrl)
        );
//视觉理解
        ChatResponse response = qwenVisionChatModel.chat(
                SystemMessage.from(VISION_SYSTEM_PROMPT),
                userMessage
        );
        String styleHint = response.aiMessage().text();
        if (!StringUtils.hasText(styleHint)) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "视觉模型未返回有效内容");
        }

        return new VisionAnalyzeResponse(
                file.getObjectName(),
                imageUrl,
                styleHint.trim()
        );
    }
//拿minio文件数据
    private FileUploadResponse loadOwnedFile(String objectName) {
        Result<FileUploadResponse> result = contentFileClient.load(objectName);
        if (result == null || result.getCode() != ErrorCode.SUCCESS.getCode()) {
            String msg = result != null && StringUtils.hasText(result.getMessage())
                    ? result.getMessage()
                    : "获取文件失败";
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, msg);
        }
        if (result.getData() == null || !StringUtils.hasText(result.getData().getUrl())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在或无权访问");
        }
        return result.getData();
    }
//校验类型
    private static void assertImageFile(FileUploadResponse file) {
        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType) && !contentType.toLowerCase().startsWith("image/")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持图片文件");
        }
    }
//构建用户提示如果有的话
    private static String buildUserPrompt(String platform) {
        if (!StringUtils.hasText(platform)) {
            return "请分析这张图片适合用什么写作风格与版式，便于用户仿写。";
        }
        return "目标平台：" + platform.trim()
                + "。请分析这张图片的写作风格、版式与语气，便于用户在该平台仿写。";
    }
}
