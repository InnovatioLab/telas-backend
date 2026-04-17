package com.telas.controllers.impl;

import com.telas.dtos.request.BoxScriptAckRequestDto;
import com.telas.dtos.response.BoxScriptPendingCommandResponseDto;
import com.telas.services.BoxScriptUpdateCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("monitoring/box-script")
@Tag(name = "Monitoring box script", description = "Comandos de atualização do box-script (API key)")
@RequiredArgsConstructor
public class MonitoringBoxScriptControllerImpl {

    private final BoxScriptUpdateCommandService boxScriptUpdateCommandService;

    @GetMapping("/pending-command")
    @Operation(summary = "Comando de atualização pendente para a box (por endereço registado)")
    public ResponseEntity<BoxScriptPendingCommandResponseDto> pendingCommand(
            @RequestParam String boxAddress) {
        Optional<BoxScriptPendingCommandResponseDto> dto =
                boxScriptUpdateCommandService.pollPending(boxAddress);
        return dto.map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/commands/ack")
    @Operation(summary = "Confirma conclusão ou falha da atualização")
    public ResponseEntity<Void> acknowledge(@Valid @RequestBody BoxScriptAckRequestDto request) {
        boxScriptUpdateCommandService.acknowledge(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
