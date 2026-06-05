package com.contentgenius.agent.config;

import com.contentgenius.agent.client.GetLevel;
import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import com.contentgenius.common.result.Result;
import com.contentgenius.agent.dto.UserLevelDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
public class RedisQueueService {

    private final GetLevel getLevel;
    private final StringRedisTemplate stringRedisTemplate;

    /** 每日额度 key（保持原格式）：chat:{userId}:{yyyyMMdd} */
    private static final String QUOTA_KEY_PREFIX = "chat:";
    /** 多轮稿件记忆 key：chat:memory:{memoryId}:{userId}，与额度 key 分离 */
    private static final String MEMORY_KEY_PREFIX = "chat:memory:";
    /** 会话创作主题 key：chat:memory:topic:{memoryId}:{userId} */
    private static final String MEMORY_TOPIC_KEY_PREFIX = "chat:memory:topic:";
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");


    public void incrementChatQuota(long userId) {
        //拿次数
        int dailyLimit = resolveDailyLimit(loadMemberLevel());

        String redisKey = buildChatQuotaKey(userId);
        Long usedCount = stringRedisTemplate.opsForValue().increment(redisKey);
        if (usedCount == null) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "额度服务暂时不可用");
        }
        if (usedCount == 1L) {
            stringRedisTemplate.expire(redisKey, secondsUntilEndOfDay(), TimeUnit.SECONDS);
        }
        //比较
        if (usedCount > dailyLimit) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "今日免费额度已用完（" + dailyLimit + " 次/天）"
            );
        }
    }

   //拿等级
    private Integer loadMemberLevel() {
        Result<UserLevelDto> result = getLevel.me();
        if (result == null || result.getData() == null) {
            return 0;
        }
        return result.getData().getMemberLevel();
    }

    //按等级赋值使用次数
    private int resolveDailyLimit(Integer memberLevel) {
        int level = memberLevel == null ? 0 : memberLevel;
        return switch (level) {
            case 0 -> 0;
            case 1 -> 3;
            default -> 10;
        };
    }

    private String buildChatQuotaKey(long userId) {
        String day = LocalDate.now().format(DAY_FORMAT);
        return QUOTA_KEY_PREFIX + userId + ":" + day;
    }

    private static long secondsUntilEndOfDay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrowStart = now.toLocalDate().plusDays(1).atStartOfDay();
        return Math.max(1L, ChronoUnit.SECONDS.between(now, tomorrowStart));
    }

   //写记忆方法
    public void loadmemoryid(String memaryid, String userid, String content) {
        if (!StringUtils.hasText(memaryid) || !StringUtils.hasText(userid)) {
            return;
        }
        String key = MEMORY_KEY_PREFIX + memaryid + ":" + userid;
        if (!StringUtils.hasText(content)) {
            return;
        }
        stringRedisTemplate.opsForValue().set(key, content, 7, TimeUnit.DAYS);
    }
    //读记忆方法
    public String getMemoryContent(String memoryId, String userId) {
        String key = MEMORY_KEY_PREFIX + memoryId + ":" + userId;
        return stringRedisTemplate.opsForValue().get(key);
    }

    public void saveMemoryTopic(String memoryId, String userId, String topic) {
        if (!StringUtils.hasText(memoryId) || !StringUtils.hasText(userId) || !StringUtils.hasText(topic)) {
            return;
        }
        String key = MEMORY_TOPIC_KEY_PREFIX + memoryId + ":" + userId;
        stringRedisTemplate.opsForValue().set(key, topic.trim(), 7, TimeUnit.DAYS);
    }

    public String getMemoryTopic(String memoryId, String userId) {
        String key = MEMORY_TOPIC_KEY_PREFIX + memoryId + ":" + userId;
        return stringRedisTemplate.opsForValue().get(key);
    }
}
