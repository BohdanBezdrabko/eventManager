package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.dto.CreatorDto;
import com.example.sportadministrationsystem.dto.EventDto;
import com.example.sportadministrationsystem.dto.EventPayload;
import com.example.sportadministrationsystem.exception.NotFoundException;
import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.EventCategory;
import com.example.sportadministrationsystem.model.Tag;
import com.example.sportadministrationsystem.model.User;
import com.example.sportadministrationsystem.repository.EventRepository;
import com.example.sportadministrationsystem.repository.TagRepository;
import com.example.sportadministrationsystem.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock EventRepository eventRepository;
    @Mock TagRepository tagRepository;
    @Mock UserRepository userRepository;
    @Mock PostGenerationService postGenerationService; // якщо немає у проєкті — приберіть поле і verify нижче

    @InjectMocks EventService eventService;

    private User author;
    private Event baseEvent;

    @BeforeEach
    void setUp() {
        author = User.builder().id(101L).username("alex").password("x").build();

        baseEvent = new Event();
        baseEvent.setId(1L);
        baseEvent.setName("Summer Cup");
        baseEvent.setStartAt(LocalDateTime.of(2025, 7, 1, 10, 0));
        baseEvent.setLocation("Kyiv Arena");
        baseEvent.setCapacity(200);
        baseEvent.setDescription("Main summer tournament");
        baseEvent.setCoverUrl("https://img/c1.jpg");
        baseEvent.setCategory(EventCategory.SPORTS);
        baseEvent.setTags(new HashSet<>(Set.of(Tag.builder().id(1L).name("u18").build())));
        baseEvent.setCreatedBy(author);
    }

    private static void mockSecurityUsername(String username) {
        SecurityContext sc = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(username);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);
    }

    @Test
    void getById_returnsDtoWithCreatorAndTags() {
        when(eventRepository.findWithTagsById(1L)).thenReturn(Optional.of(baseEvent));

        EventDto dto = eventService.getById(1L);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("Summer Cup");
        assertThat(dto.category()).isEqualTo("SPORTS");
        assertThat(dto.tags()).containsExactly("u18");
        assertThat(dto.createdBy()).isEqualTo(101L);
        assertThat(dto.createdByUsername()).isEqualTo("alex");
    }

    @Test
    void getById_throws404WhenMissing() {
        when(eventRepository.findWithTagsById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> eventService.getById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event not found");
    }

    @Test
    void list_appliesCategoryAndTagFiltersInMemory() {
        Event e1 = cloneEvent(11L, "A", EventCategory.EDUCATION, Set.of("u16"));
        Event e2 = cloneEvent(12L, "B", EventCategory.SPORTS,    Set.of("u18","final"));
        Event e3 = cloneEvent(13L, "C", EventCategory.SPORTS,    Set.of("qual"));

        Page<Event> page = new PageImpl<>(List.of(e1, e2, e3), PageRequest.of(0, 20, Sort.by("startAt")), 3);
        when(eventRepository.findAll(any(Pageable.class))).thenReturn(page);

        // category=SPORTS + tag=u18
        Page<EventDto> out = eventService.list("sports", "U18", PageRequest.of(0,20));

        assertThat(out.getContent()).hasSize(1);
        EventDto only = out.getContent().get(0);
        assertThat(only.id()).isEqualTo(12L);
        assertThat(only.category()).isEqualTo("SPORTS");
        assertThat(only.tags()).contains("u18", "final");
    }

    @Test
    void findByName_and_findByLocation_areCaseInsensitive() {
        Event e1 = cloneEvent(21L, "Spring Open", EventCategory.MUSIC, Set.of("open"));
        Event e2 = cloneEvent(22L, "SPRING masters", EventCategory.MUSIC, Set.of("masters"));
        when(eventRepository.findAll()).thenReturn(List.of(e1, e2));

        List<EventDto> byName = eventService.findByName("spring");
        assertThat(byName).extracting(EventDto::id).containsExactlyInAnyOrder(21L, 22L);

        e1.setLocation("Lviv"); e2.setLocation("Kyiv");
        List<EventDto> byLoc = eventService.findByLocation("lviv");
        assertThat(byLoc).extracting(EventDto::id).containsExactly(21L);
    }

    @Test
    void getCreator_returnsEmptyCreatorWhenNull() {
        Event noCreator = cloneEvent(33L, "No author", EventCategory.SPORTS, Set.of());
        noCreator.setCreatedBy(null);
        when(eventRepository.findById(33L)).thenReturn(Optional.of(noCreator));

        CreatorDto c = eventService.getCreator(33L);
        assertThat(c.id()).isNull();
        assertThat(c.username()).isNull();
    }

    @Test
    void create_setsCurrentUserAsAuthor_parsesCategory_andCreatesMissingTags() {
        mockSecurityUsername("alex");
        when(userRepository.findByUsername("alex")).thenReturn(Optional.of(author));

        when(tagRepository.findByNameIgnoreCase("u18"))
                .thenReturn(Optional.of(Tag.builder().id(1L).name("u18").build()));
        when(tagRepository.findByNameIgnoreCase("final")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> {
            Tag t = inv.getArgument(0);
            return Tag.builder().id(777L).name(t.getName()).build();
        });

        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            e.setId(555L);
            return e;
        });

        EventPayload p = new EventPayload();
        p.setName("  New Cup  ");
        p.setStartAt(LocalDateTime.of(2025, 8, 10, 12, 0));
        p.setLocation("Kharkiv");
        p.setCapacity(120);
        p.setDescription("  desc  ");
        p.setCoverUrl("  http://img  ");
        p.setCategory("sports");     // має перетворитись у ENUM
        p.setTags(List.of("u18", "  final "));

        EventDto out = eventService.create(p);

        assertThat(out.createdBy()).isEqualTo(101L);
        assertThat(out.createdByUsername()).isEqualTo("alex");
        assertThat(out.category()).isEqualTo("SPORTS");
        assertThat(out.tags()).containsExactlyInAnyOrder("u18", "final");

        verify(postGenerationService, atMostOnce()).ensureEventScheduledPosts(any(Event.class));
    }

    @Test
    void create_throwsWhenCurrentUserNotResolved() {
        mockSecurityUsername("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        EventPayload p = new EventPayload();
        p.setName("n"); p.setStartAt(LocalDateTime.now());

        assertThatThrownBy(() -> eventService.create(p))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Current user not found");
    }

    @Test
    void update_overwritesFields_andNormalizesInvalidCategoryToNull() {
        Event existing = cloneEvent(1L, "Old", EventCategory.COMMUNITY, Set.of("old"));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(tagRepository.findByNameIgnoreCase("u16")).thenReturn(Optional.of(Tag.builder().id(5L).name("u16").build()));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventPayload p = new EventPayload();
        p.setName("  New Name ");
        p.setStartAt(LocalDateTime.of(2026, 1, 1, 9, 0));
        p.setLocation("  Odesa ");
        p.setCapacity(50);
        p.setDescription("  d  ");
        p.setCoverUrl("  c  ");
        p.setCategory("INVALID_ENUM"); // має впасти в null
        p.setTags(List.of("u16"));

        EventDto out = eventService.update(1L, p);

        assertThat(out.name()).isEqualTo("New Name");
        assertThat(out.location()).isEqualTo("Odesa");
        assertThat(out.capacity()).isEqualTo(50);
        assertThat(out.category()).isNull();
        assertThat(out.tags()).containsExactly("u16");

        verify(postGenerationService, atMostOnce()).ensureEventScheduledPosts(any(Event.class));
    }

    @Test
    void delete_removesEvent() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(baseEvent));
        eventService.delete(1L);
        verify(eventRepository).delete(baseEvent);
    }

    private static Event cloneEvent(Long id, String name, EventCategory cat, Set<String> tags) {
        Event e = new Event();
        e.setId(id);
        e.setName(name);
        e.setStartAt(LocalDateTime.of(2025, 1, 1, 10, 0));
        e.setLocation("loc");
        e.setCapacity(10);
        e.setDescription("d");
        e.setCoverUrl("c");
        e.setCategory(cat);
        Set<Tag> tagSet = new HashSet<>();
        for (String t : tags) tagSet.add(Tag.builder().id((long) t.hashCode()).name(t).build());
        e.setTags(tagSet);
        e.setCreatedBy(User.builder().id(101L).username("alex").password("x").build());
        return e;
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
}
