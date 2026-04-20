package com.telas.services;

import com.telas.dtos.request.SmartPlugIngestRequestDto;

public interface SmartPlugIngestService {
    void ingest(SmartPlugIngestRequestDto dto);
}

