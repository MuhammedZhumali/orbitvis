package com.orbitvis.orbit.tle;

import com.orbitvis.orbit.model.TleData;
import org.springframework.stereotype.Service;

@Service
public class TleParser {
    
    public TleData parse(String line1, String line2){
        TleData tleData = new TleData();
        tleData.setName("Satellite");
        tleData.setLine1(line1.trim());
        tleData.setLine2(line2.trim());
        return tleData;
    }
}
