package com.orbitvis.orbit.controller;

import com.orbitvis.orbit.model.CartesianPoint;
import com.orbitvis.orbit.model.OrbitPropagateRequest;
import com.orbitvis.orbit.model.TleData;
import com.orbitvis.orbit.service.OrbitPropogator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CancellationException;

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

        PropagationWindow w = propagationWindow(request);
        List<CartesianPoint> points = propagator.propagateToECRF(tle, w.start(), w.end(), w.step());
        return ResponseEntity.ok(points);
    }

    /**
     * Streams orbit points as SSE (event {@code point} per sample, then {@code done}).
     * POST body matches {@link #propagate}; use fetch + streaming reader on the client (EventSource is GET-only).
     */
    @PostMapping(value = "/propagate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<?>> propagateStream(@RequestBody OrbitPropagateRequest request) {
        if (request.getLine1() == null || request.getLine2() == null) {
            return Flux.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "line1 and line2 are required"));
        }
        TleData tle = new TleData();
        tle.setLine1(request.getLine1());
        tle.setLine2(request.getLine2());

        PropagationWindow w = propagationWindow(request);

        final TleData tleFinal = tle;
        final Instant startFinal = w.start();
        final Instant endFinal = w.end();
        final Duration stepFinal = w.step();

        return Flux.<ServerSentEvent<?>>create(sink -> {
                    try {
                        propagator.streamPropagateToECRF(tleFinal, startFinal, endFinal, stepFinal, p -> {
                            if (sink.isCancelled()) {
                                throw new CancellationException("Client disconnected");
                            }
                            sink.next(ServerSentEvent.builder(p).event("point").build());
                        });
                        if (!sink.isCancelled()) {
                            sink.next(ServerSentEvent.builder("ok").event("done").build());
                            sink.complete();
                        }
                    } catch (CancellationException e) {
                        sink.complete();
                    } catch (Exception e) {
                        sink.error(e);
                    }
                }, FluxSink.OverflowStrategy.BUFFER)
                .subscribeOn(Schedulers.boundedElastic());
    }

    private record PropagationWindow(Instant start, Instant end, Duration step) {}

    private static PropagationWindow propagationWindow(OrbitPropagateRequest request) {
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
        return new PropagationWindow(start, end, Duration.ofSeconds(stepSec));
    }
}
