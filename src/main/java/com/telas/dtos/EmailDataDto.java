package com.telas.dtos;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.telas.shared.utils.TrimLowercaseDeserializer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class EmailDataDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -3963846843873646628L;

  @JsonDeserialize(using = TrimLowercaseDeserializer.class)
  private String email;
  private String template;
  private String subject;

  private Map<String, String> params = new HashMap<>();

  public EmailDataDto(String email, String template, String subject, Map<String, String> params) {
    this.email = email;
    this.template = template;
    this.subject = subject;
    this.params = params;
  }
}