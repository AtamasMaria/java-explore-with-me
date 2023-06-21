package ru.practicum.ewm.location.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.location.dto.LocationDto;
import ru.practicum.ewm.location.model.Location;

@UtilityClass
public class LocationMapper {
    public LocationDto toLocationDto(Location location) {
        return LocationDto.builder().lon(location.getLon()).lat(location.getLat()).build();
    }

    public Location toLocation(LocationDto locationDto) {
        return Location.builder().lon(locationDto.getLon()).lat(locationDto.getLat()).build();
    }
}
