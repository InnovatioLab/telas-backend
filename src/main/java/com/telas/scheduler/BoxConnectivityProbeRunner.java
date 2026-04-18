package com.telas.scheduler;

import com.telas.services.BoxConnectivityProbeService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BoxConnectivityProbeRunner {

    private final BoxConnectivityProbeService boxConnectivityProbeService;
    private final SchedulerJobRunService schedulerJobRunService;
    private final SchedulerJobRunContext schedulerJobRunContext;

    @SchedulerLock(name = "boxConnectivityProbe", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    public void runLocked() {
        UUID runId = schedulerJobRunService.start("boxConnectivityProbe");
        schedulerJobRunContext.begin(runId);
        try {
            boxConnectivityProbeService.runScheduledProbes();
            schedulerJobRunService.finishSuccess(runId, schedulerJobRunContext.takeSummary());
        } catch (Throwable t) {
            schedulerJobRunService.finishFailure(runId, t, schedulerJobRunContext.takeSummary());
            throw t;
        }
    }
}
