package com.telas.dtos.request;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AdRequestDto extends AttachmentRequestDto implements Serializable {
  @Serial
  private static final long serialVersionUID = 4107068995329945486L;

  UUID adRequestId;
}