package com.contentgenius.agent.rag;

import com.contentgenius.agent.config.QdrantStorage;
import com.contentgenius.common.rag.RagIndexJob;
import com.contentgenius.common.rag.RagIndexJobSerde;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagIndexConsumer {

    private final RagIndexQueueDequeue ragIndexQueueDequeue;
    private final QdrantStorage qdrantStorage;

    @Scheduled(fixedDelay = 1000)
    public void consumeOne() {
        String payload = ragIndexQueueDequeue.blockingDequeue(1);
        if (payload == null) {
            return;
        }
        try {
            RagIndexJob job = RagIndexJobSerde.fromJson(payload);
            if (!isValid(job)) {
                log.warn("跳过无效 RAG 入队任务: {}", payload);
                return;
            }
            qdrantStorage.ingest(
                    job.getPlatform().trim(),
                    job.getUserId(),
                    job.getVersionId(),
                    job.getContent());
            log.info("RAG 入库完成 versionId={} userId={} platform={}",
                    job.getVersionId(), job.getUserId(), job.getPlatform());
        } catch (Exception e) {
            log.error("RAG 入库失败 payload={}: {}", payload, e.getMessage(), e);
        }
    }

    private static boolean isValid(RagIndexJob job) {
        return job != null
                && job.getVersionId() != null
                && job.getUserId() != null
                && StringUtils.hasText(job.getPlatform())
                && StringUtils.hasText(job.getContent());
    }
}
