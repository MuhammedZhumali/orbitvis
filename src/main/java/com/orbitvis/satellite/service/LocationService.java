package com.orbitvis.satellite.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.orbitvis.satellite.domain.Location;
import com.orbitvis.satellite.domain.LocationDto;
import com.orbitvis.satellite.repository.LocationRepository;
import lombok.RequiredArgsConstructor;


import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository repo;

    @Transactional(readOnly = true)
    public List<LocationDto> listAll(){
        return repo.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public LocationDto findById(Long id){
        return repo.findById(id).map(this::toDto).orElse(null);
    }

    private LocationDto toDto(Location l){
        var polygon = l.getPolygon();
        return new LocationDto(
            l.getId(),
            l.getName(),
            l.getLongitude(),
            l.getLatitude(),
            l.getAltitude(),
            polygon != null ? new ArrayList<>(polygon) : null
        );
    }
    
}
