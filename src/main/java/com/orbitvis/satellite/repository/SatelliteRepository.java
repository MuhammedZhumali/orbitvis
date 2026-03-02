package com.orbitvis.satellite.repository;

import com.orbitvis.satellite.domain.Satellite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SatelliteRepository extends JpaRepository<Satellite, UUID> {
}
