package com.telas.services;

import com.telas.dtos.response.IpResponseDto;

import java.util.List;

public interface IpService {
  List<IpResponseDto> findAll();
}

