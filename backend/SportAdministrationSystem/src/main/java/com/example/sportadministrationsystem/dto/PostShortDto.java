package com.example.sportadministrationsystem.dto;

import com.example.sportadministrationsystem.model.Audience;
import com.example.sportadministrationsystem.model.Channel;
import com.example.sportadministrationsystem.model.PostStatus;

import java.time.LocalDateTime;

public class PostShortDto {
    private Long id;
    private String title;
    private LocalDateTime publishAt;
    private PostStatus status;
    private Audience audience;
    private Channel channel;
    private boolean generated;

    public PostShortDto() {}

    public PostShortDto(Long id, String title, LocalDateTime publishAt,
                        PostStatus status, Audience audience, Channel channel, boolean generated) {
        this.id = id;
        this.title = title;
        this.publishAt = publishAt;
        this.status = status;
        this.audience = audience;
        this.channel = channel;
        this.generated = generated;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public LocalDateTime getPublishAt() { return publishAt; }
    public PostStatus getStatus() { return status; }
    public Audience getAudience() { return audience; }
    public Channel getChannel() { return channel; }
    public boolean isGenerated() { return generated; }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setPublishAt(LocalDateTime publishAt) { this.publishAt = publishAt; }
    public void setStatus(PostStatus status) { this.status = status; }
    public void setAudience(Audience audience) { this.audience = audience; }
    public void setChannel(Channel channel) { this.channel = channel; }
    public void setGenerated(boolean generated) { this.generated = generated; }
}
