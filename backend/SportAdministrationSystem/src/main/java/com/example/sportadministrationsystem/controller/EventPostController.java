package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.dto.PostDto;
import com.example.sportadministrationsystem.dto.PostPayload;
import com.example.sportadministrationsystem.dto.PostStatusUpdateRequest;
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

    /** CREATE */
    @PostMapping
    public ResponseEntity<PostDto> create(@PathVariable Long eventId,
                                          @Valid @RequestBody PostPayload payload) {
        PostDto saved = postService.create(eventId, payload);
        return ResponseEntity.created(URI.create("/api/v1/events/" + eventId + "/posts/" + saved.id()))
                .body(saved);
    }

    /** LIST (?status=&audience=&channel=) */
    @GetMapping
    public ResponseEntity<List<PostDto>> list(@PathVariable Long eventId,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String audience,
                                              @RequestParam(required = false) String channel) {
        return ResponseEntity.ok(postService.list(eventId, status, audience, channel));
    }

    /** GET */
    @GetMapping("/{postId:\\d+}")
    public ResponseEntity<PostDto> get(@PathVariable Long eventId, @PathVariable Long postId) {
        return ResponseEntity.ok(postService.get(eventId, postId));
    }

    /** UPDATE */
    @PutMapping("/{postId:\\d+}")
    public ResponseEntity<PostDto> update(@PathVariable Long eventId,
                                          @PathVariable Long postId,
                                          @Valid @RequestBody PostPayload payload) {
        return ResponseEntity.ok(postService.update(eventId, postId, payload));
    }

    /** PATCH status */
    @PatchMapping("/{postId:\\d+}/status")
    public ResponseEntity<PostDto> patchStatus(@PathVariable Long eventId,
                                               @PathVariable Long postId,
                                               @RequestBody @Valid PostStatusUpdateRequest req) {
        return ResponseEntity.ok(postService.changeStatus(eventId, postId, req.status(), req.error()));
    }

    /** Publish now (ignores publishAt) */
    @PostMapping("/{postId:\\d+}/publish-now")
    public ResponseEntity<PostDto> publishNow(@PathVariable Long eventId, @PathVariable Long postId) {
        return ResponseEntity.ok(postService.publishNow(eventId, postId));
    }

    /** Ручний запуск диспатчу прострочених */
    @PostMapping("/_dispatch-tick")
    public ResponseEntity<Integer> dispatchTick(@PathVariable Long eventId) {
        // Лишаємо через PostService, як у тебе було.
        return ResponseEntity.ok(postService.dispatchDue());
    }

    /** DELETE */
    @DeleteMapping("/{postId:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable Long eventId, @PathVariable Long postId) {
        postService.delete(eventId, postId);
        return ResponseEntity.noContent().build();
    }
}
