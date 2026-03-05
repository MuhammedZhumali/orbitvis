package com.orbitvis.realtime.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.orekit.utils.Constants;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import com.orbitvis.pass.model.SitePoint;
import com.orbitvis.propagation.PropagatorFactory;
import com.orbitvis.realtime.model.RealtimeStateDto;
import com.orbitvis.satellite.service.SatelliteService;

@Service
public class RealTimeService {
    private final SatelliteService satelliteService;
    private final PropagatorFactory propagatorFactory;

    public RealTimeService(SatelliteService satelliteService, PropagatorFactory propagatorFactory){
        this.satelliteService = satelliteService;
        this.propagatorFactory = propagatorFactory;
    }

    public Flux<RealtimeStateDto> stream(UUID satelliteId, Optional<SitePoint> maybeSite, double rateHz){
        if(rateHz<=0 || rateHz > 5){
            rateHz=1.0;
        }

        var sat = satelliteService.findById(satelliteId);
        if (sat == null) {
            return Flux.empty();
        }
        Propagator propagator = propagatorFactory.createForSatellite(sat);

        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                itrf
        );

        TopocentricFrame topo = maybeSite
                .map(site -> {
                    GeodeticPoint gp = new GeodeticPoint(
                            FastMath.toRadians(site.getLat()),
                            FastMath.toRadians(site.getLon()),
                            site.getAltMeters()
                    );
                    return new TopocentricFrame(earth, gp, "site");
                })
                .orElse(null);

        long periodMs = (long) (1000.0 / rateHz);

        return Flux.interval(Duration.ofMillis(periodMs))
                .map(tick->{
                    Instant now = Instant.now();
                    AbsoluteDate date = new AbsoluteDate(java.util.Date.from(now), TimeScalesFactory.getUTC());

                    SpacecraftState st = propagator.propagate(date);

                    var pvEcef = st.getPVCoordinates(itrf);
                    var pos = pvEcef.getPosition();

                    GeodeticPoint geo = earth.transform(pos, itrf, date);
                    double latDeg = FastMath.toDegrees(geo.getLatitude());
                    double lonDeg = FastMath.toDegrees(geo.getLongitude());
                    double altM = geo.getAltitude();

                    Double azDeg = null;
                    Double elDeg = null;
                    Double rangeKm = null;
                    boolean inView = false;

                    if (topo != null) {
                        double az = topo.getAzimuth(pos, itrf, date);
                        double el = topo.getElevation(pos, itrf, date);
                        double range = topo.getRange(pos, itrf, date);

                        azDeg = FastMath.toDegrees(az);
                        elDeg = FastMath.toDegrees(el);
                        rangeKm = range / 1000.0;

                        // например "видимость" >= 10 градусов (можно параметром)
                        inView = elDeg >= 10.0;
                    }

                    return new RealtimeStateDto(
                            now,
                            latDeg,
                            lonDeg,
                            altM,
                            pos.getX(),
                            pos.getY(),
                            pos.getZ(),
                            azDeg != null ? azDeg : 0.0,
                            elDeg != null ? elDeg : 0.0,
                            rangeKm != null ? rangeKm : 0.0,
                            inView
                    );
                });
   }

}
