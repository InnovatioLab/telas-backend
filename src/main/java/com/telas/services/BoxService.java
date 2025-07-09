package com.telas.services;

import com.telas.dtos.request.BoxRequestDto;
import com.telas.dtos.response.BoxMonitorAdResponseDto;
import com.telas.dtos.response.BoxResponseDto;
import com.telas.dtos.response.StatusMonitorsResponseDto;

import java.util.List;
import java.util.UUID;

public interface BoxService {
  List<BoxResponseDto> findAll();

  void save(BoxRequestDto request, UUID boxId);

  List<BoxMonitorAdResponseDto> getMonitorsAdsByIp(String ip);

  void checkMonitorsHealth(List<StatusMonitorsResponseDto> responseList);
}