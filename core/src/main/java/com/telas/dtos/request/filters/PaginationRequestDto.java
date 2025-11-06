package com.telas.dtos.request.filters;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaginationRequestDto {
  private int page = 1;
  private int size = 10;
  private String sortBy = "firstName";
  private String sortDir = "desc";
}
