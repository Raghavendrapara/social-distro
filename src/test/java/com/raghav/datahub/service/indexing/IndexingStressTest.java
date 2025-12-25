package com.raghav.datahub.service.indexing;

import com.raghav.datahub.config.ExecutorConfig;
import com.raghav.datahub.domain.model.DataItem;
import com.raghav.datahub.domain.model.IndexingJob;
import com.raghav.datahub.domain.model.JobStatus;
import com.raghav.datahub.domain.model.Pod;
import com.raghav.datahub.domain.repository.IndexingJobRepository;
import com.raghav.datahub.domain.repository.PodIndexRepository;
import com.raghav.datahub.domain.repository.PodRepository;
import com.raghav.datahub.infrastructure.persistence.repository.JpaVectorChunkRepository;
import com.raghav.datahub.service.embedding.EmbeddingClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(ExecutorConfig.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "datahub.indexing.simulated-delay-ms=100",
        "datahub.indexing.chunk-size=1",
        "datahub.indexing.chunk-timeout-ms=5000"
})
class IndexingStressTest {

    @Autowired
    private IndexingService indexingService;


    @MockitoBean
    private IndexingMetrics metrics;

    @MockitoBean
    private PodRepository podRepository;

    @MockitoBean
    private PodIndexRepository podIndexRepository;

    @MockitoBean
    private IndexingJobRepository jobRepository;

    @MockitoBean
    private JpaVectorChunkRepository vectorChunkRepository;

    @MockitoBean
    private EmbeddingClient embeddingClient;

    @Test
    void stressTestVirtualThreads() {
        int itemCount = 5000;
        String podId = "stress-pod";

        List<DataItem> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            items.add(new DataItem("content-" + i));
        }

        Pod pod = new Pod(podId, "Stress Pod", "user-1", items);


        when(podRepository.findById(podId)).thenReturn(pod);
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(embeddingClient.generateEmbedding(anyString())).thenReturn(List.of(0.1, 0.2, 0.3));
        when(vectorChunkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));


        System.out.println("Starting stress test with " + itemCount + " virtual threads...");
        long start = System.currentTimeMillis();

        indexingService.startIndexing(podId);

        ArgumentCaptor<IndexingJob> jobCaptor = ArgumentCaptor.forClass(IndexingJob.class);

        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    verify(jobRepository, atLeast(1)).save(jobCaptor.capture());
                    List<IndexingJob> jobs = jobCaptor.getAllValues();
                    assertThat(jobs).isNotEmpty();

                    IndexingJob lastState = jobs.get(jobs.size() - 1);
                    assertThat(lastState.getStatus()).isEqualTo(JobStatus.COMPLETED);
                });

        long end = System.currentTimeMillis();
        long duration = end - start;

        System.out.println("Finished processing " + itemCount + " chunks in " + duration + "ms");

        assertThat(duration).isLessThan(4000);
    }
}