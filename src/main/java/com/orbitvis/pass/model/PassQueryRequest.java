package com.orbitvis.pass.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassQueryRequest {

    private UUID satelliteId;

    private SitePoint site;

    private Instant startTime;

    private Instant endTime;

    /** Minimum elevation in degrees for a pass to be considered visible. */
    private double minElevationDeg;
}

