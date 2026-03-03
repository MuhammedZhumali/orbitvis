package com.orbitvis.orbit.controller;

import com.orbitvis.orbit.model.CartesianPoint;
import com.orbitvis.orbit.model.OrbitPropagateRequest;
import com.orbitvis.orbit.model.TleData;
import com.orbitvis.orbit.service.OrbitPropogator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/orbit")
public class OrbitController {

    private final OrbitPropogator propagator;

    public OrbitController(OrbitPropogator propagator) {
        this.propagator = propagator;
    }

    @PostMapping("/propagate")
    public ResponseEntity<List<CartesianPoint>> propagate(@RequestBody OrbitPropagateRequest request) {
        if (request.getLine1() == null || request.getLine2() == null) {
            return ResponseEntity.badRequest().build();
        }
        TleData tle = new TleData();
        tle.setLine1(request.getLine1());
        tle.setLine2(request.getLine2());

        Instant start;
        Instant end;
        if (request.getStartEpoch() == -1 || request.getStartEpoch() == 0) {
            start = Instant.now();
            end = start.plusSeconds(90 * 60);
        } else {
            start = Instant.ofEpochSecond(request.getStartEpoch());
            end = request.getEndEpoch() == -1 || request.getEndEpoch() == 0
                    ? start.plusSeconds(90 * 60)
                    : Instant.ofEpochSecond(request.getEndEpoch());
        }
        if (end.isBefore(start)) {
            end = start.plusSeconds(10 * 60);
        }
        long stepSec = request.getStepSeconds() > 0 ? request.getStepSeconds() : 60;
        Duration step = Duration.ofSeconds(stepSec);

        List<CartesianPoint> points = propagator.propagateToECRF(tle, start, end, step);
        return ResponseEntity.ok(points);
    }
}
