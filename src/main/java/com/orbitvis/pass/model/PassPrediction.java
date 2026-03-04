package com.orbitvis.pass.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassPrediction {

    /** Acquisition of signal (rise above elevation mask). */
    private Instant aos;

    /** Time of closest approach (maximum elevation). */
    private Instant tca;

    /** Loss of signal (drop below elevation mask). */
    private Instant los;

    /** Maximum elevation in degrees. */
    private double maxElevationDeg;

    /** Pass duration in seconds. */
    private long durationSec;
    private double azimuthAtAosDeg;
    private double azimuthAtLosDeg;
}
