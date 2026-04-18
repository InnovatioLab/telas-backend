package com.telas.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telas.scheduler.model.SchedulerJobRunEntity;
import com.telas.scheduler.model.SchedulerJobRunStatus;
import com.telas.scheduler.repository.SchedulerJobRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class SchedulerJobRunService {

    private final SchedulerJobRunRepository schedulerJobRunRepository;
    private final ObjectMapper objectMapper;

    public SchedulerJobRunService(
            SchedulerJobRunRepository schedulerJobRunRepository, ObjectMapper objectMapper) {
        this.schedulerJobRunRepository = schedulerJobRunRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID start(String jobId) {
        SchedulerJobRunEntity row = new SchedulerJobRunEntity();
        row.setId(UUID.randomUUID());
        row.setJobId(jobId);
        row.setStartedAt(Instant.now());
        row.setStatus(SchedulerJobRunStatus.RUNNING);
        schedulerJobRunRepository.save(row);
        return row.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishSuccess(UUID runId, Map<String, Object> summary) {
        schedulerJobRunRepository
                .findById(runId)
                .ifPresent(
                        row -> {
                            row.setEndedAt(Instant.now());
                            row.setStatus(SchedulerJobRunStatus.SUCCESS);
                            row.setResultSummary(SchedulerJobResultSummarySupport.normalize(objectMapper, summary));
                            schedulerJobRunRepository.save(row);
                        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishFailure(UUID runId, Throwable error, Map<String, Object> summary) {
        schedulerJobRunRepository
                .findById(runId)
                .ifPresent(
                        row -> {
                            row.setEndedAt(Instant.now());
                            row.setStatus(SchedulerJobRunStatus.FAILED);
                            row.setErrorMessage(
                                    error != null && error.getMessage() != null
                                            ? truncate(error.getMessage(), 4000)
                                            : "failed");
                            row.setResultSummary(SchedulerJobResultSummarySupport.normalize(objectMapper, summary));
                            schedulerJobRunRepository.save(row);
                        });
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }
}
