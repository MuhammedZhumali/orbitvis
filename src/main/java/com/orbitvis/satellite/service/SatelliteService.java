package com.orbitvis.satellite.service;

import com.orbitvis.satellite.domain.Satellite;
import com.orbitvis.satellite.domain.SatelliteDto;
import com.orbitvis.satellite.repository.SatelliteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SatelliteService {
    private final SatelliteRepository satRepo;

    public List<SatelliteDto> listAll() {
        return satRepo.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public SatelliteDto findById(UUID id) {
        return satRepo.findById(id)
                .map(this::toDto)
                .orElse(null);
    }

    public void add(SatelliteDto dto){
        var satellite = new Satellite();
        satellite.setLine1(dto.getLine1());
        satellite.setLine2(dto.getLine2());
        satellite.setMaxNadirOffAngle(dto.getMaxNadirOffAngle());
        satellite.setMinSunAngle(dto.getMinSunAngle());
        satellite.setMaxRollAngle(dto.getMaxRollAngle());
        satRepo.save(satellite);
    }

    public boolean update(UUID id, SatelliteDto dto){
        return satRepo.findById(id).map(satellite -> {
            satellite.setLine1(dto.getLine1());
            satellite.setLine2(dto.getLine2());
            satellite.setMaxNadirOffAngle(dto.getMaxNadirOffAngle());
            satellite.setMinSunAngle(dto.getMinSunAngle());
            satellite.setMaxRollAngle(dto.getMaxRollAngle());
            satRepo.save(satellite);
            return true;
        }).orElse(false);
    }

    public void delete(UUID id){
        satRepo.deleteById(id);
    }

    private SatelliteDto toDto(Satellite s) {
        return new SatelliteDto(
                s.getSatelliteId(),
                s.getLine1(),
                s.getLine2(),
                s.getMaxNadirOffAngle(),
                s.getMinSunAngle(),
                s.getMaxRollAngle()
        );
    }
}
