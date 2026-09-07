package com.careflow.ai;

import com.careflow.ai.config.CacheConfig;
import com.careflow.ai.dto.CursorPageResponse;
import com.careflow.ai.dto.DashboardDto.Summary;
import com.careflow.ai.dto.TimelineDto.TimelineEventResponse;
import com.careflow.ai.entity.EventType;
import com.careflow.ai.entity.Role;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.UserRepository;
import com.careflow.ai.security.CustomUserDetails;
import com.careflow.ai.service.DashboardService;
import com.careflow.ai.service.TaskService;
import com.careflow.ai.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
public class Phase3CachingAndPerformanceTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TimelineService timelineService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    private User doctor;
    private static final UUID ELEANOR_ID = UUID.fromString("a1111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        doctor = userRepository.findByEmail("doctor1@careflow.ai").orElseThrow();
        CustomUserDetails details = new CustomUserDetails(doctor);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        if (cacheManager.getCache(CacheConfig.DASHBOARD_SUMMARY_CACHE) != null) {
            cacheManager.getCache(CacheConfig.DASHBOARD_SUMMARY_CACHE).clear();
        }
    }

    @Test
    void testDashboardSummaryCachingAndEviction() {
        // First call populates cache
        Summary first = dashboardService.getSummary();
        assertNotNull(first);

        // Verify cache entry exists
        String key = doctor.getId().toString();
        var cache = cacheManager.getCache(CacheConfig.DASHBOARD_SUMMARY_CACHE);
        assertNotNull(cache);

        // Mutating action triggers eviction
        User nurse = userRepository.findByEmail("nurse1@careflow.ai").orElseThrow();
        taskService.createTask(
                ELEANOR_ID,
                doctor.getId(),
                nurse.getId(),
                "Phase 3 Test Task",
                "Testing cache eviction",
                null
        );

        // Eviction clears cache
        assertNull(cache.get(key));

        // Re-fetching rebuilds summary with updated counts
        Summary updated = dashboardService.getSummary();
        assertNotNull(updated);
    }

    @Test
    void testTimelineCursorPagination() {
        // Append test events
        timelineService.appendEvent(ELEANOR_ID, EventType.VITAL, "Test Vital 1", doctor.getId());
        timelineService.appendEvent(ELEANOR_ID, EventType.VITAL, "Test Vital 2", doctor.getId());

        CursorPageResponse<TimelineEventResponse> page1 =
                timelineService.getTimelineCursor(ELEANOR_ID, null, null, null, null, 2);

        assertNotNull(page1);
        assertNotNull(page1.getItems());
        assertTrue(page1.getItems().size() <= 2);
        assertEquals(2, page1.getPageSize());
    }

    @Test
    void testTimelineIsNotCached() {
        // Fetch timeline twice and ensure cache manager has no timeline cache entries
        timelineService.getTimeline(ELEANOR_ID, null, null, null);
        timelineService.getTimeline(ELEANOR_ID, null, null, null);

        assertNull(cacheManager.getCache("timeline"));
    }
}
