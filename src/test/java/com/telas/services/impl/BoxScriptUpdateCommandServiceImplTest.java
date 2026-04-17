package com.telas.services.impl;

import com.telas.dtos.request.BoxScriptAckRequestDto;
import com.telas.entities.Box;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.monitoring.entities.BoxScriptUpdateCommandEntity;
import com.telas.monitoring.entities.BoxScriptUpdateCommandStatus;
import com.telas.monitoring.repositories.BoxScriptUpdateCommandEntityRepository;
import com.telas.repositories.BoxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoxScriptUpdateCommandServiceImplTest {

    @Mock private BoxRepository boxRepository;
    @Mock private BoxScriptUpdateCommandEntityRepository commandRepository;

    private BoxScriptUpdateCommandServiceImpl service;

    private final UUID boxId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new BoxScriptUpdateCommandServiceImpl(boxRepository, commandRepository);
        ReflectionTestUtils.setField(service, "configuredTargetVersion", "2.0.0");
        ReflectionTestUtils.setField(service, "configuredArtifactUrl", "https://example.com/a.zip");
        ReflectionTestUtils.setField(service, "configuredArtifactSha256", "deadbeef");
    }

    @Test
    void enqueue_persists_pending() {
        Box box = new Box();
        box.setId(boxId);
        when(boxRepository.findById(boxId)).thenReturn(Optional.of(box));
        when(commandRepository.existsByBox_IdAndStatus(boxId, BoxScriptUpdateCommandStatus.PENDING))
                .thenReturn(false);

        service.enqueue(boxId);

        ArgumentCaptor<BoxScriptUpdateCommandEntity> captor =
                ArgumentCaptor.forClass(BoxScriptUpdateCommandEntity.class);
        verify(commandRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(BoxScriptUpdateCommandStatus.PENDING);
        assertThat(captor.getValue().getTargetVersion()).isEqualTo("2.0.0");
    }

    @Test
    void enqueue_throws_when_pending_exists() {
        Box box = new Box();
        box.setId(boxId);
        when(boxRepository.findById(boxId)).thenReturn(Optional.of(box));
        when(commandRepository.existsByBox_IdAndStatus(boxId, BoxScriptUpdateCommandStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> service.enqueue(boxId)).isInstanceOf(BusinessRuleException.class);

        verify(commandRepository, never()).save(any());
    }
}
