package com.jecstar.etm.server.core.converter;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for the <code>JsonEntityConverter</code> class.
 */
class JsonEntityConverterTest {

    @SuppressWarnings("unchecked")
    @Test
    void testStringConversion() {
        StringAnnotatedClass entity = new StringAnnotatedClass();
        entity.setFirstValue("First value");
        entity.setSecondValue("Second value");

        JsonEntityConverter converter = new JsonEntityConverter<>(f -> new StringAnnotatedClass());
        String json = converter.write(entity);
        assertEquals("{\"first_value\": \"First value\", \"second_value\": \"Second value\"}", json);
        StringAnnotatedClass entity2 = (StringAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testStringConversionWithNull() {
        StringAnnotatedClass entity = new StringAnnotatedClass();

        JsonEntityConverter converter = new JsonEntityConverter<>(f -> new StringAnnotatedClass());
        String json = converter.write(entity);
        assertEquals("{\"second_value\": null}", json);
        StringAnnotatedClass entity2 = (StringAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testLongConversion() {
        LongAnnotatedClass entity = new LongAnnotatedClass();
        entity.setFirstValue(101L);
        entity.setSecondValue(213L);
        entity.setThirdValue(456);

        JsonEntityConverter converter = new JsonEntityConverter<>(f -> new LongAnnotatedClass());
        String json = converter.write(entity);
        assertEquals("{\"first_value\": 101, \"second_value\": 213, \"third_value\": 456}", json);
        LongAnnotatedClass entity2 = (LongAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testLongConversionWithNull() {
        LongAnnotatedClass entity = new LongAnnotatedClass();

        JsonEntityConverter converter = new JsonEntityConverter<>(f -> new LongAnnotatedClass());
        String json = converter.write(entity);
        assertEquals("{\"second_value\": null, \"third_value\": 0}", json);
        LongAnnotatedClass entity2 = (LongAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testDoubleConversion() {
        DoubleAnnotatedClass entity = new DoubleAnnotatedClass();
        entity.setFirstValue(101.23);
        entity.setSecondValue(213.45);
        entity.setThirdValue(456.54);

        JsonEntityConverter converter = new JsonEntityConverter<>(f -> new DoubleAnnotatedClass());
        String json = converter.write(entity);
        assertEquals("{\"first_value\": 101.23, \"second_value\": 213.45, \"third_value\": 456.54}", json);
        DoubleAnnotatedClass entity2 = (DoubleAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testDoubleConversionWithNull() {
        DoubleAnnotatedClass entity = new DoubleAnnotatedClass();

        JsonEntityConverter converter = new JsonEntityConverter<>(f -> new DoubleAnnotatedClass());
        String json = converter.write(entity);
        assertEquals("{\"second_value\": null, \"third_value\": 0.0}", json);
        DoubleAnnotatedClass entity2 = (DoubleAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testIntegerConversion() {
        IntegerAnnotatedClass entity = new IntegerAnnotatedClass();
        entity.setFirstValue(101);
        entity.setSecondValue(213);
        entity.setThirdValue(456);

        JsonEntityConverter converter = new JsonEntityConverter<>(f -> new IntegerAnnotatedClass());
        String json = converter.write(entity);
        assertEquals("{\"first_value\": 101, \"second_value\": 213, \"third_value\": 456}", json);
        IntegerAnnotatedClass entity2 = (IntegerAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testIntegerConversionWithNull() {
        IntegerAnnotatedClass entity = new IntegerAnnotatedClass();

        JsonEntityConverter converter = new JsonEntityConverter<>(f -> new IntegerAnnotatedClass());
        String json = converter.write(entity);
        assertEquals("{\"second_value\": null, \"third_value\": 0}", json);
        IntegerAnnotatedClass entity2 = (IntegerAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testBooleanConversion() {
        BooleanAnnotatedClass entity = new BooleanAnnotatedClass();
        entity.setFirstValue(true);
        entity.setSecondValue(false);
        entity.setThirdValue(true);

        JsonEntityConverter converter = new JsonEntityConverter<>(f -> new BooleanAnnotatedClass());
        String json = converter.write(entity);
        assertEquals("{\"first_value\": true, \"second_value\": false, \"third_value\": true}", json);
        BooleanAnnotatedClass entity2 = (BooleanAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testBooleanConversionWithNull() {
        BooleanAnnotatedClass entity = new BooleanAnnotatedClass();

        JsonEntityConverter converter = new JsonEntityConverter<>(f -> new BooleanAnnotatedClass());
        String json = converter.write(entity);
        assertEquals("{\"second_value\": null, \"third_value\": false}", json);
        BooleanAnnotatedClass entity2 = (BooleanAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testInstantConversion() {
        InstantAnnotatedClass entity = new InstantAnnotatedClass();
        Instant instant1 = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Instant instant2 = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        entity.setFirstValue(instant1);
        entity.setSecondValue(instant2);

        JsonEntityConverter converter = new JsonEntityConverter<>(f -> new InstantAnnotatedClass());
        String json = converter.write(entity);
        assertEquals("{\"first_value\": " + instant1.toEpochMilli() + ", \"second_value\": " + instant2.toEpochMilli() + "}", json);
        InstantAnnotatedClass entity2 = (InstantAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testInstantConversionWithNull() {
        InstantAnnotatedClass entity = new InstantAnnotatedClass();

        JsonEntityConverter converter = new JsonEntityConverter<>(f -> new InstantAnnotatedClass());
        String json = converter.write(entity);
        assertEquals("{\"second_value\": null}", json);
        InstantAnnotatedClass entity2 = (InstantAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }
}
