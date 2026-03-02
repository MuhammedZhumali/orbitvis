package com.orbitvis.satellite.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "satellite")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Satellite {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID satelliteId;

    private String line1;
    private String line2;
    private double maxNadirOffAngle;
    private double minSunAngle;
    private double maxRollAngle;
}
