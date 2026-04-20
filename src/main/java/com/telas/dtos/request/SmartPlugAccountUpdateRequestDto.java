package com.telas.dtos.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmartPlugAccountUpdateRequestDto {

    private String accountEmail;

    private String password;

    private Boolean enabled;
}
