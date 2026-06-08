package com.telas.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BoxAddressRequestDto implements Serializable {
  @Serial
  private static final long serialVersionUID = 3812945012387654321L;

  @NotBlank(message = "IP is required")
  @Size(max = 45, message = "IP must be at most 45 characters")
  private String ip;

  @NotBlank(message = "MAC address is required")
  @Size(max = 17, message = "MAC address must be at most 17 characters")
  private String mac;

  @Size(max = 253, message = "DNS must be at most 253 characters")
  private String dns;
}
