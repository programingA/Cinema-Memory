package com.cinemamemory.api.film;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "memory_scenes")
public class MemoryScene {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "film_id")
    private Film film;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "memory_date")
    private LocalDate memoryDate;

    private String location;

    private String mood;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "scene", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MediaAsset> mediaAssets = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MemoryScene() {
    }

    public MemoryScene(Film film, String title, String body, LocalDate memoryDate, String location, String mood, int sortOrder) {
        this.film = film;
        this.title = title;
        this.body = body;
        this.memoryDate = memoryDate;
        this.location = location;
        this.mood = mood;
        this.sortOrder = sortOrder;
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

    public Film getFilm() {
        return film;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public LocalDate getMemoryDate() {
        return memoryDate;
    }

    public String getLocation() {
        return location;
    }

    public String getMood() {
        return mood;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public List<MediaAsset> getMediaAssets() {
        return mediaAssets;
    }

    public void update(String title, String body, LocalDate memoryDate, String location, String mood, int sortOrder) {
        this.title = title;
        this.body = body;
        this.memoryDate = memoryDate;
        this.location = location;
        this.mood = mood;
        this.sortOrder = sortOrder;
    }

    public void updateSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
