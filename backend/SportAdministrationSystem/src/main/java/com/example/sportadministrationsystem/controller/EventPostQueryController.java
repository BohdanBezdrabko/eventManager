package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.dto.EventTreeResponse;
import com.example.sportadministrationsystem.dto.PostShortDto;
import com.example.sportadministrationsystem.model.Audience;
import com.example.sportadministrationsystem.model.Channel;
import com.example.sportadministrationsystem.model.PostStatus;
import com.example.sportadministrationsystem.service.EventTreeService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events/{eventId}")
@RequiredArgsConstructor
public class EventPostQueryController {

    private final EventTreeService treeService;

    @GetMapping("/posts")
    public List<PostShortDto> listPosts(
            @PathVariable @Min(1) Long eventId,
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) Audience audience,
            @RequestParam(required = false) Channel channel
    ) {
        return treeService.listPosts(eventId, status, audience, channel);
    }

    @GetMapping("/tree")
    public EventTreeResponse getTree(@PathVariable @Min(1) Long eventId) {
        return treeService.getTree(eventId);
    }
}
