package com.example.realtimecommunication.module.location.controller;

import com.example.realtimecommunication.module.location.dto.LocationDto;
import com.example.realtimecommunication.module.location.service.LocationService;
import com.example.realtimecommunication.module.location.service.SseService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/location")
@Slf4j
public class LocationController {
    private final LocationService locationService;
    private final SseService sseService;

    public LocationController(LocationService locationService, SseService sseService) {
        this.locationService = locationService;
        this.sseService = sseService;
    }

    @GetMapping("/cur/{id}")
    public ResponseEntity<LocationDto> shareCurLocation(@PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(locationService.shareCurLocation(id));
    }

    @GetMapping(value = "/sse/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectSse() {
        return sseService.add();
    }

    @PostMapping(value = "/sse/share")
    public ResponseEntity<Void> shareCurLocationBySse() {
        sseService.shareCurLocation();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/long/{groupId}")
    public DeferredResult<LocationDto> poll(@PathVariable final Long groupId) {
        return locationService.longPoll(groupId);
    }

    @PostMapping("/long/{groupId}/notify")
    public void notifyGroup(@PathVariable final Long groupId) {
        locationService.notifyGroup(groupId);
    }
}
