package com.orbitvis.satellite.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.orbitvis.satellite.domain.LocationDto;
import com.orbitvis.satellite.repository.LocationRepository;
import com.orbitvis.satellite.service.LocationService;

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
    public LocationDto findById(@PathVariable Long id){
        return locationService.findById(id);
    }

}
