package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.dto.EventTreeResponse;
import com.example.sportadministrationsystem.dto.PostShortDto;
import com.example.sportadministrationsystem.model.*;
import com.example.sportadministrationsystem.repository.EventRepository;
import com.example.sportadministrationsystem.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventTreeServiceTest {

    @Mock EventRepository eventRepository;
    @Mock PostRepository postRepository;

    @InjectMocks EventTreeService eventTreeService;

    @Test
    void listPosts_delegatesFiltersAndMapsToShortDto() {
        Long eventId = 7L;

        Post p1 = new Post(); p1.setId(1L); p1.setTitle("A"); p1.setPublishAt(nowPlus(1)); p1.setStatus(PostStatus.DRAFT);     p1.setAudience(Audience.PUBLIC);       p1.setChannel(Channel.TELEGRAM); p1.setGenerated(false);
        Post p2 = new Post(); p2.setId(2L); p2.setTitle("B"); p2.setPublishAt(nowPlus(2)); p2.setStatus(PostStatus.SCHEDULED); p2.setAudience(Audience.SUBSCRIBERS); p2.setChannel(Channel.INTERNAL); p2.setGenerated(true);

        when(postRepository.findByEventIdAndFilters(eventId, PostStatus.SCHEDULED, Audience.SUBSCRIBERS, Channel.INTERNAL))
                .thenReturn(List.of(p2));

        List<PostShortDto> out = eventTreeService.listPosts(eventId, PostStatus.SCHEDULED, Audience.SUBSCRIBERS, Channel.INTERNAL);

        assertThat(out).hasSize(1);
        PostShortDto dto = out.get(0);
        assertThat(dto.getId()).isEqualTo(2L);
        assertThat(dto.getStatus()).isEqualTo(PostStatus.SCHEDULED);
        assertThat(dto.getAudience()).isEqualTo(Audience.SUBSCRIBERS);
        assertThat(dto.getChannel()).isEqualTo(Channel.INTERNAL);
        assertThat(dto.isGenerated()).isTrue();
    }

    @Test
    void getTree_buildsSummaryAndPosts() {
        Long eventId = 9L;

        Event e = new Event();
        e.setId(eventId);
        e.setName("Winter Cup");
        e.setStartAt(LocalDateTime.of(2025, 12, 10, 18, 0));
        e.setLocation("Arena");
        e.setCategory(EventCategory.MUSIC);
        e.setTags(new HashSet<>(Set.of(
                Tag.builder().id(1L).name("u14").build(),
                Tag.builder().id(2L).name("playoff").build()
        )));

        when(eventRepository.findWithTagsById(eventId)).thenReturn(Optional.of(e));

        Post p = new Post();
        p.setId(5L);
        p.setTitle("Teaser");
        p.setPublishAt(nowPlus(1));
        p.setStatus(PostStatus.SCHEDULED);
        p.setAudience(Audience.PUBLIC);
        p.setChannel(Channel.TELEGRAM);
        p.setGenerated(true);

        when(postRepository.findByEventIdAndFilters(eventId, null, null, null))
                .thenReturn(List.of(p));

        EventTreeResponse tree = eventTreeService.getTree(eventId);

        assertThat(tree.getEvent().getId()).isEqualTo(eventId);
        assertThat(tree.getEvent().getName()).isEqualTo("Winter Cup");
        assertThat(tree.getEvent().getCategory()).isEqualTo("MUSIC");
        assertThat(tree.getEvent().getTags()).containsExactlyInAnyOrder("u14", "playoff");

        assertThat(tree.getPosts()).hasSize(1);
        assertThat(tree.getPosts().get(0).getTitle()).isEqualTo("Teaser");
    }

    @Test
    void getTree_404WhenEventMissing() {
        when(eventRepository.findWithTagsById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> eventTreeService.getTree(404L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    private static LocalDateTime nowPlus(int hours) {
        return LocalDateTime.now().plusHours(hours);
    }
}
