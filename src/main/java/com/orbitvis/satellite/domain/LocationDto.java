package com.orbitvis.satellite.domain;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {
    private Long id;
    private String name;
    private double longitude;
    private double latitude;
    private double altitude;
    private List<GeoPointDTO> polygon;
}
