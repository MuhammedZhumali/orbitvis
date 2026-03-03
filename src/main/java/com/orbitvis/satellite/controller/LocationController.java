package com.orbitvis.satellite.controller;

import com.orbitvis.satellite.domain.LocationDto;
import com.orbitvis.satellite.service.LocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService service){
        this.locationService = service;
    }
    
    @GetMapping("/getAll")
    public List<LocationDto> all(){
        return locationService.listAll();
    } 

    @GetMapping("/get/{id}")
    public ResponseEntity<LocationDto> findById(@PathVariable Long id){
        LocationDto dto = locationService.findById(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }
}
