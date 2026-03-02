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
    
        return orbitPoints;
    }

}
