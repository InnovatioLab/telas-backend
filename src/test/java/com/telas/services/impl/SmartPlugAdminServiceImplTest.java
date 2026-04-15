package com.telas.services.impl;

import com.telas.entities.Monitor;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.monitoring.crypto.AesTextEncryptionService;
import com.telas.monitoring.entities.SmartPlugEntity;
import com.telas.monitoring.plug.SmartPlugClient;
import com.telas.monitoring.repositories.SmartPlugEntityRepository;
import com.telas.entities.Box;
import com.telas.repositories.BoxRepository;
import com.telas.repositories.MonitorRepository;
import com.telas.shared.constants.valitation.BoxValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.constants.valitation.MonitoringValidationMessages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartPlugAdminServiceImplTest {

    @Mock
    private SmartPlugEntityRepository smartPlugEntityRepository;

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private BoxRepository boxRepository;

    @Mock
    private AesTextEncryptionService encryptionService;

    @Mock
    private SmartPlugClient smartPlugClient;

    @InjectMocks
    private SmartPlugAdminServiceImpl service;

    private final UUID plugId = UUID.randomUUID();
    private final UUID monitorId = UUID.randomUUID();
    private final UUID boxId = UUID.randomUUID();
    private final UUID otherPlugId = UUID.randomUUID();

    @Test
    void assignToMonitor_throwsWhenPlugMissing() {
        when(smartPlugEntityRepository.findById(plugId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignToMonitor(plugId, monitorId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(MonitoringValidationMessages.SMART_PLUG_NOT_FOUND);
    }

    @Test
    void assignToMonitor_throwsWhenMonitorMissing() {
        SmartPlugEntity plug = new SmartPlugEntity();
        plug.setId(plugId);
        when(smartPlugEntityRepository.findById(plugId)).thenReturn(Optional.of(plug));
        when(monitorRepository.findById(monitorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignToMonitor(plugId, monitorId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(MonitorValidationMessages.MONITOR_NOT_FOUND);
    }

    @Test
    void assignToMonitor_throwsWhenMonitorAlreadyHasOtherPlug() {
        SmartPlugEntity plug = new SmartPlugEntity();
        plug.setId(plugId);
        Monitor monitor = new Monitor();
        monitor.setId(monitorId);
        SmartPlugEntity other = new SmartPlugEntity();
        other.setId(otherPlugId);
        other.setMonitor(monitor);

        when(smartPlugEntityRepository.findById(plugId)).thenReturn(Optional.of(plug));
        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));
        when(smartPlugEntityRepository.findByMonitor_Id(monitorId)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.assignToMonitor(plugId, monitorId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(MonitoringValidationMessages.SMART_PLUG_MONITOR_ALREADY_LINKED);
    }

    @Test
    void assignToMonitor_persistsWhenInventory() {
        SmartPlugEntity plug = new SmartPlugEntity();
        plug.setId(plugId);
        Monitor monitor = new Monitor();
        monitor.setId(monitorId);

        when(smartPlugEntityRepository.findById(plugId)).thenReturn(Optional.of(plug));
        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));
        when(smartPlugEntityRepository.findByMonitor_Id(monitorId)).thenReturn(Optional.empty());
        when(smartPlugEntityRepository.save(any(SmartPlugEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.assignToMonitor(plugId, monitorId).getMonitorId()).isEqualTo(monitorId);
        verify(smartPlugEntityRepository).save(plug);
    }

    @Test
    void unassign_setsMonitorNull() {
        SmartPlugEntity plug = new SmartPlugEntity();
        plug.setId(plugId);
        Monitor monitor = new Monitor();
        monitor.setId(monitorId);
        plug.setMonitor(monitor);

        when(smartPlugEntityRepository.findById(plugId)).thenReturn(Optional.of(plug));
        when(smartPlugEntityRepository.save(any(SmartPlugEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.unassign(plugId);
        assertThat(dto.getMonitorId()).isNull();
        assertThat(dto.getBoxId()).isNull();
        verify(smartPlugEntityRepository).save(plug);
    }

    @Test
    void assignToBox_throwsWhenBoxMissing() {
        SmartPlugEntity plug = new SmartPlugEntity();
        plug.setId(plugId);
        when(smartPlugEntityRepository.findById(plugId)).thenReturn(Optional.of(plug));
        when(boxRepository.findById(boxId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignToBox(plugId, boxId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(BoxValidationMessages.BOX_NOT_FOUND);
    }

    @Test
    void assignToBox_throwsWhenBoxAlreadyHasOtherPlug() {
        SmartPlugEntity plug = new SmartPlugEntity();
        plug.setId(plugId);
        Box box = new Box();
        box.setId(boxId);
        SmartPlugEntity other = new SmartPlugEntity();
        other.setId(otherPlugId);
        other.setBox(box);

        when(smartPlugEntityRepository.findById(plugId)).thenReturn(Optional.of(plug));
        when(boxRepository.findById(boxId)).thenReturn(Optional.of(box));
        when(smartPlugEntityRepository.findByBox_Id(boxId)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.assignToBox(plugId, boxId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(MonitoringValidationMessages.SMART_PLUG_BOX_ALREADY_LINKED);
    }

    @Test
    void assignToBox_clearsMonitorAndSetsBox() {
        SmartPlugEntity plug = new SmartPlugEntity();
        plug.setId(plugId);
        Monitor monitor = new Monitor();
        monitor.setId(monitorId);
        plug.setMonitor(monitor);
        Box box = new Box();
        box.setId(boxId);

        when(smartPlugEntityRepository.findById(plugId)).thenReturn(Optional.of(plug));
        when(boxRepository.findById(boxId)).thenReturn(Optional.of(box));
        when(smartPlugEntityRepository.findByBox_Id(boxId)).thenReturn(Optional.empty());
        when(smartPlugEntityRepository.save(any(SmartPlugEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.assignToBox(plugId, boxId);
        assertThat(dto.getBoxId()).isEqualTo(boxId);
        assertThat(dto.getMonitorId()).isNull();
        verify(smartPlugEntityRepository).save(plug);
    }
}
