package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.dto.*;
import com.example.sportadministrationsystem.exception.NotFoundException;
import com.example.sportadministrationsystem.model.*;
import com.example.sportadministrationsystem.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {
    private final EventRepository eventRepository;
    private final PostRepository postRepository;

    private PostDto toDto(Post p) {
        return new PostDto(
                p.getId(), p.getEvent().getId(), p.getTitle(), p.getBody(), p.getPublishAt(),
                p.getStatus().name(), p.getAudience().name(), p.getChannel().name(),
                p.getExternalId(), p.getError(), p.isGenerated(), p.getCreatedAt(), p.getUpdatedAt()
        );
    }

    private Audience parseAudience(String s) {
        return Audience.valueOf(s.trim().toUpperCase());
    }

    private Channel parseChannel(String s) {
        return Channel.valueOf(s.trim().toUpperCase());
    }

    private PostStatus parseStatus(String s) {
        return PostStatus.valueOf(s.trim().toUpperCase());
    }

    @Transactional
    public PostDto create(Long eventId, PostPayload payload) {
        Event ev = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));
        Post p = Post.builder()
                .event(ev)
                .title(payload.title())
                .body(payload.body())
                .publishAt(payload.publishAt())
                .audience(parseAudience(payload.audience()))
                .channel(parseChannel(payload.channel()))
                .status(payload.status() == null ? PostStatus.DRAFT : parseStatus(payload.status()))
                .generated(false)
                .build();
        return toDto(postRepository.save(p));
    }

    @Transactional
    public PostDto update(Long eventId, Long postId, PostPayload payload) {
        Post p = postRepository.findById(postId).orElseThrow(() -> new NotFoundException("Post not found"));
        if (!p.getEvent().getId().equals(eventId)) throw new NotFoundException("Post not in event");
        p.setTitle(payload.title());
        p.setBody(payload.body());
        p.setPublishAt(payload.publishAt());
        p.setAudience(parseAudience(payload.audience()));
        p.setChannel(parseChannel(payload.channel()));
        if (payload.status() != null) p.setStatus(parseStatus(payload.status()));
        return toDto(postRepository.save(p));
    }

    @Transactional
    public void delete(Long eventId, Long postId) {
        Post p = postRepository.findById(postId).orElseThrow(() -> new NotFoundException("Post not found"));
        if (!p.getEvent().getId().equals(eventId)) throw new NotFoundException("Post not in event");
        postRepository.delete(p);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<PostDto> list(Long eventId, String status, String audience, String channel) {
        return postRepository.findByEventAndFilters(
                eventId,
                status == null || status.isBlank() ? null : parseStatus(status),
                audience == null || audience.isBlank() ? null : parseAudience(audience),
                channel == null || channel.isBlank() ? null : parseChannel(channel)
        ).stream().map(this::toDto).toList();
    }

    @Transactional
    public PostDto changeStatus(Long eventId, Long postId, PostStatusUpdateRequest req) {
        Post p = postRepository.findById(postId).orElseThrow(() -> new NotFoundException("Post not found"));
        if (!p.getEvent().getId().equals(eventId)) throw new NotFoundException("Post not in event");
        PostStatus next = parseStatus(req.status());
        p.setStatus(next);
        p.setError(req.error());
        return toDto(postRepository.save(p));
    }
}
