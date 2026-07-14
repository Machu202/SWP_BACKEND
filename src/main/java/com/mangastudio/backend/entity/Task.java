package com.mangastudio.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hitbox_id", nullable = false, unique = true)
    private Hitbox hitbox;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mangaka_id", nullable = false)
    private User mangaka;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assistant_id")
    private User assistant;

    @Column(length = 50)
    private String status;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    /**
     * Immutable snapshot of the page image that the Mangaka used when the task
     * was created. It must not be replaced by the Assistant submission.
     */
    @Column(name = "reference_image_url", length = 500)
    private String referenceImageUrl;

    @Column(name = "submitted_image_url", length = 500)
    private String submittedImageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Task() {
    }

    public Task(Long id, Hitbox hitbox, User mangaka, User assistant, String status,
                String description, String referenceImageUrl, String submittedImageUrl,
                LocalDateTime createdAt) {
        this.id = id;
        this.hitbox = hitbox;
        this.mangaka = mangaka;
        this.assistant = assistant;
        this.status = status;
        this.description = description;
        this.referenceImageUrl = referenceImageUrl;
        this.submittedImageUrl = submittedImageUrl;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long id;
        private Hitbox hitbox;
        private User mangaka;
        private User assistant;
        private String status;
        private String description;
        private String referenceImageUrl;
        private String submittedImageUrl;
        private LocalDateTime createdAt;

        private Builder() {
        }

        public Builder id(Long id) { this.id = id; return this; }
        public Builder hitbox(Hitbox hitbox) { this.hitbox = hitbox; return this; }
        public Builder mangaka(User mangaka) { this.mangaka = mangaka; return this; }
        public Builder assistant(User assistant) { this.assistant = assistant; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder referenceImageUrl(String referenceImageUrl) { this.referenceImageUrl = referenceImageUrl; return this; }
        public Builder submittedImageUrl(String submittedImageUrl) { this.submittedImageUrl = submittedImageUrl; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Task build() {
            return new Task(id, hitbox, mangaka, assistant, status, description,
                    referenceImageUrl, submittedImageUrl, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Hitbox getHitbox() { return hitbox; }
    public void setHitbox(Hitbox hitbox) { this.hitbox = hitbox; }
    public User getMangaka() { return mangaka; }
    public void setMangaka(User mangaka) { this.mangaka = mangaka; }
    public User getAssistant() { return assistant; }
    public void setAssistant(User assistant) { this.assistant = assistant; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReferenceImageUrl() { return referenceImageUrl; }
    public void setReferenceImageUrl(String referenceImageUrl) { this.referenceImageUrl = referenceImageUrl; }
    public String getSubmittedImageUrl() { return submittedImageUrl; }
    public void setSubmittedImageUrl(String submittedImageUrl) { this.submittedImageUrl = submittedImageUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
