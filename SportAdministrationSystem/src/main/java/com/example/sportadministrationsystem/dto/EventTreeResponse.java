package com.example.sportadministrationsystem.dto;

import java.time.LocalDateTime;
import java.util.List;

public class EventTreeResponse {

    // Мінімальний «зріз» івента — щоб не тягнути весь EventDto і не ламати існуючі мапери
    public static class EventSummary {
        private Long id;
        private String name;
        private LocalDateTime startAt;
        private String location;
        private String category;     // code/name — як у тебе зручно
        private List<String> tags;   // список назв тегів

        public EventSummary() {}

        public EventSummary(Long id, String name, LocalDateTime startAt,
                            String location, String category, List<String> tags) {
            this.id = id;
            this.name = name;
            this.startAt = startAt;
            this.location = location;
            this.category = category;
            this.tags = tags;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public LocalDateTime getStartAt() { return startAt; }
        public String getLocation() { return location; }
        public String getCategory() { return category; }
        public List<String> getTags() { return tags; }

        public void setId(Long id) { this.id = id; }
        public void setName(String name) { this.name = name; }
        public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
        public void setLocation(String location) { this.location = location; }
        public void setCategory(String category) { this.category = category; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }

    private EventSummary event;
    private List<PostShortDto> posts;

    public EventTreeResponse() {}

    public EventTreeResponse(EventSummary event, List<PostShortDto> posts) {
        this.event = event;
        this.posts = posts;
    }

    public EventSummary getEvent() { return event; }
    public List<PostShortDto> getPosts() { return posts; }

    public void setEvent(EventSummary event) { this.event = event; }
    public void setPosts(List<PostShortDto> posts) { this.posts = posts; }
}
