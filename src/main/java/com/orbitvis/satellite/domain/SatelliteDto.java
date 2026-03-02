package com.orbitvis.satellite.domain;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SatelliteDto {
    private UUID satelliteId;
    private String line1;
    private String line2;
    private double maxNadirOffAngle;
    private double minSunAngle;
    private double maxRollAngle;
}
