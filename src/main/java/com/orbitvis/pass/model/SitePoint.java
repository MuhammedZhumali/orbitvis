package com.orbitvis.pass.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SitePoint {

    /** Latitude in degrees. */
    private double lat;

    /** Longitude in degrees. */
    private double lon;

    /** Altitude above ellipsoid in metres. */
    private double altMeters;
}

