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
        itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        earth = new OneAxisEllipsoid(
             Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            itrf
        );
    }
}
