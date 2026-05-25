package com.telas.infra.security.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class TokenData {
    private UUID id;
    private String email;
}
