package com.orbitvis.satellite.domain;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeoPoint {
    private double latDeg;
    private double lonDeg;
}
