package com.orbitvis.satellite.controller;

import com.orbitvis.satellite.domain.SatelliteDto;
import com.orbitvis.satellite.service.SatelliteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<SatelliteDto> findById(@PathVariable UUID id){
        SatelliteDto dto = service.findById(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }
}
