package ru.practicum.ewm.event.mapper;

import ru.practicum.ewm.event.dto.LocationDto;
import ru.practicum.ewm.event.model.Location;

public class LocationMapper {
    public static LocationDto toLocationDto(Location location) {
        return LocationDto.builder().lon(location.getLon()).lat(location.getLat()).build();
    }

    public static Location toLocation(LocationDto locationDto) {
        return Location.builder().lon(locationDto.getLon()).lat(locationDto.getLat()).build();
    }
}
