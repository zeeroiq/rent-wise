package com.rentwise.backend.location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "cities",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"state_id", "code"},
        name = "uk_city_state_code"
    )
)
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "state_id", nullable = false, foreignKey = @ForeignKey(name = "fk_city_state"))
    private State state;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected City() {
    }

    public City(State state, String code, String name) {
        this.state = state;
        this.code = code;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public State getState() {
        return state;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

