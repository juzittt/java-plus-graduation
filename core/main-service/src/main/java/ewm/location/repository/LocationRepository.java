package ewm.location.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ewm.events.model.Event;
import ewm.location.model.Location;
import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {

    @Query(value = """
        SELECT l.* FROM locations l
        WHERE ABS(l.lat - :lat) < 0.001 AND ABS(l.lon - :lon) < 0.001
        """, nativeQuery = true)
    List<Location> findLocationsContainingPoint(@Param("lat") double lat, @Param("lon") double lon);

    @Query(value = """
        SELECT e.* FROM events e
        JOIN locations l ON e.location_id = l.id
        WHERE l.id = :locationId
        """, nativeQuery = true)
    List<Event> findEventsInLocation(@Param("locationId") Long locationId);

    @Query(value = """
        SELECT e.id, e.title, e.annotation, e.event_date, e.paid, 0.0 as distance
        FROM events e
        JOIN locations l ON e.location_id = l.id
        WHERE e.state = 'PUBLISHED'
          AND (:lat IS NOT NULL)
          AND (:lon IS NOT NULL)
          AND (:radius IS NOT NULL)
        ORDER BY e.event_date
        """, nativeQuery = true)
    List<Object[]> findEventsWithDistanceWithinRadius(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radius") double radius
    );

    @Query(value = """
        SELECT 0.0 
        FROM events e
        JOIN locations l ON e.location_id = l.id
        WHERE e.id = :eventId
          AND (:lat IS NOT NULL)
          AND (:lon IS NOT NULL)
        """, nativeQuery = true)
    Double findDistanceToEvent(
            @Param("eventId") Long eventId,
            @Param("lat") double lat,
            @Param("lon") double lon
    );
}