package com.contentgenius.agent.config;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;


@Component
public class PromptBuilder {

    /** Nacos 未配置时的全局写稿规则兜底 */
    private static final String DEFAULT_BASE_INSTRUCTION = """
            你是自媒体内容创作助手。请根据平台写法与用户主题撰写一篇可直接发布的初稿。
            输出格式：先给一行标题，再写正文；不要解释你是 AI，不要输出与成稿无关的闲聊或步骤说明。
            """;

    /** 平台提示词兜底（Nacos 与 DB 都缺失时生效） */
    private static final String DEFAULT_HINT_XHS = "口语化、分段、适量 emoji，种草笔记风格";
    private static final String DEFAULT_HINT_WECHAT = "标题吸引人、小标题清晰、结尾引导互动，公众号长文风格";

    private final PromptProperties promptProperties;//引入nacos配置
//构造方法注入
    public PromptBuilder(PromptProperties promptProperties) {
        this.promptProperties = promptProperties;
    }

    public String buildSystemPrompt(String platform, String promptHint) {
        return buildSystemPrompt(platform, promptHint, null);
    }

    /**
     * @param platform   模板名称
     * @param promptHint 表中写法
     * @param ragContext 预留后续向量
     */
    public String buildSystemPrompt(String platform, String promptHint, String ragContext) {
        String platformLine = StringUtils.hasText(platform) ? "目标平台：" + platform.trim() + "。\n" : "";
        String styleLine = "平台写法要求：" + resolvePromptHint(platform, promptHint) + "。\n";//获取写法

        StringBuilder sb = new StringBuilder(resolveBaseInstruction())//加默认模板
                .append('\n')
                .append(platformLine)//添加平台
                .append(styleLine);//添加写法

        if (StringUtils.hasText(ragContext)) {//后续rag
            sb.append("\n可参考以下联网检索摘要（含来源链接，注意时效并自行核实，勿照抄）：\n")
                    .append(ragContext.trim())
                    .append('\n')
                    .append("""
                            
                            输出要求（强制）：
                            1) 正文写完后，必须追加一个“参考链接”小节；
                            2) 至少列出 3 条可访问 URL（每行一条）；
                            3) 只能使用上方联网检索摘要里出现过的链接，不得编造。
                            """);
        }

        return sb.toString().trim();
    }

    /** 拼进 UserMessage，对应用户本次 topic */
    public String buildUserPrompt(String topic) {
        if (!StringUtils.hasText(topic)) {
            throw new IllegalArgumentException("创作主题不能为空");
        }
        return "请围绕以下主题撰写初稿：\n" + topic.trim();
    }

    /**
     * 判断当前平台是否命中 Nacos 的平台提示词配置。
     */
    public boolean hasNacosPrompt(String platform) {
        Map<String, String> nacosPlatformPrompt = promptProperties.getPlatform();
        if (nacosPlatformPrompt == null || nacosPlatformPrompt.isEmpty()) {
            return false;
        }
        String normalizedPlatform = normalizePlatform(platform);
        String fromNacos = nacosPlatformPrompt.get(normalizedPlatform);
        if (!StringUtils.hasText(fromNacos) && StringUtils.hasText(platform)) {
            fromNacos = nacosPlatformPrompt.get(platform.trim());
        }
        return StringUtils.hasText(fromNacos);
    }

    /**
     * 选择nacos还是表还是默认
     */
    private String resolvePromptHint(String platform, String promptHint) {
        String normalizedPlatform = normalizePlatform(platform);//平台名
        Map<String, String> nacosPlatformPrompt = promptProperties.getPlatform();//获取nacos配置

        if (nacosPlatformPrompt != null) {//判空
            String fromNacos = nacosPlatformPrompt.get(normalizedPlatform);//获取value
            if (!StringUtils.hasText(fromNacos) && StringUtils.hasText(platform)) {
                fromNacos = nacosPlatformPrompt.get(platform.trim());
            }
            if (StringUtils.hasText(fromNacos)) {
                return fromNacos.trim();//nacos配置优先
            }
        }

        if (StringUtils.hasText(promptHint)) {
            return promptHint.trim();//表配置
        }

        return switch (normalizedPlatform) {//默认
            case "wechat", "公众号" -> DEFAULT_HINT_WECHAT;
            case "xiaohongshu", "xhs", "小红书" -> DEFAULT_HINT_XHS;
            default -> DEFAULT_HINT_XHS;
        };
    }

    private String resolveBaseInstruction() {
        String fromNacos = promptProperties.getBaseInstruction();//默认配置
        return StringUtils.hasText(fromNacos) ? fromNacos.trim() : DEFAULT_BASE_INSTRUCTION;
    }

    private static String normalizePlatform(String platform) {
        return StringUtils.hasText(platform) ? platform.trim().toLowerCase() : "";
    }
}
