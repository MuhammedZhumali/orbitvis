package com.orbitvis.realtime.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeStateDto {
    private Instant t;
    private double latDeg;
    private double lonDeg;
    private double altMeters;
    private double ecefX;
    private double ecefY;
    private double ecefZ;
    private double azimuthDeg;
    private double elevtionDeg;
    private double rangeKm;
    private boolean inView;
}
