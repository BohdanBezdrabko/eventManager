package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.dto.EventTreeResponse;
import com.example.sportadministrationsystem.dto.PostShortDto;
import com.example.sportadministrationsystem.model.Audience;
import com.example.sportadministrationsystem.model.Channel;
import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.Post;
import com.example.sportadministrationsystem.model.PostStatus;
import com.example.sportadministrationsystem.repository.EventRepository;
import com.example.sportadministrationsystem.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventTreeService {

    private final EventRepository eventRepository;
    private final PostRepository postRepository;

    @Transactional
    public List<PostShortDto> listPosts(Long eventId, PostStatus status, Audience audience, Channel channel) {
        List<Post> posts = postRepository.findByEventIdAndFilters(eventId, status, audience, channel);
        return posts.stream()
                .map(p -> new PostShortDto(
                        p.getId(),
                        p.getTitle(),
                        p.getPublishAt(),
                        p.getStatus(),
                        p.getAudience(),
                        p.getChannel(),
                        p.isGenerated()   // ← тут було getGenerated()
                ))
                .toList();
    }

    @Transactional
    public EventTreeResponse getTree(Long eventId) {
        Event event = eventRepository.findWithTagsById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        EventTreeResponse.EventSummary summary = new EventTreeResponse.EventSummary(
                event.getId(),
                event.getName(),
                event.getStartAt(),
                event.getLocation(),
                event.getCategory() == null ? null : event.getCategory().name(),
                event.getTags() == null ? List.of() : event.getTags().stream().map(t -> t.getName()).toList()
        );

        List<PostShortDto> posts = postRepository.findByEventIdAndFilters(eventId, null, null, null)
                .stream()
                .map(p -> new PostShortDto(
                        p.getId(),
                        p.getTitle(),
                        p.getPublishAt(),
                        p.getStatus(),
                        p.getAudience(),
                        p.getChannel(),
                        p.isGenerated()
                ))
                .toList();

        return new EventTreeResponse(summary, posts);
    }
}
