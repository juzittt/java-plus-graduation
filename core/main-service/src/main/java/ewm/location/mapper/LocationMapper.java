package ewm.location.mapper;

import ewm.location.dto.LocationDto;
import ewm.location.dto.NewLocationDto;
import ewm.location.dto.UpdateLocationDto;
import ewm.location.model.Location;
import ewm.location.model.LocationType;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LocationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "locationType", source = "locationType", qualifiedByName = "stringToLocationType")
    Location toEntity(NewLocationDto dto);

    @Mapping(target = "locationType", source = "locationType", qualifiedByName = "locationTypeToString")
    LocationDto toDto(Location location);

    @Mapping(target = "locationType", source = "locationType", qualifiedByName = "stringToLocationType")
    void updateEntityFromDto(UpdateLocationDto dto, @MappingTarget Location location);

    @Named("stringToLocationType")
    default LocationType stringToLocationType(String value) {
        if (value == null || value.isBlank()) return null;
        return LocationType.valueOf(value.toUpperCase());
    }

    @Named("locationTypeToString")
    default String locationTypeToString(LocationType value) {
        return value != null ? value.name() : null;
    }
}