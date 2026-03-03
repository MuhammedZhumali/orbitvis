package com.orbitvis.pass.service;

import com.orbitvis.orbit.model.TleData;
import com.orbitvis.orbit.service.OrbitPropogator;
import com.orbitvis.pass.model.PassDto;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.hipparchus.ode.events.Action;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class PassService {

    private final OrbitPropogator orbitPropagator;

    public PassService(OrbitPropogator orbitPropagator) {
        this.orbitPropagator = orbitPropagator;
    }

    /**
     * Detects the first satellite pass (rise/set) over the given location within the time interval.
     *
     * @param lat      latitude in degrees
     * @param lon      longitude in degrees
     * @param alt      altitude above ellipsoid in metres
     * @param tle      TLE data
     * @param interval search window duration from now
     * @return first pass in the interval, or null if none
     */
    public PassDto detectPass(double lat, double lon, double alt, TleData tle, Duration interval) {
        if (tle == null || tle.getLine1() == null || tle.getLine2() == null) {
            return null;
        }
        Instant start = Instant.now();
        Instant end = start.plus(interval);
        return detectPass(lat, lon, alt, tle, start, end);
    }

    /**
     * Detects the first satellite pass (rise/set) over the given location between start and end.
     */
    public PassDto detectPass(double lat, double lon, double alt, TleData tle, Instant start, Instant end) {
        if (tle == null || tle.getLine1() == null || tle.getLine2() == null || start.isAfter(end)) {
            return null;
        }

        var utc = TimeScalesFactory.getUTC();
        TLE tleObj = new TLE(tle.getLine1(), tle.getLine2());
        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tleObj);
        TopocentricFrame topo = orbitPropagator.createTopocentricFrame(lat, lon, alt);

        AbsoluteDate startDate = new AbsoluteDate(Date.from(start), utc);
        AbsoluteDate endDate = new AbsoluteDate(Date.from(end), utc);

        ElevationPassHandler handler = new ElevationPassHandler();
        ElevationDetector detectorWithHandler = new ElevationDetector(topo)
                .withConstantElevation(0.0)
                .withHandler(handler);
        propagator.addEventDetector(detectorWithHandler);

        try {
            propagator.propagate(startDate, endDate);
        } catch (Exception e) {
            return null;
        }

        if (handler.riseDate == null || handler.setDate == null || handler.setDate.isBeforeOrEqualTo(handler.riseDate)) {
            return null;
        }

        Instant riseTime = handler.riseDate.toDate(utc).toInstant();
        Instant setTime = handler.setDate.toDate(utc).toInstant();

        double maxElevation = 0;
        double azimuthAtMax = 0;
        double stepSec = 10;
        AbsoluteDate t = handler.riseDate;
        while (t.isBeforeOrEqualTo(handler.setDate)) {
            PVCoordinates pv = propagator.getPVCoordinates(t, orbitPropagator.getItrf());
            double elev = topo.getElevation(pv.getPosition(), orbitPropagator.getItrf(), t);
            double az = topo.getAzimuth(pv.getPosition(), orbitPropagator.getItrf(), t);
            if (elev > maxElevation) {
                maxElevation = elev;
                azimuthAtMax = az;
            }
            t = t.shiftedBy(stepSec);
        }
        maxElevation = Math.toDegrees(maxElevation);
        azimuthAtMax = Math.toDegrees(azimuthAtMax);

        Duration duration = Duration.between(riseTime, setTime);
        return new PassDto(riseTime, setTime, maxElevation, duration, azimuthAtMax);
    }

    private static class ElevationPassHandler implements EventHandler {
        AbsoluteDate riseDate;
        AbsoluteDate setDate;

        @Override
        public Action eventOccurred(SpacecraftState s, EventDetector detector, boolean increasing) {
            if (increasing) {
                if (riseDate == null) riseDate = s.getDate();
            } else {
                if (setDate == null && riseDate != null) setDate = s.getDate();
            }
            return Action.CONTINUE;
        }
    }
}
