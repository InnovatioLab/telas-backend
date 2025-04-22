package com.marketingproject.dtos.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeolocalizationResponseDto implements Serializable {

    @Serial
    private static final long serialVersionUID = -856233043255432157L;

    private Double lat;
    private Double lng;
}
