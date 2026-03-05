package com.orbitvis.propagation;

import com.orbitvis.satellite.domain.SatelliteDto;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.springframework.stereotype.Component;

@Component
public class PropagatorFactory {

    /**
     * Creates an Orekit TLE propagator for the given satellite (uses line1/line2).
     * Orekit data must be initialized elsewhere (e.g. via OrbitPropogator bean).
     */
    public Propagator createForSatellite(SatelliteDto satellite) {
        if (satellite == null || satellite.getLine1() == null || satellite.getLine2() == null) {
            throw new IllegalArgumentException("Satellite and TLE lines (line1, line2) are required");
        }
        TLE tle = new TLE(satellite.getLine1(), satellite.getLine2());
        return TLEPropagator.selectExtrapolator(tle);
    }
}
