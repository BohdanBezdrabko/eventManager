// File: src/main/java/com/example/sportadministrationsystem/controller/EventPostsController.java
package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.dto.EventTreeResponse;
import com.example.sportadministrationsystem.dto.PostDto;
import com.example.sportadministrationsystem.dto.PostPayload;
import com.example.sportadministrationsystem.dto.PostShortDto;
import com.example.sportadministrationsystem.dto.PostStatusUpdateRequest;
import com.example.sportadministrationsystem.model.Audience;
import com.example.sportadministrationsystem.model.Channel;
import com.example.sportadministrationsystem.model.PostStatus;
import com.example.sportadministrationsystem.service.EventTreeService;
import com.example.sportadministrationsystem.service.PostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

/**
 * Unified controller: all event posts endpoints + tree endpoint live here.
 *
 * Endpoints kept backward compatible:
 *  - POST   /api/v1/events/{eventId}/posts               (create)
 *  - GET    /api/v1/events/{eventId}/posts               (list full PostDto; filters: status/audience/channel)
 *  - GET    /api/v1/events/{eventId}/posts/short         (list short PostShortDto; filters: status/audience/channel)
 *  - GET    /api/v1/events/{eventId}/posts/{postId}      (get)
 *  - PUT    /api/v1/events/{eventId}/posts/{postId}      (update)
 *  - PATCH  /api/v1/events/{eventId}/posts/{postId}/status (changeStatus)
 *  - POST   /api/v1/events/{eventId}/posts/{postId}/publish-now (publish now)
 *  - POST   /api/v1/events/{eventId}/posts/_dispatch-tick (manual dispatch)
 *  - DELETE /api/v1/events/{eventId}/posts/{postId}      (delete)
 *  - GET    /api/v1/events/{eventId}/tree                (event posts tree / view)
 *
 * NOTE: Remove old EventPostController and EventPostQueryController to avoid route duplication.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events/{eventId:\\d+}")
public class EventPostController {

    private final PostService postService;
    private final EventTreeService treeService;

    // ---- CREATE ----
    @PostMapping("/posts")
    public ResponseEntity<PostDto> create(@PathVariable @Min(1) Long eventId,
                                          @Valid @RequestBody PostPayload payload) {
        PostDto saved = postService.create(eventId, payload);
        return ResponseEntity.created(URI.create("/api/v1/events/" + eventId + "/posts/" + saved.id()))
                .body(saved);
    }

    // ---- LIST (full DTO) ----
    @GetMapping("/posts")
    public ResponseEntity<List<PostDto>> list(@PathVariable @Min(1) Long eventId,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String audience,
                                              @RequestParam(required = false) String channel) {
        // Let service parse/validate strings (backwards compatible with your current PostService)
        return ResponseEntity.ok(postService.list(eventId, status, audience, channel));
    }

    // ---- LIST (short DTO) ----
    @GetMapping("/posts/short")
    public List<PostShortDto> listShort(@PathVariable @Min(1) Long eventId,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String audience,
                                        @RequestParam(required = false) String channel) {
        return treeService.listPosts(
                eventId,
                parseStatus(status),
                parseAudience(audience),
                parseChannel(channel)
        );
    }

    // ---- GET ----
    @GetMapping("/posts/{postId:\\d+}")
    public ResponseEntity<PostDto> get(@PathVariable @Min(1) Long eventId,
                                       @PathVariable @Min(1) Long postId) {
        return ResponseEntity.ok(postService.get(eventId, postId));
    }

    // ---- UPDATE ----
    @PutMapping("/posts/{postId:\\d+}")
    public ResponseEntity<PostDto> update(@PathVariable @Min(1) Long eventId,
                                          @PathVariable @Min(1) Long postId,
                                          @Valid @RequestBody PostPayload payload) {
        return ResponseEntity.ok(postService.update(eventId, postId, payload));
    }

    // ---- PATCH status ----
    @PatchMapping("/posts/{postId:\\d+}/status")
    public ResponseEntity<PostDto> patchStatus(@PathVariable @Min(1) Long eventId,
                                               @PathVariable @Min(1) Long postId,
                                               @RequestBody @Valid PostStatusUpdateRequest req) {
        return ResponseEntity.ok(postService.changeStatus(eventId, postId, req.status(), req.error()));
    }

    // ---- Publish now ----
    @PostMapping("/posts/{postId:\\d+}/publish-now")
    public ResponseEntity<PostDto> publishNow(@PathVariable @Min(1) Long eventId,
                                              @PathVariable @Min(1) Long postId) {
        return ResponseEntity.ok(postService.publishNow(eventId, postId));
    }

    // ---- Manual dispatcher tick (optional) ----
    @PostMapping("/posts/_dispatch-tick")
    public ResponseEntity<Integer> dispatchTick(@PathVariable @Min(1) Long eventId) {
        return ResponseEntity.ok(postService.dispatchDue());
    }

    // ---- DELETE ----
    @DeleteMapping("/posts/{postId:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable @Min(1) Long eventId,
                                       @PathVariable @Min(1) Long postId) {
        postService.delete(eventId, postId);
        return ResponseEntity.noContent().build();
    }

    // ---- TREE (read-only) ----
    @GetMapping("/tree")
    public EventTreeResponse getTree(@PathVariable @Min(1) Long eventId) {
        return treeService.getTree(eventId);
    }

    // ---- helpers ----
    private PostStatus parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return PostStatus.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status: " + s);
        }
    }

    private Audience parseAudience(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Audience.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown audience: " + s);
        }
    }

    private Channel parseChannel(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Channel.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown channel: " + s);
        }
    }
}
