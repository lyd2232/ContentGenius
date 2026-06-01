package com.contentgenius.agent.config;

import com.contentgenius.agent.client.GetLevel;
import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import com.contentgenius.common.result.Result;
import com.contentgenius.agent.dto.UserLevelDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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

    private static final String KEY_PREFIX = "chat:";
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
        return KEY_PREFIX + userId + ":" + day;
    }

    private static long secondsUntilEndOfDay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrowStart = now.toLocalDate().plusDays(1).atStartOfDay();
        return Math.max(1L, ChronoUnit.SECONDS.between(now, tomorrowStart));
    }
}
