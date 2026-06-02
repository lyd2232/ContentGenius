package com.contentgenius.agent.config;

import com.contentgenius.agent.tools.ChatAssistant;
import com.contentgenius.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotTopicSearcher {

    private static final int WEB_CONTEXT_MAX_LEN = 2500;

    private final ChatAssistant chatAssistant;
    private final StringRedisTemplate stringRedisTemplate;

    //联网搜索写法
    public String search(String topic) {
        if (!StringUtils.hasText(topic)) {
            return null;
        }
        try {
            String cacheKey = "agent:websearch:cache:" + DigestUtils.md5DigestAsHex(topic.trim().toLowerCase().getBytes());//写key
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);//读key看看有没有
            if (StringUtils.hasText(cached)) {
                return cached;//有的话直接返回上次内容
            }
            String summary = chatAssistant.chat(topic.trim());//没有就联网搜
            if (!StringUtils.hasText(summary)) {
                return null;
            }
            String normalized = summary.trim();//联网搜索的数据
            stringRedisTemplate.opsForValue().set(cacheKey, normalized, 3600, TimeUnit.SECONDS);//存缓存设置过期时间
            return normalized.length() <= WEB_CONTEXT_MAX_LEN
                    ? normalized
                    : normalized.substring(0, WEB_CONTEXT_MAX_LEN);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("联网搜索失败 topic={}: {}", topic, ex.getMessage());
            return null;
        }
    }
}
