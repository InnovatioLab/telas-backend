package com.telas.services;

import com.telas.dtos.response.BoxConnectivityProbeRowResponseDto;

import java.util.List;

public interface BoxConnectivityProbeService {

    List<BoxConnectivityProbeRowResponseDto> listProbeRows();

    void runScheduledProbes();
}
