package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.dto.*;
import com.example.sportadministrationsystem.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events/{eventId:\\d+}/posts")
public class EventPostController {
    private final PostService postService;

    @PostMapping
    public ResponseEntity<PostDto> create(@PathVariable Long eventId, @Valid @RequestBody PostPayload payload) {
        PostDto saved = postService.create(eventId, payload);
        return ResponseEntity.created(URI.create("/api/v1/events/" + eventId + "/posts/" + saved.id())).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<PostDto>> list(@PathVariable Long eventId,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String audience,
                                              @RequestParam(required = false) String channel) {
        return ResponseEntity.ok(postService.list(eventId, status, audience, channel));
    }

    @PutMapping("/{postId:\\d+}")
    public ResponseEntity<PostDto> update(@PathVariable Long eventId,
                                          @PathVariable Long postId,
                                          @Valid @RequestBody PostPayload payload) {
        return ResponseEntity.ok(postService.update(eventId, postId, payload));
    }

    @PatchMapping("/{postId:\\d+}/status")
    public ResponseEntity<PostDto> changeStatus(@PathVariable Long eventId,
                                                @PathVariable Long postId,
                                                @Valid @RequestBody PostStatusUpdateRequest req) {
        return ResponseEntity.ok(postService.changeStatus(eventId, postId, req));
    }

    @DeleteMapping("/{postId:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable Long eventId, @PathVariable Long postId) {
        postService.delete(eventId, postId);
        return ResponseEntity.noContent().build();
    }
}
