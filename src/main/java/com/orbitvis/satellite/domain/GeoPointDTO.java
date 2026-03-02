package com.orbitvis.satellite.domain;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeoPointDTO {
    private double latDeg;
    private double lonDeg;
}
