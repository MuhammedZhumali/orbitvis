package com.orbitvis.satellite.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SatelliteResponse {
    private UUID satelliteId;
    private String name;
    private List<GeoPointDTO> polygon;
    private double latitude;
    private double longitude;
}
