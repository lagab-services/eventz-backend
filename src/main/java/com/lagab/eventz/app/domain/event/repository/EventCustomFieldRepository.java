package com.lagab.eventz.app.domain.event.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lagab.eventz.app.domain.event.model.EventCustomField;
import com.lagab.eventz.app.domain.event.model.FieldType;

@Repository
public interface EventCustomFieldRepository extends JpaRepository<EventCustomField, Long> {

    /**
     * Finds all custom fields for an event, sorted by display order
     */
    List<EventCustomField> findByEventIdOrderByDisplayOrder(Long eventId);

    /**
     * Finds custom fields for an event for a specific ticket type
     */
    @Query("""
            SELECT ecf FROM EventCustomField ecf 
            WHERE ecf.event.id = :eventId 
            AND (ecf.ticketType.id = :ticketTypeId OR ecf.ticketType IS NULL)
            ORDER BY ecf.displayOrder ASC
            """)
    List<EventCustomField> findByEventIdAndTicketTypeIdOrderByDisplayOrder(
            @Param("eventId") Long eventId,
            @Param("ticketTypeId") Long ticketTypeId
    );

    /**
     * Finds all required fields for an event and ticket type
     */
    @Query("""
            SELECT ecf FROM EventCustomField ecf 
            WHERE ecf.event.id = :eventId 
            AND ecf.required = true
            AND (ecf.ticketType.id = :ticketTypeId OR ecf.ticketType IS NULL)
            ORDER BY ecf.displayOrder ASC
            """)
    List<EventCustomField> findRequiredFieldsByEventAndTicketType(
            @Param("eventId") Long eventId,
            @Param("ticketTypeId") Long ticketTypeId
    );

    /**
     * Finds a custom field by name and event
     */
    @Query("""
            SELECT ecf FROM EventCustomField ecf 
            WHERE ecf.event.id = :eventId 
            AND ecf.fieldName = :fieldName
            """)
    Optional<EventCustomField> findByEventIdAndFieldName(
            @Param("eventId") Long eventId,
            @Param("fieldName") String fieldName
    );

    /**
     * Finds all custom fields of a specific type for an event
     */
    @Query("""
            SELECT ecf FROM EventCustomField ecf 
            WHERE ecf.event.id = :eventId 
            AND ecf.fieldType = :fieldType
            ORDER BY ecf.displayOrder ASC
            """)
    List<EventCustomField> findByEventIdAndFieldType(
            @Param("eventId") Long eventId,
            @Param("fieldType") FieldType fieldType
    );

    /**
     * Finds all global fields (not linked to a specific ticket type)
     */
    @Query("""
            SELECT ecf FROM EventCustomField ecf 
            WHERE ecf.event.id = :eventId 
            AND ecf.ticketType IS NULL
            ORDER BY ecf.displayOrder ASC
            """)
    List<EventCustomField> findGlobalFieldsByEventId(@Param("eventId") Long eventId);

    /**
     * Finds all fields specific to a ticket type
     */
    @Query("""
            SELECT ecf FROM EventCustomField ecf 
            WHERE ecf.event.id = :eventId 
            AND ecf.ticketType.id = :ticketTypeId
            ORDER BY ecf.displayOrder ASC
            """)
    List<EventCustomField> findByEventIdAndTicketTypeId(
            @Param("eventId") Long eventId,
            @Param("ticketTypeId") Long ticketTypeId
    );

    /**
     * Counts the number of custom fields for an event
     */
    @Query("SELECT COUNT(ecf) FROM EventCustomField ecf WHERE ecf.event.id = :eventId")
    long countByEventId(@Param("eventId") Long eventId);

    /**
     * Counts the number of required fields for an event
     */
    @Query("""
            SELECT COUNT(ecf) FROM EventCustomField ecf 
            WHERE ecf.event.id = :eventId 
            AND ecf.required = true
            """)
    long countRequiredFieldsByEventId(@Param("eventId") Long eventId);

    /**
     * Finds the next available display order for an event
     */
    @Query("""
            SELECT COALESCE(MAX(ecf.displayOrder), 0) + 1 
            FROM EventCustomField ecf 
            WHERE ecf.event.id = :eventId
            """)
    Integer findNextDisplayOrder(@Param("eventId") Long eventId);

    /**
     * Checks if a field name already exists for an event
     */
    @Query("""
            SELECT CASE WHEN COUNT(ecf) > 0 THEN true ELSE false END 
            FROM EventCustomField ecf 
            WHERE ecf.event.id = :eventId 
            AND ecf.fieldName = :fieldName
            AND (:excludeId IS NULL OR ecf.id != :excludeId)
            """)
    boolean existsByEventIdAndFieldNameAndIdNot(
            @Param("eventId") Long eventId,
            @Param("fieldName") String fieldName,
            @Param("excludeId") Long excludeId
    );

