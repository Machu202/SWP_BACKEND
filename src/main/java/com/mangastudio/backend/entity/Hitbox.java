package com.mangastudio.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "Hitbox")
public class Hitbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    @JsonIgnore
    private Page page;

    /** Null means this hitbox belongs to the current live page image. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_version_id")
    @JsonIgnore
    private PageVersion pageVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "x_coord", nullable = false)
    private Double xCoord;

    @Column(name = "y_coord", nullable = false)
    private Double yCoord;

    @Column(nullable = false)
    private Double width;

    @Column(nullable = false)
    private Double height;

    @OneToOne(mappedBy = "hitbox", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Task task;

    @OneToMany(mappedBy = "hitbox", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<HitboxComment> comments;

    public Hitbox() {}

    public Hitbox(Long id, Page page, PageVersion pageVersion, User createdBy, Double xCoord, Double yCoord,
                  Double width, Double height, Task task, List<HitboxComment> comments) {
        this.id = id;
        this.page = page;
        this.pageVersion = pageVersion;
        this.createdBy = createdBy;
        this.xCoord = xCoord;
        this.yCoord = yCoord;
        this.width = width;
        this.height = height;
        this.task = task;
        this.comments = comments;
    }

    public static HitboxBuilder builder() { return new HitboxBuilder(); }

    public static class HitboxBuilder {
        private Long id;
        private Page page;
        private PageVersion pageVersion;
        private User createdBy;
        private Double xCoord;
        private Double yCoord;
        private Double width;
        private Double height;
        private Task task;
        private List<HitboxComment> comments;

        public HitboxBuilder id(Long id) { this.id = id; return this; }
        public HitboxBuilder page(Page page) { this.page = page; return this; }
        public HitboxBuilder pageVersion(PageVersion pageVersion) { this.pageVersion = pageVersion; return this; }
        public HitboxBuilder createdBy(User createdBy) { this.createdBy = createdBy; return this; }
        public HitboxBuilder xCoord(Double xCoord) { this.xCoord = xCoord; return this; }
        public HitboxBuilder yCoord(Double yCoord) { this.yCoord = yCoord; return this; }
        public HitboxBuilder width(Double width) { this.width = width; return this; }
        public HitboxBuilder height(Double height) { this.height = height; return this; }
        public HitboxBuilder task(Task task) { this.task = task; return this; }
        public HitboxBuilder comments(List<HitboxComment> comments) { this.comments = comments; return this; }
        public Hitbox build() { return new Hitbox(id, page, pageVersion, createdBy, xCoord, yCoord, width, height, task, comments); }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Page getPage() { return page; }
    public void setPage(Page page) { this.page = page; }
    public PageVersion getPageVersion() { return pageVersion; }
    public void setPageVersion(PageVersion pageVersion) { this.pageVersion = pageVersion; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public Double getXCoord() { return xCoord; }
    public void setXCoord(Double xCoord) { this.xCoord = xCoord; }
    public Double getYCoord() { return yCoord; }
    public void setYCoord(Double yCoord) { this.yCoord = yCoord; }
    public Double getWidth() { return width; }
    public void setWidth(Double width) { this.width = width; }
    public Double getHeight() { return height; }
    public void setHeight(Double height) { this.height = height; }
    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }
    public List<HitboxComment> getComments() { return comments; }
    public void setComments(List<HitboxComment> comments) { this.comments = comments; }
}
