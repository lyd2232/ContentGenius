package com.contentgenius.agent.config;



import org.springframework.stereotype.Component;

import org.springframework.util.StringUtils;



/**

 * 将平台风格（prompt_hint）、用户主题拼成 System / User Prompt，供 ArticleWriter 使用。

 */

@Component

public class PromptBuilder {



    /** 所有平台共用的写稿规则（输出格式、禁止闲聊等） */

    private static final String BASE_INSTRUCTION = """

            你是自媒体内容创作助手。请根据平台写法与用户主题撰写一篇可直接发布的初稿。

            输出格式：先给一行标题，再写正文；不要解释你是 AI，不要输出与成稿无关的闲聊或步骤说明。

            """;



    /** content 拉模板失败时，小红书默认 hint（与 04-content-seed.sql 一致） */

    private static final String DEFAULT_HINT_XHS = "口语化、分段、适量 emoji，种草笔记风格";

    /** content 拉模板失败时，公众号默认 hint */

    private static final String DEFAULT_HINT_WECHAT = "标题吸引人、小标题清晰、结尾引导互动，公众号长文风格";



    public String buildSystemPrompt(String platform, String promptHint) {

        return buildSystemPrompt(platform, promptHint, null);

    }



    /**

     * @param platform   如 xiaohongshu

     * @param promptHint 来自 template 表，可为 null

     * @param ragContext 6/17+ 历史稿片段，6/9 传 null

     */

    public String buildSystemPrompt(String platform, String promptHint, String ragContext) {

        String platformLine = StringUtils.hasText(platform)

                ? "目标平台：" + platform.trim() + "。\n"

                : "";

        String styleLine = "平台写法要求：" + resolvePromptHint(platform, promptHint) + "。\n";



        StringBuilder sb = new StringBuilder(BASE_INSTRUCTION).append('\n').append(platformLine).append(styleLine);

        if (StringUtils.hasText(ragContext)) {

            sb.append("\n可参考以下历史稿件片段（勿照抄，仅作风格与事实参考）：\n")

                    .append(ragContext.trim())

                    .append('\n');

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



    /** 优先用库表 prompt_hint，否则按 platform 选内置默认 */

    private String resolvePromptHint(String platform, String promptHint) {

        if (StringUtils.hasText(promptHint)) {

            return promptHint.trim();

        }

        if (!StringUtils.hasText(platform)) {

            return DEFAULT_HINT_XHS;

        }

        return switch (platform.trim().toLowerCase()) {

            case "wechat", "公众号" -> DEFAULT_HINT_WECHAT;

            case "xiaohongshu", "xhs", "小红书" -> DEFAULT_HINT_XHS;

            default -> DEFAULT_HINT_XHS;

        };

    }

}


