package com.example.realtimecommunication.module.location.service;

import com.example.realtimecommunication.module.location.dto.LocationDto;

import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class LocationService {

    private static final long TIMEOUT = 10_000L;
    private final Map<Long, List<LocationDto>> locations = new ConcurrentHashMap<>();
    private final Map<Long, BlockingQueue<DeferredResult<LocationDto>>> groupRequests =
            new ConcurrentHashMap<>();

    public LocationDto shareCurLocation(Long groupId) {
        List<LocationDto> groupLocations =
                locations.computeIfAbsent(groupId, k -> new CopyOnWriteArrayList<>());

        // 새로운 위치 정보 생성 및 추가
        LocationDto newLocation = makeRandomLocation();
        groupLocations.add(newLocation);

        return newLocation;
    }

    public LocationDto makeRandomLocation() {
        double randomX = Math.random() * 100;
        double randomY = Math.random() * 100;

        return new LocationDto(randomX, randomY);
    }

    public DeferredResult<LocationDto> longPoll(final Long groupId) {
        final DeferredResult<LocationDto> deferredResult = new DeferredResult<>(TIMEOUT);

        deferredResult.onTimeout(() -> deferredResult.setErrorResult("Request timeout"));

        groupRequests
                .computeIfAbsent(groupId, k -> new LinkedBlockingQueue<>())
                .add(deferredResult);

        return deferredResult;
    }

    public void notifyGroup(final Long groupId) {
        final BlockingQueue<DeferredResult<LocationDto>> queue = groupRequests.get(groupId);

        Optional.ofNullable(queue)
                .ifPresent(
                        q -> {
                            while (!q.isEmpty()) {
                                final DeferredResult<LocationDto> connection = q.poll();
                                if (connection != null) {
                                    connection.setResult(makeRandomLocation());
                                }
                            }
                        });
    }
}
