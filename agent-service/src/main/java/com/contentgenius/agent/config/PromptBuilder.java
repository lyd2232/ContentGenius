package com.contentgenius.agent.config;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.regex.Pattern;


@Component
public class PromptBuilder {

    private static final Pattern HTTP_URL = Pattern.compile("https?://\\S+");

    /** Nacos 未配置时的全局写稿规则兜底 */
    private static final String DEFAULT_BASE_INSTRUCTION = """
            你是自媒体内容创作助手。请根据平台写法与用户主题撰写一篇可直接发布的初稿。
            输出格式：先给一行标题，再写正文；不要解释你是 AI，不要输出与成稿无关的闲聊或步骤说明。
            """;

    /** 平台提示词兜底（Nacos 与 DB 都缺失时生效） */
    private static final String DEFAULT_HINT_XHS = "口语化、分段、适量 emoji，种草笔记风格";
    private static final String DEFAULT_HINT_WECHAT = "标题吸引人、小标题清晰、结尾引导互动，公众号长文风格";

    private final PromptProperties promptProperties;

    public PromptBuilder(PromptProperties promptProperties) {
        this.promptProperties = promptProperties;
    }

    public String buildSystemPrompt(String platform, String promptHint) {
        return buildSystemPrompt(platform, promptHint, null, null);
    }

    /**
     * @param platform   目标平台
     * @param promptHint 模板表
     * @param webContext 联网检索摘要
     * @param ragContext 向量库检索到的历史稿片段
     */
    public String buildSystemPrompt(String platform, String promptHint, String webContext, String ragContext) {
        String platformLine = StringUtils.hasText(platform) ? "目标平台：" + platform.trim() + "。\n" : "";
        String styleLine = "平台写法要求：" + resolvePromptHint(platform, promptHint) + "。\n";

        StringBuilder sb = new StringBuilder(resolveBaseInstruction())
                .append('\n')
                .append(platformLine)
                .append(styleLine);

        if (StringUtils.hasText(ragContext)) {
            sb.append("\n可参考以下历史稿片段（同平台、同作者风格，仅供语气与结构参考，勿照抄）：\n")
                    .append(ragContext.trim())
                    .append('\n');
        }

        if (StringUtils.hasText(webContext) && containsHttpUrl(webContext)) {
            sb.append("\n可参考以下联网检索摘要（含来源链接，注意时效并自行核实，勿照抄）：\n")
                    .append(webContext.trim())
                    .append('\n')
                    .append("""
                            
                            输出要求（参考链接，仅当上方摘要含真实 URL 时）：
                            1) 正文后可追加「参考链接」小节，每行一条完整 https:// 或 http:// 地址；
                            2) 只能使用摘要里出现过的链接，不得编造；
                            3) 禁止写 [链接1]、占位符或「这里应该放置链接」等说明性文字；凑不齐 URL 则不要输出该小节。
                            """);
        } else if (StringUtils.hasText(webContext)) {
            sb.append("\n可参考以下联网检索摘要（注意时效并自行核实，勿照抄）：\n")
                    .append(webContext.trim())
                    .append('\n');
        }

        sb.append("""
                
                通用约束：若无明确可用的 https:// 来源，不要输出「参考链接」小节，不要写任何链接占位符。
                """);

        return sb.toString().trim();
    }

    /** 拼进 UserMessage，对应用户本次 topic */
    public String buildUserPrompt(String topic) {
        if (!StringUtils.hasText(topic)) {
            throw new IllegalArgumentException("创作主题不能为空");
        }
        return "请围绕以下主题撰写初稿：\n" + topic.trim();
    }

    /** 创作主题稳定；创作要求作为本轮 instruction 拼入首版 UserPrompt */
    public String buildUserPromptWithRequirement(String creationTheme, String requirement) {
        if (!StringUtils.hasText(creationTheme)) {
            throw new IllegalArgumentException("创作主题不能为空");
        }
        String theme = creationTheme.trim();
        if (!StringUtils.hasText(requirement) || requirement.trim().equals(theme)) {
            return buildUserPrompt(theme);
        }
        return buildThemeAndRequirementBlock(theme, requirement.trim(), "创作要求");
    }

    /** 改稿时拼入「主题 + 修改要求」，避免只传短句导致模型忽略创作要求 */
    public String buildReviseRequirementBlock(String creationTheme, String instruction) {
        if (!StringUtils.hasText(instruction)) {
            if (!StringUtils.hasText(creationTheme)) {
                return "请优化正文";
            }
            return "请围绕创作主题「" + creationTheme.trim() + "」优化正文";
        }
        String req = instruction.trim();
        if (!StringUtils.hasText(creationTheme) || creationTheme.trim().equals(req)) {
            return req;
        }
        return buildThemeAndRequirementBlock(creationTheme.trim(), req, "修改要求（须严格执行）");
    }

    private static String buildThemeAndRequirementBlock(String theme, String requirement, String requirementLabel) {
        return "创作主题：" + theme + "\n\n" + requirementLabel + "：\n" + requirement
                + "\n\n请确保正文完整体现以上要求。";
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
        String normalizedPlatform = normalizePlatform(platform);
        Map<String, String> nacosPlatformPrompt = promptProperties.getPlatform();

        if (nacosPlatformPrompt != null) {
            String fromNacos = nacosPlatformPrompt.get(normalizedPlatform);
            if (!StringUtils.hasText(fromNacos) && StringUtils.hasText(platform)) {
                fromNacos = nacosPlatformPrompt.get(platform.trim());
            }
            if (StringUtils.hasText(fromNacos)) {
                return fromNacos.trim();
            }
        }

        if (StringUtils.hasText(promptHint)) {
            return promptHint.trim();
        }

        return switch (normalizedPlatform) {
            case "wechat", "公众号" -> DEFAULT_HINT_WECHAT;
            case "xiaohongshu", "xhs", "小红书" -> DEFAULT_HINT_XHS;
            default -> DEFAULT_HINT_XHS;
        };
    }

    private String resolveBaseInstruction() {
        String fromNacos = promptProperties.getBaseInstruction();
        return StringUtils.hasText(fromNacos) ? fromNacos.trim() : DEFAULT_BASE_INSTRUCTION;
    }

    private static String normalizePlatform(String platform) {
        return StringUtils.hasText(platform) ? platform.trim().toLowerCase() : "";
    }

    private static boolean containsHttpUrl(String text) {
        return StringUtils.hasText(text) && HTTP_URL.matcher(text).find();
    }
}
