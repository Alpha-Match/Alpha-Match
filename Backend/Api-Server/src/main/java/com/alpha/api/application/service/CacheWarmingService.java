package com.alpha.api.application.service;

import com.alpha.api.presentation.graphql.type.UserMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Cache Warming Service (Application Layer)
 * - Preloads static data into cache on application startup
 * - Listens to ApplicationReadyEvent
 * - Targets: skillCategories, dashboardData (CANDIDATE/RECRUITER)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheWarmingService {

    private final CacheService cacheService;
    private final SearchService searchService;
    private final DashboardService dashboardService;

    /**
     * Warm cache on application startup
     * - Executed after ApplicationContext is fully initialized
     * - Preloads skillCategories and dashboardData
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmCacheOnStartup() {
        log.info("=== Cache Warming Started ===");

        // Warm skillCategories
        warmSkillCategories()
                .then(warmDashboardData(UserMode.CANDIDATE))
                .then(warmDashboardData(UserMode.RECRUITER))
                .doOnSuccess(v -> log.info("=== Cache Warming Completed Successfully ==="))
                .doOnError(error -> log.error("=== Cache Warming Failed ===", error))
                .subscribe(); // Subscribe to trigger execution
    }

    /**
     * Warm skillCategories cache
     * - Key: "skill:categories"
     * - TTL: 24 hours
     */
    private Mono<Void> warmSkillCategories() {
        log.info("Warming cache: skillCategories");
        String key = CacheService.skillCategoriesKey();

        return searchService.getSkillCategories()
                .flatMap(categories -> cacheService.warmCache(key, categories))
                .doOnSuccess(result -> log.info("skillCategories warmed: {} categories", key))
                .doOnError(error -> log.error("Failed to warm skillCategories: {}", error.getMessage()))
                .then();
    }

    /**
     * Warm dashboardData cache for given UserMode
     * - Key: "dashboard:{CANDIDATE|RECRUITER}"
     * - TTL: 24 hours
     *
     * @param userMode User mode
     */
    private Mono<Void> warmDashboardData(UserMode userMode) {
        log.info("Warming cache: dashboardData for {}", userMode);
        String key = CacheService.dashboardKey(userMode.name());

        return dashboardService.getDashboardData(userMode)
                .flatMap(data -> cacheService.warmCache(key, data))
                .doOnSuccess(result -> log.info("dashboardData warmed for {}: {}", userMode, key))
                .doOnError(error -> log.error("Failed to warm dashboardData for {}: {}", userMode, error.getMessage()))
                .then();
    }
}
