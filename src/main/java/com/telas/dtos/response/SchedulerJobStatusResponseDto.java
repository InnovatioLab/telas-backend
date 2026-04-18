package com.telas.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerJobStatusResponseDto {

    private String jobId;
    private String title;
    private String scheduleKind;
    private String cronExpression;
    private String zone;
    private Long fixedDelayMillis;
    private Instant lastStartedAt;
    private Instant lastEndedAt;
    private String lastStatus;
    private Long lastDurationMillis;
    private Instant nextExecutionEstimated;
    private Map<String, Object> lastRunSummary;
}
