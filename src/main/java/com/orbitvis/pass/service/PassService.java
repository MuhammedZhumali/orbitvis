package com.orbitvis.pass.service;

import com.orbitvis.orbit.model.TleData;
import com.orbitvis.orbit.service.OrbitPropogator;
import com.orbitvis.pass.model.PassDto;
import com.orbitvis.pass.model.PassPrediction;
import com.orbitvis.pass.model.PassQueryRequest;
import com.orbitvis.satellite.domain.Satellite;
import com.orbitvis.satellite.repository.SatelliteRepository;
import org.hipparchus.ode.events.Action;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class PassService {

    private final OrbitPropogator orbitPropagator;
    private final SatelliteRepository satelliteRepository;

    public PassService(OrbitPropogator orbitPropagator, SatelliteRepository satelliteRepository) {
        this.orbitPropagator = orbitPropagator;
        this.satelliteRepository = satelliteRepository;
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

    /**
     * Predict all visible passes for a satellite and site within the given time window.
     */
    public List<PassPrediction> predict(PassQueryRequest request) {
        if (request == null
                || request.getSatelliteId() == null
                || request.getSite() == null
                || request.getStartTime() == null
                || request.getEndTime() == null
                || request.getStartTime().isAfter(request.getEndTime())) {
            return List.of();
        }

        Optional<Satellite> satOpt = satelliteRepository.findById(request.getSatelliteId());
        if (satOpt.isEmpty()) {
            return List.of();
        }
        Satellite sat = satOpt.get();

        TleData tle = new TleData();
        tle.setLine1(sat.getLine1());
        tle.setLine2(sat.getLine2());

        double lat = request.getSite().getLat();
        double lon = request.getSite().getLon();
        double alt = request.getSite().getAltMeters();
        double minElevRad = Math.toRadians(request.getMinElevationDeg());

        return computePasses(lat, lon, alt, tle, request.getStartTime(), request.getEndTime(), minElevRad);
    }

    private List<PassPrediction> computePasses(double lat, double lon, double altMeters,
                                               TleData tle, Instant start, Instant end, double minElevRad) {
        List<PassPrediction> result = new ArrayList<>();
        if (tle == null || tle.getLine1() == null || tle.getLine2() == null || start.isAfter(end)) {
            return result;
        }

        var utc = TimeScalesFactory.getUTC();
        TLE tleObj = new TLE(tle.getLine1(), tle.getLine2());
        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tleObj);
        TopocentricFrame topo = orbitPropagator.createTopocentricFrame(lat, lon, altMeters);

        AbsoluteDate startDate = new AbsoluteDate(Date.from(start), utc);
        AbsoluteDate endDate = new AbsoluteDate(Date.from(end), utc);

        CollectingPassHandler handler = new CollectingPassHandler();
        ElevationDetector detector = new ElevationDetector(topo)
                .withConstantElevation(minElevRad)
                .withHandler(handler);
        propagator.addEventDetector(detector);

        try {
            propagator.propagate(startDate, endDate);
        } catch (Exception e) {
            return result;
        }

        List<PassWindow> windowsCopy = new ArrayList<>(handler.windows);
        for (PassWindow window : windowsCopy) {
           if (window.aos == null || window.los == null || window.los.isBeforeOrEqualTo(window.aos)) {
                continue;
            }

            Instant aosInstant = window.aos.toDate(utc).toInstant();
            Instant losInstant = window.los.toDate(utc).toInstant();
            long durationSec = Duration.between(aosInstant, losInstant).getSeconds();
            if (durationSec <= 0) {
                continue;
            }

            // Azimuth at AOS and LOS
            PVCoordinates pvAos = propagator.getPVCoordinates(window.aos, orbitPropagator.getItrf());
            double azAos = Math.toDegrees(topo.getAzimuth(pvAos.getPosition(), orbitPropagator.getItrf(), window.aos));

            PVCoordinates pvLos = propagator.getPVCoordinates(window.los, orbitPropagator.getItrf());
            double azLos = Math.toDegrees(topo.getAzimuth(pvLos.getPosition(), orbitPropagator.getItrf(), window.los));

            // Max elevation and TCA by sampling between AOS and LOS
            double maxElev = -1e9;
            AbsoluteDate tMax = window.aos;
            double stepSec = 10.0;
            AbsoluteDate t = window.aos;
            while (t.isBeforeOrEqualTo(window.los)) {
                PVCoordinates pv = propagator.getPVCoordinates(t, orbitPropagator.getItrf());
                double elevRad = topo.getElevation(pv.getPosition(), orbitPropagator.getItrf(), t);
                if (elevRad > maxElev) {
                    maxElev = elevRad;
                    tMax = t;
                }
                t = t.shiftedBy(stepSec);
            }
            if (maxElev <= -1e8) {
                continue;
            }
            double maxElevationDeg = Math.toDegrees(maxElev);
            Instant tcaInstant = tMax.toDate(utc).toInstant();

            result.add(new PassPrediction(
                    aosInstant,
                    tcaInstant,
                    losInstant,
                    maxElevationDeg,
                    durationSec,
                    azAos,
                    azLos
            ));
        }

        return result;
    }

    private static class PassWindow {
        AbsoluteDate aos;
        AbsoluteDate los;
    }

    private static class CollectingPassHandler implements EventHandler {
        final List<PassWindow> windows = new ArrayList<>();
        private AbsoluteDate currentRise;

        @Override
        public Action eventOccurred(SpacecraftState s, EventDetector detector, boolean increasing) {
            if (increasing) {
                currentRise = s.getDate();
            } else {
                if (currentRise != null) {
                    PassWindow w = new PassWindow();
                    w.aos = currentRise;
                    w.los = s.getDate();
                    windows.add(w);
                    currentRise = null;
                }
            }
            return Action.CONTINUE;
        }
    }
}
