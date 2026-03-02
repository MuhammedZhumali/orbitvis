package com.orbitvis.orbit.service;

import com.orbitvis.orbit.model.OrbitPoint;
import com.orbitvis.orbit.model.CartesianPoint;
import com.orbitvis.orbit.model.TleData;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.SpacecraftState;
import javax.management.RuntimeErrorException;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.springframework.stereotype.Service;
import org.orekit.time.TimeScalesFactory;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Service
public class OrbitPropogator {

    private final OneAxisEllipsoid earth;
    private final Frame itrf;
    private static final Logger log = LoggerFactory.getLogger(OrbitPropogator.class);
 
    
    public OrbitPropogator(){
        initializeOrekit();
        itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        earth = new OneAxisEllipsoid(
             Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            itrf
        );
    }

    private void initializeOrekit(){
        try{
            File orekitData = findOrekitDataDirectory();
            if(orekitData==null||!orekitData.exists()){
                throw new RuntimeException("Orekit directory was not found");
            }

            DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
            manager.clearProviders();
            manager.addProvider(new DirectoryCrawler(orekitData));
            log.info("Orekit data load from" + orekitData.getAbsolutePath());
        }
        catch(Exception e){
            throw new RuntimeException("Orekit init failed" + e.getMessage(), e);
        }
    }

    private File findOrekitDataDirectory(){
        File dockerPath = new File("/app/orekit-data");
        if(dockerPath.exists() && dockerPath.isDirectory()){
            log.debug("Found orekit path at" + dockerPath.getAbsolutePath());
            return dockerPath;
        }

        try{
            URL resourceUrl = getClass().getClassLoader().getResource("orekit-data");
            if(resourceUrl!=null){
                String protocol = resourceUrl.getProtocol();
                if("file".equals(protocol)){
                    File resourceFile = new File(resourceUrl.toURI());
                    if(resourceFile.exists() && resourceFile.isDirectory()){
                        log.debug("Found orekit in classpath" + resourceFile.getAbsolutePath());
                        return resourceFile;
                    }
                    else if("jar".equals(protocol)){
                        log.debug("Orekit data found in Jar");
                    }
                }
            }
        }catch(Exception e){
            log.debug("Could not load orekit from classpath" + e.getMessage());
        }

        File relativePath = new File("../backend/resources/orekit-data");
        if(relativePath.exists() && relativePath.isDirectory()){
            log.debug("Found orekit at:", relativePath.getAbsolutePath());
            return relativePath;
        }

        File projectPath = new File(System.getProperty("user.dir"), "backend/resources/orekit-data");
        if (projectPath.exists() && projectPath.isDirectory()) {
            log.debug("Found Orekit data at project path: {}", projectPath.getAbsolutePath());
            return projectPath;
        }

        log.error("Orekit data directory not found in any of the checked locations:");
        log.error("  - /app/orekit-data (Docker)");
        log.error("  - classpath:orekit-data");
        log.error("  - /resources/orekit-data (relative)");
        log.error("  - /resources/orekit-data", System.getProperty("user.dir"));
        
        return null;
    }


