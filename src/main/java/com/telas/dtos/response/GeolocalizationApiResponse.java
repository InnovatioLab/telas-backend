package com.telas.dtos.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeolocalizationApiResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 7545390761423366122L;

    private List<Result> results;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Result implements Serializable {
        @Serial
        private static final long serialVersionUID = -7332003891605871251L;

        private Geometry geometry;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Geometry implements Serializable {
        @Serial
        private static final long serialVersionUID = 2093184063377807084L;

        private Location location;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Location implements Serializable {
        @Serial
        private static final long serialVersionUID = 624565354143607219L;

        private Double lat;
        private Double lng;
    }
}
