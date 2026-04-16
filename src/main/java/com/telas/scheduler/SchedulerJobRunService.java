package com.telas.scheduler;

import com.telas.scheduler.model.SchedulerJobRunEntity;
import com.telas.scheduler.model.SchedulerJobRunStatus;
import com.telas.scheduler.repository.SchedulerJobRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchedulerJobRunService {

    private final SchedulerJobRunRepository schedulerJobRunRepository;

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
    public void finishSuccess(UUID runId) {
        schedulerJobRunRepository
                .findById(runId)
                .ifPresent(
                        row -> {
                            row.setEndedAt(Instant.now());
                            row.setStatus(SchedulerJobRunStatus.SUCCESS);
                            schedulerJobRunRepository.save(row);
                        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishFailure(UUID runId, Throwable error) {
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
