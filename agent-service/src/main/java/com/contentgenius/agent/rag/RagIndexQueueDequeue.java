package com.contentgenius.agent.rag;

import com.contentgenius.common.rag.RagIndexJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RagIndexQueueDequeue {

    private final StringRedisTemplate stringRedisTemplate;

    /** 阻塞出队（BRPOP），超时无数据返回 null */
    public String blockingDequeue(long timeoutSeconds) {
        return stringRedisTemplate.opsForList()
                .rightPop(RagIndexJob.QUEUE_KEY, timeoutSeconds, TimeUnit.SECONDS);
    }
}
