package com.telas.services;

import com.telas.dtos.request.HeartbeatRequestDto;

public interface BoxHeartbeatService {

    void record(HeartbeatRequestDto request);
}
