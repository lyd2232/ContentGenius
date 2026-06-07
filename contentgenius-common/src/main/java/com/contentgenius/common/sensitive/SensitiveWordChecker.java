package com.contentgenius.common.sensitive;

import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import com.github.houbb.sensitive.word.core.SensitiveWordHelper;

/**
 * 与 agent 写稿共用的敏感词拦截（houbb sensitive-word）。
 */
public final class SensitiveWordChecker {

    private SensitiveWordChecker() {
    }

    public static void requireClean(String... texts) {
        if (texts == null) {
            return;
        }
        for (String text : texts) {
            if (text != null && SensitiveWordHelper.contains(text)) {
                throw new BusinessException(
                        ErrorCode.CONTENT_CONTAINS_SENSITIVE_WORDS,
                        "内容包含敏感词，请修改后重试。参考：" + SensitiveWordHelper.replace(text)
                );
            }
        }
    }
}
