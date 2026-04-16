package com.telas.scheduler.repository;

import com.telas.scheduler.model.SchedulerJobRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchedulerJobRunRepository extends JpaRepository<SchedulerJobRunEntity, UUID> {

    Optional<SchedulerJobRunEntity> findFirstByJobIdOrderByStartedAtDesc(String jobId);
}
