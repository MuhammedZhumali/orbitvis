package com.orbitvis.pass.model;

import com.orbitvis.orbit.model.TleData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassRequest {
    /** Latitude in degrees */
    private double lat;
    /** Longitude in degrees */
    private double lon;
    /** Altitude above ellipsoid in metres */
    private double alt = 0;
    private String line1;
    private String line2;
    /** Start of search window (epoch seconds), or null for now */
    private Long startEpoch;
    /** End of search window (epoch seconds), or null for start + 24 hours */
    private Long endEpoch;

    public TleData toTleData() {
        TleData tle = new TleData();
        tle.setLine1(line1);
        tle.setLine2(line2);
        return tle;
    }
}
