package com.cinemamemory.api.film;

import com.cinemamemory.api.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "films")
public class Film {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    private String mood;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FilmVisibility visibility = FilmVisibility.PRIVATE;

    @OneToMany(mappedBy = "film", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemoryScene> scenes = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Film() {
    }

    public Film(User user, String title, String description, String coverImageUrl, String mood) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.mood = mood;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public String getMood() {
        return mood;
    }

    public FilmVisibility getVisibility() {
        return visibility;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<MemoryScene> getScenes() {
        return scenes;
    }

    public void update(String title, String description, String coverImageUrl, String mood) {
        this.title = title;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.mood = mood;
    }
}
