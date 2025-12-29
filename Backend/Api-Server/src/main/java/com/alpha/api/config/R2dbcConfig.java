package com.alpha.api.config;

import io.r2dbc.postgresql.codec.Vector;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * R2DBC Configuration for Reactive Database Access
 * - PostgreSQL + pgvector support
 * - Custom converters for Vector type
 */
@Configuration
public class R2dbcConfig extends AbstractR2dbcConfiguration {

    private final ConnectionFactory connectionFactory;

    public R2dbcConfig(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public ConnectionFactory connectionFactory() {
        return connectionFactory;
    }

    @Bean
    @Override
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new VectorToListConverter());
        converters.add(new StringToListConverter());
        converters.add(new ListToStringConverter());
        return new R2dbcCustomConversions(getStoreConversions(), converters);
    }

    /**
     * Converts R2DBC PostgreSQL Vector to List<Float> for reading from DB
     * R2DBC PostgreSQL returns Vector objects for vector columns
     */
    @ReadingConverter
    public static class VectorToListConverter implements Converter<Vector, List<Float>> {
        @Override
        public List<Float> convert(Vector source) {
            if (source == null) {
                return null;
            }
            // Vector.getVector() returns float[]
            float[] vector = source.getVector();
            List<Float> result = new ArrayList<>(vector.length);
            for (float v : vector) {
                result.add(v);
            }
            return result;
        }
    }

    /**
     * Converts pgvector String representation to List<Float> for reading from DB
     * Example: "[0.1, 0.2, 0.3]" -> List<Float>
     */
    @ReadingConverter
    public static class StringToListConverter implements Converter<String, List<Float>> {
        @Override
        public List<Float> convert(String source) {
            if (source == null || source.isEmpty()) {
                return null;
            }
            // Remove brackets and split by comma
            String cleaned = source.replace("[", "").replace("]", "").trim();
            if (cleaned.isEmpty()) {
                return new ArrayList<>();
            }
            return Arrays.stream(cleaned.split(","))
                    .map(String::trim)
                    .map(Float::parseFloat)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Converts List<Float> to pgvector String representation for writing to DB
     * Example: List<Float> -> "[0.1,0.2,0.3]"
     */
    @WritingConverter
    public static class ListToStringConverter implements Converter<List<Float>, String> {
        @Override
        public String convert(List<Float> source) {
            if (source == null || source.isEmpty()) {
                return null;
            }
            return "[" + source.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")) + "]";
        }
    }
}
