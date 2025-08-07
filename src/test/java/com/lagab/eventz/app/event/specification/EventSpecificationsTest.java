package com.lagab.eventz.app.event.specification;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import com.lagab.eventz.app.domain.event.dto.EventSearchDTO;
import com.lagab.eventz.app.domain.event.model.Address;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.model.EventStatus;
import com.lagab.eventz.app.domain.event.model.EventType;
import com.lagab.eventz.app.domain.event.specification.EventSpecifications;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventSpecifications Tests")
class EventSpecificationsTest {

    @Mock
    private Root<Event> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private Predicate mockPredicate;

    @Nested
    @DisplayName("hasKeyword Tests")
    class HasKeywordTests {

        @Test
        @DisplayName("Should return conjunction when keyword is null")
        void shouldReturnConjunctionWhenKeywordIsNull() {
            // Given
            when(criteriaBuilder.conjunction()).thenReturn(mockPredicate);
            Specification<Event> spec = EventSpecifications.hasKeyword(null);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).conjunction();
            assertEquals(mockPredicate, result);
        }

        @Test
        @DisplayName("Should return conjunction when keyword is empty")
        void shouldReturnConjunctionWhenKeywordIsEmpty() {
            // Given
            when(criteriaBuilder.conjunction()).thenReturn(mockPredicate);
            Specification<Event> spec = EventSpecifications.hasKeyword("   ");

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).conjunction();
            assertEquals(mockPredicate, result);
        }

        @Test
        @DisplayName("Should create OR predicate for name and description when keyword is provided")
        void shouldCreateOrPredicateWhenKeywordIsProvided() {
            // Given
            String keyword = "test";

            Path<String> namePath = mock(Path.class);
            Path<String> descriptionPath = mock(Path.class);
            Expression<String> lowerNameExpression = mock(Expression.class);
            Expression<String> lowerDescExpression = mock(Expression.class);
            Predicate namePredicate = mock(Predicate.class);
            Predicate descPredicate = mock(Predicate.class);

            when(root.get("name")).thenReturn((Path) namePath);
            when(root.get("description")).thenReturn((Path) descriptionPath);
            when(criteriaBuilder.lower(namePath)).thenReturn(lowerNameExpression);
            when(criteriaBuilder.lower(descriptionPath)).thenReturn(lowerDescExpression);
            when(criteriaBuilder.like(lowerNameExpression, "%test%")).thenReturn(namePredicate);
            when(criteriaBuilder.like(lowerDescExpression, "%test%")).thenReturn(descPredicate);
            when(criteriaBuilder.or(namePredicate, descPredicate)).thenReturn(mockPredicate);

            Specification<Event> spec = EventSpecifications.hasKeyword(keyword);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).or(namePredicate, descPredicate);
            assertEquals(mockPredicate, result);
        }
    }

    @Nested
    @DisplayName("hasType Tests")
    class HasTypeTests {

        @Test
        @DisplayName("Should return conjunction when type is null")
        void shouldReturnConjunctionWhenTypeIsNull() {
            // Given
            when(criteriaBuilder.conjunction()).thenReturn(mockPredicate);
            Specification<Event> spec = EventSpecifications.hasType(null);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).conjunction();
            assertEquals(mockPredicate, result);
        }

        @Test
        @DisplayName("Should create equal predicate when type is provided")
        void shouldCreateEqualPredicateWhenTypeIsProvided() {
            // Given
            EventType eventType = EventType.CONFERENCE;
            Path<EventType> typePath = mock(Path.class);

            when(root.get("type")).thenReturn((Path) typePath);
            when(criteriaBuilder.equal(typePath, eventType)).thenReturn(mockPredicate);

            Specification<Event> spec = EventSpecifications.hasType(eventType);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).equal(typePath, eventType);
            assertEquals(mockPredicate, result);
        }
    }

    @Nested
    @DisplayName("hasStatus Tests")
    class HasStatusTests {

        @Test
        @DisplayName("Should return conjunction when status is null")
        void shouldReturnConjunctionWhenStatusIsNull() {
            // Given
            when(criteriaBuilder.conjunction()).thenReturn(mockPredicate);
            Specification<Event> spec = EventSpecifications.hasStatus(null);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).conjunction();
            assertEquals(mockPredicate, result);
        }

        @Test
        @DisplayName("Should create equal predicate when status is provided")
        void shouldCreateEqualPredicateWhenStatusIsProvided() {
            // Given
            EventStatus status = EventStatus.PUBLISHED;
            Path<EventStatus> statusPath = mock(Path.class);

            when(root.get("status")).thenReturn((Path) statusPath);
            when(criteriaBuilder.equal(statusPath, status)).thenReturn(mockPredicate);

            Specification<Event> spec = EventSpecifications.hasStatus(status);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).equal(statusPath, status);
            assertEquals(mockPredicate, result);
        }
    }

    @Nested
    @DisplayName("hasCity Tests")
    class HasCityTests {

        @Test
        @DisplayName("Should return conjunction when city is null")
        void shouldReturnConjunctionWhenCityIsNull() {
            // Given
            when(criteriaBuilder.conjunction()).thenReturn(mockPredicate);
            Specification<Event> spec = EventSpecifications.hasCity(null);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).conjunction();
            assertEquals(mockPredicate, result);
        }

        @Test
        @DisplayName("Should return conjunction when city is empty")
        void shouldReturnConjunctionWhenCityIsEmpty() {
            // Given
            when(criteriaBuilder.conjunction()).thenReturn(mockPredicate);
            Specification<Event> spec = EventSpecifications.hasCity("   ");

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).conjunction();
            assertEquals(mockPredicate, result);
        }

        @Test
        @DisplayName("Should create equal predicate with lowercase city")
        void shouldCreateEqualPredicateWithLowercaseCity() {
            // Given
            String city = "PARIS";

            // Mock du Join pour la relation address
            Join<Event, Address> addressJoin = mock(Join.class);
            Path<String> cityPath = mock(Path.class);
            Expression<String> lowerCityExpression = mock(Expression.class);

            // Configuration des mocks pour le join
            when(root.join("address", JoinType.LEFT)).thenReturn((Join) addressJoin);
            when(addressJoin.get("city")).thenReturn((Path) cityPath);
            when(criteriaBuilder.lower(cityPath)).thenReturn(lowerCityExpression);
            when(criteriaBuilder.equal(lowerCityExpression, "paris")).thenReturn(mockPredicate);

            Specification<Event> spec = EventSpecifications.hasCity(city);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(root).join("address", JoinType.LEFT);
            verify(addressJoin).get("city");
            verify(criteriaBuilder).lower(cityPath);
            verify(criteriaBuilder).equal(lowerCityExpression, "paris");
            assertEquals(mockPredicate, result);
        }
    }

    @Nested
    @DisplayName("startsAfter Tests")
    class StartsAfterTests {

        @Test
        @DisplayName("Should return conjunction when startDate is null")
        void shouldReturnConjunctionWhenStartDateIsNull() {
            // Given
            when(criteriaBuilder.conjunction()).thenReturn(mockPredicate);
            Specification<Event> spec = EventSpecifications.startsAfter(null);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).conjunction();
            assertEquals(mockPredicate, result);
        }

        @Test
        @DisplayName("Should create greaterThanOrEqualTo predicate when startDate is provided")
        void shouldCreateGreaterThanOrEqualToPredicateWhenStartDateIsProvided() {
            // Given
            LocalDateTime startDate = LocalDateTime.now();
            Path<LocalDateTime> startDatePath = mock(Path.class);

            when(root.get("startDate")).thenReturn((Path) startDatePath);
            when(criteriaBuilder.greaterThanOrEqualTo(startDatePath, startDate)).thenReturn(mockPredicate);

            Specification<Event> spec = EventSpecifications.startsAfter(startDate);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).greaterThanOrEqualTo(startDatePath, startDate);
            assertEquals(mockPredicate, result);
        }
    }

    @Nested
    @DisplayName("endsBefore Tests")
    class EndsBeforeTests {

        @Test
        @DisplayName("Should return conjunction when endDate is null")
        void shouldReturnConjunctionWhenEndDateIsNull() {
            // Given
            when(criteriaBuilder.conjunction()).thenReturn(mockPredicate);
            Specification<Event> spec = EventSpecifications.endsBefore(null);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).conjunction();
            assertEquals(mockPredicate, result);
        }

        @Test
        @DisplayName("Should create lessThanOrEqualTo predicate when endDate is provided")
        void shouldCreateLessThanOrEqualToPredicateWhenEndDateIsProvided() {
            // Given
            LocalDateTime endDate = LocalDateTime.now();
            Path<LocalDateTime> endDatePath = mock(Path.class);

            when(root.get("endDate")).thenReturn((Path) endDatePath);
            when(criteriaBuilder.lessThanOrEqualTo(endDatePath, endDate)).thenReturn(mockPredicate);

            Specification<Event> spec = EventSpecifications.endsBefore(endDate);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).lessThanOrEqualTo(endDatePath, endDate);
            assertEquals(mockPredicate, result);
        }
    }

    @Nested
    @DisplayName("isFree Tests")
    class IsFreeTests {

        @Test
        @DisplayName("Should return conjunction when isFree is null")
        void shouldReturnConjunctionWhenIsFreeIsNull() {
            // Given
            when(criteriaBuilder.conjunction()).thenReturn(mockPredicate);
            Specification<Event> spec = EventSpecifications.isFree(null);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).conjunction();
            assertEquals(mockPredicate, result);
        }

        @Test
        @DisplayName("Should create equal predicate when isFree is provided")
        void shouldCreateEqualPredicateWhenIsFreeIsProvided() {
            // Given
            Boolean isFree = true;
            Path<Boolean> isFreePath = mock(Path.class);

            when(root.get("isFree")).thenReturn((Path) isFreePath);
            when(criteriaBuilder.equal(isFreePath, isFree)).thenReturn(mockPredicate);

            Specification<Event> spec = EventSpecifications.isFree(isFree);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).equal(isFreePath, isFree);
            assertEquals(mockPredicate, result);
        }
    }

    @Nested
    @DisplayName("isPublic Tests")
    class IsPublicTests {

        @Test
        @DisplayName("Should create isTrue predicate for isPublic field")
        void shouldCreateIsTruePredicateForIsPublicField() {
            // Given
            Path<Boolean> isPublicPath = mock(Path.class);

            when(root.get("isPublic")).thenReturn((Path) isPublicPath);
            when(criteriaBuilder.isTrue(isPublicPath)).thenReturn(mockPredicate);

            Specification<Event> spec = EventSpecifications.isPublic();

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).isTrue(isPublicPath);
            assertEquals(mockPredicate, result);
        }
    }

    @Nested
    @DisplayName("isWithinRadius Tests")
    class IsWithinRadiusTests {

        @Test
        @DisplayName("Should return conjunction when latitude is null")
        void shouldReturnConjunctionWhenLatitudeIsNull() {
            // Given
            when(criteriaBuilder.conjunction()).thenReturn(mockPredicate);
            Specification<Event> spec = EventSpecifications.isWithinRadius(null, 2.0, 10.0);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).conjunction();
            assertEquals(mockPredicate, result);
        }

        @Test
        @DisplayName("Should return conjunction when longitude is null")
        void shouldReturnConjunctionWhenLongitudeIsNull() {
            // Given
            when(criteriaBuilder.conjunction()).thenReturn(mockPredicate);
            Specification<Event> spec = EventSpecifications.isWithinRadius(1.0, null, 10.0);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).conjunction();
            assertEquals(mockPredicate, result);
        }

        @Test
        @DisplayName("Should return conjunction when radius is null")
        void shouldReturnConjunctionWhenRadiusIsNull() {
            // Given
            when(criteriaBuilder.conjunction()).thenReturn(mockPredicate);
            Specification<Event> spec = EventSpecifications.isWithinRadius(1.0, 2.0, null);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).conjunction();
            assertEquals(mockPredicate, result);
        }

        @Test
        @DisplayName("Should create distance calculation predicate when all parameters are provided")
        void shouldCreateDistanceCalculationPredicateWhenAllParametersAreProvided() {
            // Given
            Double latitude = 48.8566;
            Double longitude = 2.3522;
            Double radius = 10.0;

            Path<Object> addressPath = mock(Path.class);
            Path<Double> latitudePath = mock(Path.class);
            Path<Double> longitudePath = mock(Path.class);
            Expression<Double> diffLatExpression = mock(Expression.class);
            Expression<Double> diffLngExpression = mock(Expression.class);
            Expression<Double> powerLatExpression = mock(Expression.class);
            Expression<Double> powerLngExpression = mock(Expression.class);
            Expression<Double> sumExpression = mock(Expression.class);
            Expression<Double> sqrtExpression = mock(Expression.class);
            Expression<Integer> literalExpression = mock(Expression.class);

            when(root.get("address")).thenReturn(addressPath);
            when(addressPath.get("latitude")).thenReturn((Path) latitudePath);
            when(addressPath.get("longitude")).thenReturn((Path) longitudePath);
            when(criteriaBuilder.diff(latitudePath, latitude)).thenReturn(diffLatExpression);
            when(criteriaBuilder.diff(longitudePath, longitude)).thenReturn(diffLngExpression);
            when(criteriaBuilder.literal(2)).thenReturn(literalExpression);
            when(criteriaBuilder.function("POWER", Double.class, diffLatExpression, literalExpression)).thenReturn(powerLatExpression);
            when(criteriaBuilder.function("POWER", Double.class, diffLngExpression, literalExpression)).thenReturn(powerLngExpression);
            when(criteriaBuilder.sum(powerLatExpression, powerLngExpression)).thenReturn(sumExpression);
            when(criteriaBuilder.function("SQRT", Double.class, sumExpression)).thenReturn(sqrtExpression);
            when(criteriaBuilder.lessThanOrEqualTo(sqrtExpression, radius / 111.0)).thenReturn(mockPredicate);

            Specification<Event> spec = EventSpecifications.isWithinRadius(latitude, longitude, radius);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).function("SQRT", Double.class, sumExpression);
            verify(criteriaBuilder).lessThanOrEqualTo(sqrtExpression, radius / 111.0);
            assertEquals(mockPredicate, result);
        }
    }

    @Nested
    @DisplayName("hasAvailableTickets Tests")
    class HasAvailableTicketsTests {

        @Test
        @DisplayName("Should create join and compare quantities")
        void shouldCreateJoinAndCompareQuantities() {
            // Given
            Join<Object, Object> ticketTypesJoin = mock(Join.class);
            Path<Integer> quantityAvailablePath = mock(Path.class);
            Path<Integer> quantitySoldPath = mock(Path.class);

            when(root.join("ticketTypes")).thenReturn(ticketTypesJoin);
            when(ticketTypesJoin.get("quantityAvailable")).thenReturn((Path) quantityAvailablePath);
            when(ticketTypesJoin.get("quantitySold")).thenReturn((Path) quantitySoldPath);
            when(criteriaBuilder.greaterThan(quantityAvailablePath, quantitySoldPath)).thenReturn(mockPredicate);

            Specification<Event> spec = EventSpecifications.hasAvailableTickets();

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(root).join("ticketTypes");
            verify(criteriaBuilder).greaterThan(quantityAvailablePath, quantitySoldPath);
            assertEquals(mockPredicate, result);
        }
    }

    @Nested
    @DisplayName("hasOrganizer Tests")
    class HasOrganizerTests {

        @Test
        @DisplayName("Should return conjunction when organizerId is null")
        void shouldReturnConjunctionWhenOrganizerIdIsNull() {
            // Given
            when(criteriaBuilder.conjunction()).thenReturn(mockPredicate);
            Specification<Event> spec = EventSpecifications.hasOrganizer(null);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).conjunction();
            assertEquals(mockPredicate, result);
        }

        @Test
        @DisplayName("Should create equal predicate when organizerId is provided")
        void shouldCreateEqualPredicateWhenOrganizerIdIsProvided() {
            // Given
            Long organizerId = 123L;
            Path<Object> organizerPath = mock(Path.class);
            Path<Long> organizerIdPath = mock(Path.class);

            when(root.get("organizer")).thenReturn(organizerPath);
            when(organizerPath.get("id")).thenReturn((Path) organizerIdPath);
            when(criteriaBuilder.equal(organizerIdPath, organizerId)).thenReturn(mockPredicate);

            Specification<Event> spec = EventSpecifications.hasOrganizer(organizerId);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).equal(organizerIdPath, organizerId);
            assertEquals(mockPredicate, result);
        }
    }

    @Nested
    @DisplayName("withCriteria Tests")
    class WithCriteriaTests {

        @Test
        @DisplayName("Should create specification with only public filter when EventSearchDTO is empty")
        void shouldCreateSpecificationWithOnlyPublicFilterWhenEventSearchDTOIsEmpty() {
            // Given
            EventSearchDTO searchDTO = new EventSearchDTO(
                    null, null, null, null, null, null, null, null, null, null, null, null
            );

            Path<Boolean> isPublicPath = mock(Path.class);
            Predicate publicPredicate = mock(Predicate.class);

            when(root.get("isPublic")).thenReturn((Path) isPublicPath);
            when(criteriaBuilder.isTrue(isPublicPath)).thenReturn(publicPredicate);
            when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(mockPredicate);

            Specification<Event> spec = EventSpecifications.withCriteria(searchDTO);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).isTrue(isPublicPath);
            verify(criteriaBuilder).and(any(Predicate[].class));
            assertEquals(mockPredicate, result);
        }

        @Test
        @DisplayName("Should handle empty strings correctly")
        void shouldHandleEmptyStringsCorrectly() {
            // Given
            EventSearchDTO searchDTO = new EventSearchDTO(
                    "   ", null, null, "   ", null, null, null, null, null, null, null, null
            );

            Path<Boolean> isPublicPath = mock(Path.class);
            Predicate publicPredicate = mock(Predicate.class);

            when(root.get("isPublic")).thenReturn((Path) isPublicPath);
            when(criteriaBuilder.isTrue(isPublicPath)).thenReturn(publicPredicate);
            when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(mockPredicate);

            Specification<Event> spec = EventSpecifications.withCriteria(searchDTO);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).isTrue(isPublicPath);
            verify(criteriaBuilder, never()).like(any(Expression.class), anyString());
            verify(criteriaBuilder).and(any(Predicate[].class));
            assertEquals(mockPredicate, result);
        }

        @Test
        @DisplayName("Should handle partial geographic criteria")
        void shouldHandlePartialGeographicCriteria() {
            // Given
            EventSearchDTO searchDTO = new EventSearchDTO(
                    null, null, null, null, null, null, null, 48.8566, null, 10.0, null, null
            );

            Path<Boolean> isPublicPath = mock(Path.class);
            Predicate publicPredicate = mock(Predicate.class);

            when(root.get("isPublic")).thenReturn((Path) isPublicPath);
            when(criteriaBuilder.isTrue(isPublicPath)).thenReturn(publicPredicate);
            when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(mockPredicate);

            Specification<Event> spec = EventSpecifications.withCriteria(searchDTO);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).isTrue(isPublicPath);
            verify(criteriaBuilder, never()).function(eq("SQRT"), any(), any());
            verify(criteriaBuilder).and(any(Predicate[].class));
            assertEquals(mockPredicate, result);
        }

        private void setupCompleteWithCriteriaMocks(LocalDateTime startDate, LocalDateTime endDate) {
            // Setup all paths and expressions needed for withCriteria test
            Path<String> namePath = mock(Path.class);
            Path<String> descriptionPath = mock(Path.class);
            Path<EventType> typePath = mock(Path.class);
            Path<EventStatus> statusPath = mock(Path.class);
            Path<Object> addressPath = mock(Path.class);
            Path<String> cityPath = mock(Path.class);
            Path<LocalDateTime> startDatePath = mock(Path.class);
            Path<LocalDateTime> endDatePath = mock(Path.class);
            Path<Boolean> isFreePath = mock(Path.class);
            Path<Boolean> isPublicPath = mock(Path.class);
            Path<Object> organizerPath = mock(Path.class);
            Path<Long> organizerIdPath = mock(Path.class);

            Expression<String> lowerNameExpression = mock(Expression.class);
            Expression<String> lowerDescExpression = mock(Expression.class);
            Expression<String> lowerCityExpression = mock(Expression.class);

            when(root.get("name")).thenReturn((Path) namePath);
            when(root.get("description")).thenReturn((Path) descriptionPath);
            when(root.get("type")).thenReturn((Path) typePath);
            when(root.get("status")).thenReturn((Path) statusPath);
            when(root.get("address")).thenReturn(addressPath);
            when(root.get("startDate")).thenReturn((Path) startDatePath);
            when(root.get("endDate")).thenReturn((Path) endDatePath);
            when(root.get("isFree")).thenReturn((Path) isFreePath);
            when(root.get("isPublic")).thenReturn((Path) isPublicPath);
            when(root.get("organizer")).thenReturn(organizerPath);

            when(addressPath.get("city")).thenReturn((Path) cityPath);
            when(organizerPath.get("id")).thenReturn((Path) organizerIdPath);

            when(criteriaBuilder.lower(namePath)).thenReturn(lowerNameExpression);
            when(criteriaBuilder.lower(descriptionPath)).thenReturn(lowerDescExpression);
            when(criteriaBuilder.lower(cityPath)).thenReturn(lowerCityExpression);

            when(criteriaBuilder.like(any(Expression.class), anyString())).thenReturn(mock(Predicate.class));
            when(criteriaBuilder.or(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));
            when(criteriaBuilder.equal(any(Expression.class), any())).thenReturn(mock(Predicate.class));
            when(criteriaBuilder.greaterThanOrEqualTo(startDatePath, startDate)).thenReturn(mock(Predicate.class));
            when(criteriaBuilder.lessThanOrEqualTo(endDatePath, endDate)).thenReturn(mock(Predicate.class));
            when(criteriaBuilder.isTrue(isPublicPath)).thenReturn(mock(Predicate.class));
            when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(mockPredicate);
        }
    }

    @Nested
    @DisplayName("Convenience Methods Tests")
    class ConvenienceMethodsTests {

        @Test
        @DisplayName("publicEvents should return isPublic specification")
        void publicEventsShouldReturnIsPublicSpecification() {
            // Given
            Path<Boolean> isPublicPath = mock(Path.class);

            when(root.get("isPublic")).thenReturn((Path) isPublicPath);
            when(criteriaBuilder.isTrue(isPublicPath)).thenReturn(mockPredicate);

            Specification<Event> spec = EventSpecifications.publicEvents();

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).isTrue(isPublicPath);
            assertEquals(mockPredicate, result);
        }

        @Test
        @DisplayName("publishedEvents should return hasStatus(PUBLISHED) specification")
        void publishedEventsShouldReturnHasStatusPublishedSpecification() {
            // Given
            Path<EventStatus> statusPath = mock(Path.class);

            when(root.get("status")).thenReturn((Path) statusPath);
            when(criteriaBuilder.equal(statusPath, EventStatus.PUBLISHED)).thenReturn(mockPredicate);

            Specification<Event> spec = EventSpecifications.publishedEvents();

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).equal(statusPath, EventStatus.PUBLISHED);
            assertEquals(mockPredicate, result);
        }

        @Test
        @DisplayName("upcomingEvents should return startsAfter(now) specification")
        void upcomingEventsShouldReturnStartsAfterNowSpecification() {
            // Given
            Path<LocalDateTime> startDatePath = mock(Path.class);

            when(root.get("startDate")).thenReturn((Path) startDatePath);
            when(criteriaBuilder.greaterThanOrEqualTo(eq(startDatePath), any(LocalDateTime.class))).thenReturn(mockPredicate);

            Specification<Event> spec = EventSpecifications.upcomingEvents();

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).greaterThanOrEqualTo(eq(startDatePath), any(LocalDateTime.class));
            assertEquals(mockPredicate, result);
        }

        @Test
        @DisplayName("freeEvents should return isFree(true) specification")
        void freeEventsShouldReturnIsFreeeTrueSpecification() {
            // Given
            Path<Boolean> isFreePath = mock(Path.class);

            when(root.get("isFree")).thenReturn((Path) isFreePath);
            when(criteriaBuilder.equal(isFreePath, true)).thenReturn(mockPredicate);

            Specification<Event> spec = EventSpecifications.freeEvents();

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).equal(isFreePath, true);
            assertEquals(mockPredicate, result);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long keyword")
        void shouldHandleVeryLongKeyword() {
            // Given
            String longKeyword = "a".repeat(1000);

            Path<String> namePath = mock(Path.class);
            Path<String> descriptionPath = mock(Path.class);
            Expression<String> lowerNameExpression = mock(Expression.class);
            Expression<String> lowerDescExpression = mock(Expression.class);
            Predicate namePredicate = mock(Predicate.class);
            Predicate descPredicate = mock(Predicate.class);

            when(root.get("name")).thenReturn((Path) namePath);
            when(root.get("description")).thenReturn((Path) descriptionPath);
            when(criteriaBuilder.lower(namePath)).thenReturn(lowerNameExpression);
            when(criteriaBuilder.lower(descriptionPath)).thenReturn(lowerDescExpression);
            when(criteriaBuilder.like(lowerNameExpression, "%" + longKeyword.toLowerCase() + "%")).thenReturn(namePredicate);
            when(criteriaBuilder.like(lowerDescExpression, "%" + longKeyword.toLowerCase() + "%")).thenReturn(descPredicate);
            when(criteriaBuilder.or(namePredicate, descPredicate)).thenReturn(mockPredicate);

            Specification<Event> spec = EventSpecifications.hasKeyword(longKeyword);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).like(lowerNameExpression, "%" + longKeyword.toLowerCase() + "%");
            assertEquals(mockPredicate, result);
        }

        @Test
        @DisplayName("Should handle special characters in keyword")
        void shouldHandleSpecialCharactersInKeyword() {
            // Given
            String specialKeyword = "test@#$%^&*()";

            Path<String> namePath = mock(Path.class);
            Path<String> descriptionPath = mock(Path.class);
            Expression<String> lowerNameExpression = mock(Expression.class);
            Expression<String> lowerDescExpression = mock(Expression.class);
            Predicate namePredicate = mock(Predicate.class);
            Predicate descPredicate = mock(Predicate.class);

            when(root.get("name")).thenReturn((Path) namePath);
            when(root.get("description")).thenReturn((Path) descriptionPath);
            when(criteriaBuilder.lower(namePath)).thenReturn(lowerNameExpression);
            when(criteriaBuilder.lower(descriptionPath)).thenReturn(lowerDescExpression);
            when(criteriaBuilder.like(lowerNameExpression, "%" + specialKeyword.toLowerCase() + "%")).thenReturn(namePredicate);
            when(criteriaBuilder.like(lowerDescExpression, "%" + specialKeyword.toLowerCase() + "%")).thenReturn(descPredicate);
            when(criteriaBuilder.or(namePredicate, descPredicate)).thenReturn(mockPredicate);

            Specification<Event> spec = EventSpecifications.hasKeyword(specialKeyword);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).like(lowerNameExpression, "%" + specialKeyword.toLowerCase() + "%");
            assertEquals(mockPredicate, result);
        }

        @Test
        @DisplayName("Should handle zero radius")
        void shouldHandleZeroRadius() {
            // Given
            Double latitude = 48.8566;
            Double longitude = 2.3522;
            Double radius = 0.0;

            Path<Object> addressPath = mock(Path.class);
            Path<Double> latitudePath = mock(Path.class);
            Path<Double> longitudePath = mock(Path.class);
            Expression<Double> sqrtExpression = mock(Expression.class);

            when(root.get("address")).thenReturn(addressPath);
            when(addressPath.get("latitude")).thenReturn((Path) latitudePath);
            when(addressPath.get("longitude")).thenReturn((Path) longitudePath);

            // Setup the complex function chain
            setupGeographicCalculationMocks(latitude, longitude, latitudePath, longitudePath, sqrtExpression);

            when(criteriaBuilder.lessThanOrEqualTo(sqrtExpression, 0.0)).thenReturn(mockPredicate);

            Specification<Event> spec = EventSpecifications.isWithinRadius(latitude, longitude, radius);

            // When
            Predicate result = spec.toPredicate(root, query, criteriaBuilder);

            // Then
            verify(criteriaBuilder).lessThanOrEqualTo(sqrtExpression, 0.0);
            assertEquals(mockPredicate, result);
        }

        private void setupGeographicCalculationMocks(Double latitude, Double longitude,
                Path<Double> latitudePath, Path<Double> longitudePath,
                Expression<Double> sqrtExpression) {
            Expression<Double> diffLatExpression = mock(Expression.class);
            Expression<Double> diffLngExpression = mock(Expression.class);
            Expression<Double> powerLatExpression = mock(Expression.class);
            Expression<Double> powerLngExpression = mock(Expression.class);
            Expression<Double> sumExpression = mock(Expression.class);
            Expression<Integer> literalExpression = mock(Expression.class);

            when(criteriaBuilder.diff(latitudePath, latitude)).thenReturn(diffLatExpression);
            when(criteriaBuilder.diff(longitudePath, longitude)).thenReturn(diffLngExpression);
            when(criteriaBuilder.literal(2)).thenReturn(literalExpression);
            when(criteriaBuilder.function("POWER", Double.class, diffLatExpression, literalExpression)).thenReturn(powerLatExpression);
            when(criteriaBuilder.function("POWER", Double.class, diffLngExpression, literalExpression)).thenReturn(powerLngExpression);
            when(criteriaBuilder.sum(powerLatExpression, powerLngExpression)).thenReturn(sumExpression);
            when(criteriaBuilder.function("SQRT", Double.class, sumExpression)).thenReturn(sqrtExpression);
        }
    }

}

