package com.orbitvis.satellite.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.orbitvis.satellite.domain.SatelliteDto;
import com.orbitvis.satellite.repository.SatelliteRepository;
import com.orbitvis.satellite.service.SatelliteService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/satellite")
public class SatelliteController {

    private final SatelliteService service;

    public SatelliteController(SatelliteService service){
        this.service = service;
    }
    
    @GetMapping("/getAll")
    public List<SatelliteDto> all(){
        return service.listAll();
    } 

    @GetMapping("/get/{id}")
    public SatelliteDto findById(@PathVariable UUID id){
        return service.findById(id);
    }

}
