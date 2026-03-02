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

import java.io.File;
import java.net.URL;

import javax.management.RuntimeErrorException;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.springframework.stereotype.Service;
import org.orekit.time.TimeScalesFactory;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



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


        return null;
    }


}
