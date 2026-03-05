package com.orbitvis.realtime.controller;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.UUID;

import com.orbitvis.pass.model.SitePoint;
import com.orbitvis.realtime.model.RealtimeStateDto;
import com.orbitvis.realtime.service.RealTimeService;

@RestController
@RequestMapping("/api/realtime")
public class RealtimeController {

    private final RealTimeService service;

    public RealtimeController(RealTimeService service) {
        this.service = service;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<RealtimeStateDto>> stream(
            @RequestParam UUID satelliteId,
            @RequestParam(required = false) Double siteLat,
            @RequestParam(required = false) Double siteLon,
            @RequestParam(required = false) Double siteAlt,
            @RequestParam(defaultValue = "1") double rateHz
    ) {
        Optional<SitePoint> site = Optional.empty();
        if (siteLat != null && siteLon != null) {
            site = Optional.of(new SitePoint(
                    siteLat, siteLon, siteAlt != null ? siteAlt : 0.0
            ));
        }
        return service.stream(satelliteId, site, rateHz)
                .map(dto -> ServerSentEvent.builder(dto)
                        .event("state")
                        .build());
    }
}