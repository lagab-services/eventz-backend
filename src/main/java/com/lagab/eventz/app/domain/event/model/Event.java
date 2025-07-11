package com.lagab.eventz.app.domain.event.model;

import java.time.LocalDateTime;
import java.util.List;

import com.lagab.eventz.app.domain.payment.entity.Order;
import com.lagab.eventz.app.domain.ticket.entity.TicketType;
import com.lagab.eventz.app.domain.user.model.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "short_description", length = 500)
    private String summary;

    @Column(name = "surtitle", length = 100)
    private String surtitle;

    @Column(name = "subtitle", length = 100)
    private String subtitle;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "registration_start")
    private LocalDateTime registrationStart;

    @Column(name = "registration_end")
    private LocalDateTime registrationEnd;

    @Enumerated(EnumType.STRING)
    private EventStatus status = EventStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    private EventType type;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "website_url")
    private String website;

    @Column(name = "max_attendees")
    private Integer maxAttendees;

    @Column(name = "is_public")
    private Boolean isPublic = true;

    @Column(name = "is_free")
    private Boolean isFree = false;

    @Column(name = "currency")
    private String currency = "EUR";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Relations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @OneToOne(mappedBy = "event", cascade = CascadeType.ALL)
    private Address address;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private List<TicketType> ticketTypes;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private List<Order> orders;

    /*@OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private List<Review> reviews;*/

   /* @ManyToMany
    @JoinTable(
            name = "event_categories",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<Category> categories;*/
}
