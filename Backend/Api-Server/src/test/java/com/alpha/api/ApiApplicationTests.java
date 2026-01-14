package com.alpha.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ApiApplication Unit Test
 * - Basic sanity test that doesn't require Spring context
 * - Avoids database connection issues
 *
 * NOTE: For full integration testing with Spring context,
 * use @SpringBootTest with proper test database configuration
 * or Testcontainers.
 */
@ExtendWith(MockitoExtension.class)
class ApiApplicationTests {

    @Test
    void applicationClassExists() {
        // Verify that the main application class exists
        assertTrue(ApiApplication.class.isAnnotationPresent(
                org.springframework.boot.autoconfigure.SpringBootApplication.class));
    }

    @Test
    void mainMethodExists() throws NoSuchMethodException {
        // Verify that main method exists
        ApiApplication.class.getMethod("main", String[].class);
    }
}
