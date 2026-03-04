package com.orbitvis.pass.controller;

import com.orbitvis.orbit.model.TleData;
import com.orbitvis.pass.model.*;
import com.orbitvis.pass.service.PassService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/passes")
public class PassController {

    private final PassService service;

    public PassController(PassService service) {
        this.service = service;
    }

    /**
     * Compute the first satellite pass over the given location.
     * Body: lat, lon, alt (optional), line1, line2, startEpoch (optional), endEpoch (optional).
     */
    @PostMapping
    public ResponseEntity<PassDto> detectPass(@RequestBody PassRequest request) {
        if (request.getLine1() == null || request.getLine2() == null) {
            return ResponseEntity.badRequest().build();
        }
        TleData tle = request.toTleData();
        Instant start = request.getStartEpoch() != null
                ? Instant.ofEpochSecond(request.getStartEpoch())
                : Instant.now();
        Instant end = request.getEndEpoch() != null
                ? Instant.ofEpochSecond(request.getEndEpoch())
                : start.plusSeconds(24 * 3600);
        if (end.isBefore(start)) {
            return ResponseEntity.badRequest().build();
        }
        PassDto pass = service.detectPass(
                request.getLat(),
                request.getLon(),
                request.getAlt(),
                tle,
                start,
                end
        );
        return pass != null ? ResponseEntity.ok(pass) : ResponseEntity.noContent().build();
    }

    @PostMapping("/query")
    public ResponseEntity<PassQueryResponse> passPrediction(@RequestBody PassQueryRequest request) {
        List<PassPrediction> passes = service.predict(request);
        PassQueryResponse response = new PassQueryResponse(passes);
        return ResponseEntity.ok(response);
    }
    
}
