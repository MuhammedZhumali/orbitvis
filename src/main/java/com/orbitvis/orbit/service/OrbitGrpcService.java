package com.orbitvis.orbit.service;

import com.orbitvis.orbit.model.CartesianPoint;
import com.orbitvis.orbit.model.TleData;
import com.orbitvis.orbit.tle.TleParser;
import com.orbitvis.orbit.proto.OrbitGrpc;
import com.orbitvis.orbit.proto.OrbitPoint;
import com.orbitvis.orbit.proto.OrbitRequest;
import com.orbitvis.orbit.proto.OrbitResponse;
import io.grpc.stub.StreamObserver;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.time.TimeScalesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class OrbitGrpcService extends OrbitGrpc.OrbitImplBase {

    private static final Logger log = LoggerFactory.getLogger(OrbitGrpcService.class);

    @Autowired
    private OrbitPropogator propagator;

    @Autowired
    private TleParser tleParser;

    @Override
    public void propagate(OrbitRequest request, StreamObserver<OrbitResponse> responseObserver) {
        String line1 = null;
        String line2 = null;
        long startEpochSec = -2L;
        long endEpochSec = -2L;
        long stepSec = -2L;
        
        try{
            line1 = request.getLine1();
            line2 = request.getLine2();

            TleData tle = tleParser.parse(line1, line2);

            log.debug("Line1=[{}], len = {}", line1, line1.length());
            log.debug("Line2=[{}], len = {}", line2, line2.length());

            TLE tleObj = new TLE(line1, line2);
            var utc = TimeScalesFactory.getUTC();
            Instant tleEpoch = tleObj.getDate().toDate(utc).toInstant();
            log.debug("TLE epoch: {} (epoch seconds: {})", tleEpoch, tleEpoch.getEpochSecond());
            
            long tleEpochSec = tleEpoch.getEpochSecond();
            if (tleEpochSec == 0) {
                throw new RuntimeException("TLE epoch is invalid");
            }

            // TLE epoch should be between 2000 and 2100 (reasonable satellite lifetime)
            if (tleEpochSec < 946684800L || tleEpochSec > 4102444800L) {
                log.warn("WARNING: TLE epoch seems unusual: " + tleEpoch + " (" + tleEpochSec + ")");
            }

            // 3. Обрабатываем startEpoch / endEpoch
            // -------------------------
            // Use -1 as sentinel value to indicate "use defaults" (TLE epoch)
            // This allows distinguishing between "not provided" and "explicitly set to 0"            
            startEpochSec = request.getStartEpoch();
            endEpochSec = request.getEndEpoch();
            log.debug("Request: startEpoch={}, endEpoch={}, stepSeconds = {}", startEpochSec, endEpochSec, request.getStepSeconds());

            Instant start;
            if (startEpochSec == -1 || startEpochSec == 0) {
                // Если с фронта пришёл -1 (или 0 для backward compatibility) → используем эпоху TLE
                start = tleEpoch;
                log.debug("Using TLE epoch as start: {} (epoch seconds: {})", start, start.getEpochSecond());
            } else {
                start = Instant.ofEpochSecond(startEpochSec);
                log.debug("Using provided start epoch: {} (epoch seconds: {})", start, start.getEpochSecond());
            }

            Instant end;
            if(endEpochSec==-1||endEpochSec==0){
                end = start.plusSeconds(90*60);
                log.debug("Using default end (start + 90min): {} (epoch seconds: {})", end, end.getEpochSecond());
            } else {
                end = Instant.ofEpochSecond(endEpochSec);
                log.debug("Using provided end epoch: {} (epoch seconds: {})", end, end.getEpochSecond());
            }
            
            if(end.isBefore(start)){
                end = start.plusSeconds(10*60);
            }

            log.debug("Propagation range: start={} (epoch seconds: {}), end={} (epoch seconds: {})", start, start.getEpochSecond(), end, end.getEpochSecond());
            log.debug("Duration: {} seconds", Duration.between(start, end).getSeconds());

            stepSec = request.getStepSeconds() > 0 ? request.getStepSeconds() : 60;
            Duration step = Duration.ofSeconds(stepSec);
            log.debug("Step: {} seconds", stepSec);

            List<CartesianPoint> orbitPoints = propagator.propagateToECRF(tle, start, end, step);
            log.debug("Propagation completed from {} to {} with step of {} seconds, total points: {}",
                    start, end, stepSec, orbitPoints.size()
            );

            OrbitResponse.Builder responseBuilder = OrbitResponse.newBuilder();
            for (CartesianPoint point : orbitPoints) {
                OrbitPoint protoPoint = OrbitPoint.newBuilder()
                        .setTime(point.getTime())
                        .setX(point.getX())
                        .setY(point.getY())
                        .setZ(point.getZ())
                        .build();
                responseBuilder.addPoints(protoPoint);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        }catch(Exception e){
            log.error(
                    "Orbit propagate failed: line1={}, line2={}, startEpoch={}, endEpoch={}, stepSeconds={}",
                    line1,
                    line2,
                    startEpochSec,
                    endEpochSec,
                    stepSec,
                    e
            );
            // gRPC вернёт ошибку наверх, REST уже оборачивает её в 500 и JSON
            responseObserver.onError(e);
        }
    }
}
