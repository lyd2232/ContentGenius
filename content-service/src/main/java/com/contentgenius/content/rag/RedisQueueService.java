package com.contentgenius.content.rag;

import com.contentgenius.common.rag.RagIndexJob;
import com.contentgenius.common.rag.RagIndexJobSerde;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisQueueService {

    private final StringRedisTemplate stringRedisTemplate;

    /** 生产者：定稿后入队（LPUSH JSON） */
    public void enqueue(RagIndexJob job) {
        stringRedisTemplate.opsForList().leftPush(RagIndexJob.QUEUE_KEY, RagIndexJobSerde.toJson(job));
    }
}
