package com.telas.services.impl;

import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.MonitorRepository;
import com.telas.services.monitor.MonitorMapQueryService;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MonitorServiceImplViewportTest {

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private MonitorRepository repository;

    @InjectMocks
    private MonitorMapQueryService monitorMapQueryService;

    private MonitorServiceImpl monitorService;

    @BeforeEach
    void setUp() {
        monitorService = new MonitorServiceImpl(null, monitorMapQueryService, null);
    }

    @Test
    void findAvailableMonitorsInViewport_rejectsWhenLatSpanExceedsMax() {
        assertThatThrownBy(() -> monitorService.findAvailableMonitorsInViewport(0.0, 0.6, -80.0, -79.9))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(MonitorValidationMessages.MONITOR_VIEWPORT_BOUNDS_INVALID);

        verify(repository, never())
                .findAvailableMonitorsInBounds(
                        anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(), any());
    }

    @Test
    void findAvailableMonitorsInViewport_rejectsWhenMinLatGreaterOrEqualMax() {
        assertThatThrownBy(() -> monitorService.findAvailableMonitorsInViewport(10.0, 10.0, -80.0, -79.9))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(MonitorValidationMessages.MONITOR_VIEWPORT_BOUNDS_INVALID);
    }
}
