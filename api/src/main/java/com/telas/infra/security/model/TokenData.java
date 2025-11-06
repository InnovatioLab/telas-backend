package com.telas.infra.security.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenData {
    private Long id;
    private String email;
}