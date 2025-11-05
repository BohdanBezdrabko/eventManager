package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.dto.PostDto;
import com.example.sportadministrationsystem.dto.PostPayload;
import com.example.sportadministrationsystem.exception.NotFoundException;
import com.example.sportadministrationsystem.model.*;
import com.example.sportadministrationsystem.repository.EventRepository;
import com.example.sportadministrationsystem.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock PostRepository postRepository;
    @Mock EventRepository eventRepository;
    @Mock PostDispatchService postDispatchService;

    @InjectMocks PostService postService;

    private Event event;

    @BeforeEach
    void init() {
        event = new Event();
        event.setId(100L);
        event.setName("Run");
        event.setLocation("Kyiv");
        event.setStartAt(LocalDateTime.now().plusDays(1));

        // Значення @Value("${telegram.bot.chat-id:}")
        ReflectionTestUtils.setField(postService, "defaultChatId", "123456789");
    }

    private Post newPostEntity(Long id) {
        Post p = Post.builder()
                .id(id)
                .event(event)
                .title("T")
                .body("B")
                .publishAt(LocalDateTime.now().plusHours(2))
                .status(PostStatus.DRAFT)
                .audience(Audience.PUBLIC)
                .channel(Channel.TELEGRAM)
                .externalId(null)
                .error(null)
                .generated(false)
                .telegramChatId("123456789")
                .build();
        return p;
    }

    /* ===================== CREATE ===================== */

    @Test
    @DisplayName("create(): успішне створення з payload без status -> статус DRAFT; чат береться з @Value, якщо у payload порожньо")
    void create_success_usesDefaults() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        ArgumentCaptor<Post> cap = ArgumentCaptor.forClass(Post.class);
        when(postRepository.save(cap.capture())).thenAnswer(inv -> {
            Post saved = cap.getValue();
            saved.setId(1L);
            return saved;
        });

        PostPayload payload = new PostPayload(
                "Title", "Body",
                LocalDateTime.now().plusHours(3),
                "PUBLIC", "TELEGRAM",
                null, // status not provided
                ""    // telegramChatId blank -> візьметься з @Value
        );

        PostDto dto = postService.create(100L, payload);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.status()).isEqualTo(PostStatus.DRAFT.name());
        assertThat(dto.audience()).isEqualTo(Audience.PUBLIC.name());
        assertThat(dto.channel()).isEqualTo(Channel.TELEGRAM.name());
        assertThat(dto.telegramChatId()).isEqualTo("123456789");

        Post saved = cap.getValue();
        assertThat(saved.isGenerated()).isFalse();
        assertThat(saved.getError()).isNull();
    }

    @Test
    @DisplayName("create(): якщо Event не знайдено — NotFoundException")
    void create_eventNotFound() {
        when(eventRepository.findById(100L)).thenReturn(Optional.empty());

        PostPayload payload = new PostPayload(
                "Title","Body", LocalDateTime.now().plusHours(1),
                "PUBLIC","TELEGRAM","DRAFT",null
        );

        assertThatThrownBy(() -> postService.create(100L, payload))
                .isInstanceOf(NotFoundException.class);
        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("create(): некоректні значення status/audience/channel -> IllegalArgumentException")
    void create_badEnums_throw() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));

        // bad status
        PostPayload p1 = new PostPayload("t","b", LocalDateTime.now().plusHours(1),
                "PUBLIC","TELEGRAM","BOOM",null);
        assertThatThrownBy(() -> postService.create(100L, p1))
                .isInstanceOf(IllegalArgumentException.class);

        // bad audience
        PostPayload p2 = new PostPayload("t","b", LocalDateTime.now().plusHours(1),
                "xxx","TELEGRAM",null,null);
        assertThatThrownBy(() -> postService.create(100L, p2))
                .isInstanceOf(IllegalArgumentException.class);

        // bad channel
        PostPayload p3 = new PostPayload("t","b", LocalDateTime.now().plusHours(1),
                "PUBLIC","slack",null,null);
        assertThatThrownBy(() -> postService.create(100L, p3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /* ===================== UPDATE ===================== */

    @Test
    @DisplayName("update(): часткове оновлення полів + очищення telegramChatId, якщо передано порожнє")
    void update_partialAndBlankChatClears() {
        Post existing = newPostEntity(5L);
        existing.setStatus(PostStatus.DRAFT);
        when(postRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PostPayload payload = new PostPayload(
                "NewT","NewB", LocalDateTime.now().plusDays(1),
                "SUBSCRIBERS","TELEGRAM", "SCHEDULED",
                " " // blank -> має стати null
        );

        PostDto dto = postService.update(100L, 5L, payload);

        assertThat(dto.title()).isEqualTo("NewT");
        assertThat(dto.body()).isEqualTo("NewB");
        assertThat(dto.status()).isEqualTo(PostStatus.SCHEDULED.name());
        assertThat(dto.audience()).isEqualTo(Audience.SUBSCRIBERS.name());
        assertThat(dto.telegramChatId()).isNull();
    }

    @Test
    @DisplayName("update(): якщо пост не належить цьому event — NotFoundException")
    void update_wrongEvent_throws() {
        Post existing = newPostEntity(5L);
        existing.setEvent(new Event()); // інший івент або null id
        existing.getEvent().setId(999L);

        when(postRepository.findById(5L)).thenReturn(Optional.of(existing));

        PostPayload payload = new PostPayload("t","b", LocalDateTime.now().plusHours(1),
                "PUBLIC","TELEGRAM",null,null);

        assertThatThrownBy(() -> postService.update(100L, 5L, payload))
                .isInstanceOf(NotFoundException.class);
    }

    /* ===================== DELETE ===================== */

    @Test
    @DisplayName("delete(): успішне видалення після перевірки належності до event")
    void delete_ok() {
        Post existing = newPostEntity(7L);
        when(postRepository.findById(7L)).thenReturn(Optional.of(existing));

        postService.delete(100L, 7L);

        verify(postRepository).delete(existing);
    }

    /* ===================== CHANGE STATUS ===================== */

    @Test
    @DisplayName("changeStatus(): дозволені переходи проходять; заборонені кидають IllegalStateException")
    void changeStatus_transitions() {
        Post existing = newPostEntity(10L);
        existing.setStatus(PostStatus.DRAFT);
        when(postRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // дозволений: DRAFT -> SCHEDULED
        PostDto dto = postService.changeStatus(100L, 10L, "SCHEDULED", null);
        assertThat(dto.status()).isEqualTo(PostStatus.SCHEDULED.name());

        // заборонений: PUBLISHED -> SCHEDULED (спершу виставимо PUBLISHED)
        existing.setStatus(PostStatus.PUBLISHED);
        when(postRepository.findById(10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> postService.changeStatus(100L, 10L, "SCHEDULED", null))
                .isInstanceOf(IllegalStateException.class);
    }

    /* ===================== GET / LIST ===================== */

    @Test
    @DisplayName("get(): повертає DTO; якщо інший event — NotFoundException")
    void get_checksEvent() {
        Post existing = newPostEntity(11L);
        when(postRepository.findById(11L)).thenReturn(Optional.of(existing));

        PostDto ok = postService.get(100L, 11L);
        assertThat(ok.id()).isEqualTo(11L);

        existing.getEvent().setId(999L);
        when(postRepository.findById(11L)).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> postService.get(100L, 11L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("list(): делегує фільтри у repository.findByEventIdAndFilters")
    void list_delegatesFilters() {
        Post p1 = newPostEntity(1L);
        Post p2 = newPostEntity(2L);
        when(postRepository.findByEventIdAndFilters(
                eq(100L), any(), any(), any())
        ).thenReturn(List.of(p1, p2));

        List<PostDto> out = postService.list(100L, "DRAFT", "PUBLIC", "TELEGRAM");

        assertThat(out).hasSize(2);
        verify(postRepository).findByEventIdAndFilters(eq(100L),
                eq(PostStatus.DRAFT), eq(Audience.PUBLIC), eq(Channel.TELEGRAM));
    }

    /* ===================== PUBLISH NOW ===================== */

    @Test
    @DisplayName("publishNow(): викликає PostDispatchService.dispatch і зберігає пост")
    void publishNow_callsDispatch() {
        Post existing = newPostEntity(50L);
        when(postRepository.findById(50L)).thenReturn(Optional.of(existing));
        when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PostDto dto = postService.publishNow(100L, 50L);

        verify(postDispatchService).dispatch(existing);
        assertThat(dto.id()).isEqualTo(50L);
    }

    /* ===================== DISPATCH DUE ===================== */

    @Test
    @DisplayName("dispatchDue(): бере SCHEDULED з publishAt<=now, намагається всіх відправити, зберігає, повертає processed count")
    void dispatchDue_processesAll() {
        Post p1 = newPostEntity(1L); p1.setStatus(PostStatus.SCHEDULED);
        Post p2 = newPostEntity(2L); p2.setStatus(PostStatus.SCHEDULED);
        when(postRepository.findPostsToDispatch(eq(PostStatus.SCHEDULED), any(LocalDateTime.class)))
                .thenReturn(List.of(p1, p2));

        // перший ок, другий впаде — сервіс має проковтнути (PostDispatchService сам виставить FAILED)
        doNothing().when(postDispatchService).dispatch(p1);
        doThrow(new RuntimeException("boom")).when(postDispatchService).dispatch(p2);

        int processed = postService.dispatchDue();

        assertThat(processed).isEqualTo(1);
        verify(postRepository).saveAll(List.of(p1, p2));
    }
}
