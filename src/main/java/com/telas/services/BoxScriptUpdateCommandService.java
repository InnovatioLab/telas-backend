package com.telas.services;

import com.telas.dtos.request.BoxScriptAckRequestDto;
import com.telas.dtos.response.BoxScriptPendingCommandResponseDto;

import java.util.Optional;
import java.util.UUID;

public interface BoxScriptUpdateCommandService {

    void enqueue(UUID boxId);

    Optional<BoxScriptPendingCommandResponseDto> pollPending(String boxAddress);

    void acknowledge(BoxScriptAckRequestDto request);
}
