package com.orbitvis.pass.model;

import lombok.*;

import java.time.Instant;
import java.time.Duration;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassDto{
    private Instant riseTime;
    private Instant setTime;
    private double maxElevation;
    private Duration duration;
    private double azimuth;
}