    public List<OrbitPoint> propageGeodetic(TleData tle, Instant start, Instant end, Duration step){
        if(start.isAfter(end)){
            throw new IllegalArgumentException("Start cannot be after end time");
        }

        if(step.isNegative() || step.isZero()){
            throw new IllegalArgumentException("Step must be positive");
        }

        List<OrbitPoint> orbitPoints = new ArrayList<>();
    
        try {
            TLE tleObj = new TLE(tle.getLine1(), tle.getLine2());
            TLEPropagator propagator = TLEPropagator.selectExtrapolator(tleObj);

            var utc = TimeScalesFactory.getUTC();

            AbsoluteDate startDate = new AbsoluteDate(Date.from(start), utc);
            AbsoluteDate endDate = new AbsoluteDate(Date.from(end), utc);
            log.debug("Propagator: startDate={}, endDate={}", startDate, endDate);
            log.debug("Propagator: startDate.isAfter(endDate)={}", startDate.isAfter(endDate));
            log.debug(
                "Propagator: start={} ({}), end={} ({})",
                start, start.getEpochSecond(),
                end, end.getEpochSecond()
            );

            double stepSeconds = step.getSeconds();
            log.debug("Propagator: stepSeconds={}", stepSeconds);
            
            if (startDate.isAfter(endDate)) {
                log.error("ERROR: startDate is after endDate! Cannot propagate.");
                log.error("  startDate=" + startDate);
                log.error("  endDate=" + endDate);
                throw new IllegalArgumentException("Start date must be before or equal to end date");
            }
            
            double durationSeconds = endDate.durationFrom(startDate);
            int expectedPoints = (int) Math.ceil(durationSeconds / stepSeconds) + 1;
            log.debug("Propagator: Expected approximately {} points (duration={}s, step={}s)", expectedPoints, durationSeconds, stepSeconds);

            AbsoluteDate currentDate = startDate;
            int pointCount = 0;

            while (!currentDate.isAfter(endDate)) {
                try {
                    PVCoordinates pv = propagator.getPVCoordinates(currentDate, itrf);
                    GeodeticPoint gp = earth.transform(pv.getPosition(), itrf, currentDate);

                    OrbitPoint point = new OrbitPoint();

                    // Convert AbsoluteDate to epoch seconds
                    // Use offset from J2000 epoch (2000-01-01 12:00:00 TAI) and convert to Unix epoch
                    long epochSeconds;
                    try {
                        // J2000 epoch in Unix seconds: 2000-01-01 12:00:00 UTC = 946728000
                        AbsoluteDate j2000Epoch = new AbsoluteDate(2000, 1, 1, 12, 0, 0.0, utc);
                        double secondsSinceJ2000 = currentDate.durationFrom(j2000Epoch);
                        long j2000EpochUnix = 946728000L;
                        epochSeconds = j2000EpochUnix + (long) secondsSinceJ2000;
                        
                        // Validate the conversion
                        if (epochSeconds <= 0) {
                            log.error("ERROR: Invalid epoch conversion. currentDate=" + currentDate + 
                                             ", secondsSinceJ2000=" + secondsSinceJ2000 + 
                                             ", epochSeconds=" + epochSeconds);
                            // Fallback: try toDate method
                            Date date = currentDate.toDate(utc);
                            if (date != null) {
                                Instant instant = date.toInstant();
                                if (instant != null) {
                                    epochSeconds = instant.getEpochSecond();
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("ERROR in date conversion: " + e.getMessage(), e);
                        // Fallback: try toDate method
                        try {
                            Date date = currentDate.toDate(utc);
                            if (date != null) {
                                Instant instant = date.toInstant();
                                if (instant != null) {
                                    epochSeconds = instant.getEpochSecond();
                                } else {
                                    epochSeconds = 0;
                                }
                            } else {
                                epochSeconds = 0;
                            }
                        } catch (Exception e2) {
                            log.error("Fallback conversion also failed: " + e2.getMessage());
                            epochSeconds = 0;
                        }
                    }
                    
                    if (epochSeconds == 0 && pointCount == 0) {
                        log.warn("WARNING: First point has epochSeconds=0. Date conversion failed.");
                        log.warn("  currentDate=" + currentDate);
                    }
                    
                    point.setTime(epochSeconds);

                    point.setLat(Math.toDegrees(gp.getLatitude()));
                    point.setLon(Math.toDegrees(gp.getLongitude()));
                    point.setAlt(gp.getAltitude());

                    orbitPoints.add(point);
                    pointCount++;

                    if (pointCount <= 3 || pointCount % 10 == 0) {
                        log.debug("Point {}: time={}, lat={}, lon={}", pointCount, epochSeconds, point.getLat(), point.getLon());
                    }

                    currentDate = currentDate.shiftedBy(stepSeconds);
                    
                    // Safety check: if we've only generated 1 point and currentDate is already after endDate, something is wrong
                    if (pointCount == 1 && currentDate.isAfter(endDate)) {
                        log.error("WARNING: Only 1 point generated and loop would exit. startDate=" + startDate + ", endDate=" + endDate + ", stepSeconds=" + stepSeconds);
                        log.error("  Duration between start and end: " + Duration.between(start, end).getSeconds() + " seconds");
                    }
                } catch (Exception e) {
                    log.error("Error generating point at " + currentDate + ": " + e.getMessage(), e);
                    // Don't break on first error - try to continue if possible
                    // Only break if we've tried multiple times
                    if (pointCount > 10) {
                        log.error("Too many errors, stopping propagation");
                        break;
                    }
                    // Continue to next iteration
                    currentDate = currentDate.shiftedBy(stepSeconds);
                }
            }
            
            log.info("Propagator: Generated " + pointCount + " points total");
        } catch (Exception e) {
            log.error("Error propagating orbit: " + e.getMessage(), e);
            throw new RuntimeException("Failed to propagate orbit: " + e.getMessage(), e);
        }

        return orbitPoints;
    }

}
