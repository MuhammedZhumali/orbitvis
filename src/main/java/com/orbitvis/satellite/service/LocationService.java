package com.orbitvis.service;

import org.springframework.stereotype.Service;
import com.orbitvis.satellite.domain.Location;
import com.orbitvis.satellite.domain.LocationDto;
import com.orbitvis.satellite.repository.LocationRepository;
import lombok.RequiredArgsConstructor;


import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository repo;

    public List<LocationDto> listAll(){
        return repo.findAll().stream().map(this::toDto).toList();
    }

    public LocationDto findById(Long id){
        return repo.findById(id).map(this::toDto).orElse(null);
    }

    private LocationDto toDto(Location l){
        return new LocationDto(
            l.getId(),
            l.getName(),
            l.getLongitude(),
            l.getLatitude(),
            l.getAltitude(),
            l.getPolygon()
        );
    }
    
}
