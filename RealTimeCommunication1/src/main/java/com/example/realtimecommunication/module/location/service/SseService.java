package com.example.realtimecommunication.module.location.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

    private static final String SSE_EVENT_NAME = "location";
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final LocationService locationService;

    public SseService(final LocationService locationService) {
        this.locationService = locationService;
    }

    public SseEmitter add() {
        final SseEmitter emitter = new SseEmitter();
        this.emitters.add(emitter);

        shareCurLocation();
        return emitter;
    }

    public void shareCurLocation() {

        emitters.forEach(emit -> emit.onError(e -> removeEmitter(emit)));
        emitters.forEach(emit -> emit.onTimeout(() -> removeEmitter(emit)));
        emitters.forEach(emit -> emit.onCompletion(() -> removeEmitter(emit)));

        emitters.forEach(
                emit -> {
                    try {
                        emit.send(
                                SseEmitter.event()
                                        .name(SSE_EVENT_NAME)
                                        .data(locationService.makeRandomLocation()));
                        emit.complete();
                    } catch (final Exception e) {
                        removeEmitter(emit);
                    }
                });
    }

    private void removeEmitter(final SseEmitter emitter) {
        emitter.complete();
        emitters.remove(emitter);
    }
}