    /**
     * Finds all fields with options (SELECT, RADIO, CHECKBOX)
     */
    @Query("""
            SELECT ecf FROM EventCustomField ecf 
            WHERE ecf.event.id = :eventId 
            AND ecf.fieldType IN ('SELECT', 'RADIO', 'CHECKBOX')
            AND ecf.fieldOptions IS NOT NULL
            ORDER BY ecf.displayOrder ASC
            """)
    List<EventCustomField> findFieldsWithOptions(@Param("eventId") Long eventId);

    /**
     * Deletes all custom fields for an event
     */
    void deleteByEventId(Long eventId);

    /**
     * Deletes all custom fields for a ticket type
     */
    void deleteByTicketTypeId(Long ticketTypeId);

    /**
     * Finds custom fields used in attendees
     */
    @Query("""
            SELECT DISTINCT ecf FROM EventCustomField ecf 
            JOIN AttendeeCustomField acf ON acf.eventField.id = ecf.id
            WHERE ecf.event.id = :eventId
            """)
    List<EventCustomField> findUsedFieldsByEventId(@Param("eventId") Long eventId);

    /**
     * Finds fields by event with pagination and sorting
     */
    @Query("""
            SELECT ecf FROM EventCustomField ecf 
            WHERE ecf.event.id = :eventId
            AND (:fieldType IS NULL OR ecf.fieldType = :fieldType)
            AND (:isRequired IS NULL OR ecf.required = :isRequired)
            ORDER BY 
                CASE WHEN :sortBy = 'displayOrder' THEN ecf.displayOrder END ASC,
                CASE WHEN :sortBy = 'fieldName' THEN ecf.fieldName END ASC,
                CASE WHEN :sortBy = 'fieldLabel' THEN ecf.fieldLabel END ASC
            """)
    List<EventCustomField> findByEventIdWithFilters(
            @Param("eventId") Long eventId,
            @Param("fieldType") FieldType fieldType,
            @Param("isRequired") Boolean isRequired,
            @Param("sortBy") String sortBy
    );

    /**
     * Updates the display order of multiple fields
     */
    @Query("""
            UPDATE EventCustomField ecf 
            SET ecf.displayOrder = ecf.displayOrder + :increment 
            WHERE ecf.event.id = :eventId 
            AND ecf.displayOrder >= :fromOrder
            """)
    void updateDisplayOrderFrom(
            @Param("eventId") Long eventId,
            @Param("fromOrder") Integer fromOrder,
            @Param("increment") Integer increment
    );

    /**
     * Finds duplicate fields (same name) in an event
     */
    @Query("""
            SELECT ecf.fieldName, COUNT(ecf) as count
            FROM EventCustomField ecf 
            WHERE ecf.event.id = :eventId
            GROUP BY ecf.fieldName
            HAVING COUNT(ecf) > 1
            """)
    List<Object[]> findDuplicateFieldNames(@Param("eventId") Long eventId);

    /**
     * Search fields by search term
     */
    @Query("""
            SELECT ecf FROM EventCustomField ecf 
            WHERE ecf.event.id = :eventId
            AND (
                LOWER(ecf.fieldName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
                LOWER(ecf.fieldLabel) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
                LOWER(ecf.placeholder) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            )
            ORDER BY ecf.displayOrder ASC
            """)
    List<EventCustomField> searchByEventIdAndTerm(
            @Param("eventId") Long eventId,
            @Param("searchTerm") String searchTerm
    );

    /**
     * Finds field usage statistics
     */
    @Query("""
            SELECT ecf.id, ecf.fieldName, ecf.fieldLabel, COUNT(acf.id) as usageCount
            FROM EventCustomField ecf 
            LEFT JOIN AttendeeCustomField acf ON acf.eventField.id = ecf.id
            WHERE ecf.event.id = :eventId
            GROUP BY ecf.id, ecf.fieldName, ecf.fieldLabel
            ORDER BY usageCount DESC, ecf.displayOrder ASC
            """)
    List<Object[]> findFieldUsageStatistics(@Param("eventId") Long eventId);

    /**
     * Finds unused fields (without values in AttendeeCustomField)
     */
    @Query("""
            SELECT ecf FROM EventCustomField ecf 
            WHERE ecf.event.id = :eventId
            AND NOT EXISTS (
                SELECT 1 FROM AttendeeCustomField acf 
                WHERE acf.eventField.id = ecf.id
            )
            ORDER BY ecf.displayOrder ASC
            """)
    List<EventCustomField> findUnusedFieldsByEventId(@Param("eventId") Long eventId);
}
