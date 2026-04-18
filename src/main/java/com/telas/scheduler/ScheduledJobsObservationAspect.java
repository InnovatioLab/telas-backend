package com.telas.scheduler;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ScheduledJobsObservationAspect {

    private final SchedulerJobRunService schedulerJobRunService;
    private final SchedulerPreRunNotificationService schedulerPreRunNotificationService;
    private final SchedulerJobRunContext schedulerJobRunContext;

    public ScheduledJobsObservationAspect(
            SchedulerJobRunService schedulerJobRunService,
            SchedulerPreRunNotificationService schedulerPreRunNotificationService,
            SchedulerJobRunContext schedulerJobRunContext) {
        this.schedulerJobRunService = schedulerJobRunService;
        this.schedulerPreRunNotificationService = schedulerPreRunNotificationService;
        this.schedulerJobRunContext = schedulerJobRunContext;
    }

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object aroundScheduledJob(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Scheduled scheduled = method.getAnnotation(Scheduled.class);
        if (scheduled == null) {
            return pjp.proceed();
        }
        String jobId = resolveJobId(method);
        boolean cronSchedule = isCronSchedule(scheduled);
        if (cronSchedule) {
            schedulerPreRunNotificationService.notifyCronStarting(jobId);
        }
        UUID runId = schedulerJobRunService.start(jobId);
        schedulerJobRunContext.begin(runId);
        try {
            Object result = pjp.proceed();
            schedulerJobRunService.finishSuccess(runId, schedulerJobRunContext.takeSummary());
            return result;
        } catch (Throwable t) {
            schedulerJobRunService.finishFailure(runId, t, schedulerJobRunContext.takeSummary());
            throw t;
        }
    }

    private static boolean isCronSchedule(Scheduled s) {
        if (hasText(s.fixedDelayString()) || hasText(s.fixedRateString())) {
            return false;
        }
        return hasText(s.cron());
    }

    private static boolean hasText(String v) {
        return v != null && !v.isBlank();
    }

    private static String resolveJobId(Method method) {
        SchedulerLock lock = method.getAnnotation(SchedulerLock.class);
        if (lock != null && lock.name() != null && !lock.name().isBlank()) {
            return lock.name();
        }
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }
}
