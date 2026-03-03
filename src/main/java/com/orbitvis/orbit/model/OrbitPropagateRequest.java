package com.orbitvis.orbit.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrbitPropagateRequest {
    private String line1;
    private String line2;
    /** Unix epoch seconds, or -1 for TLE epoch */
    private long startEpoch = -1L;
    /** Unix epoch seconds, or -1 for start + 90 min */
    private long endEpoch = -1L;
    private long stepSeconds = 60L;
}
