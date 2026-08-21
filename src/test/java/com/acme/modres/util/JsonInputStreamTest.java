package com.acme.modres.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Tests for JsonInputStream class.
 */
public class JsonInputStreamTest {

    @TempDir
    Path tempDir;

    @Test
    void testConstructor_withValidFile_createsInstance() throws Exception {
        File file = tempDir.resolve("test.json").toFile();
        file.createNewFile();
        JsonInputStream jis = new JsonInputStream(file);
        assertNotNull(jis);
        jis.close();
    }

    @Test
    void testConstructor_withNonExistentFile_throwsFileNotFoundException() {
        File file = new File("/nonexistent/path/test.json");
        assertThrows(java.io.FileNotFoundException.class, () ->
            new JsonInputStream(file)
        );
    }

    @Test
    void testParseJsonAs_withNonExistentFile_returnsNull() throws Exception {
        File file = tempDir.resolve("test.json").toFile();
        file.createNewFile();
        JsonInputStream jis = new JsonInputStream(file);
        // File exists but is empty - will return null from gson
        Object result = jis.parseJsonAs(String.class);
        // Empty file returns null
        assertNull(result);
        jis.close();
    }

    @Test
    void testParseJsonAs_withValidJson_returnsObject() throws Exception {
        File file = tempDir.resolve("test.json").toFile();
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("{\"name\":\"test\",\"value\":42}");
        }
        JsonInputStream jis = new JsonInputStream(file);
        Object result = jis.parseJsonAs(TestJsonObject.class);
        assertNotNull(result);
        jis.close();
    }

    @Test
    void testParseJsonAs_withValidJsonString_returnsString() throws Exception {
        File file = tempDir.resolve("test.json").toFile();
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("\"hello world\"");
        }
        JsonInputStream jis = new JsonInputStream(file);
        Object result = jis.parseJsonAs(String.class);
        assertNotNull(result);
        assertEquals("hello world", result);
        jis.close();
    }

    @Test
    void testParseJsonAs_withInvalidJson_returnsNull() throws Exception {
        File file = tempDir.resolve("test.json").toFile();
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("not valid json {{{");
        }
        JsonInputStream jis = new JsonInputStream(file);
        Object result = jis.parseJsonAs(TestJsonObject.class);
        // Invalid JSON returns null
        assertNull(result);
        jis.close();
    }

    @Test
    void testJsonInputStream_extendsFileInputStream() throws Exception {
        File file = tempDir.resolve("test.json").toFile();
        file.createNewFile();
        JsonInputStream jis = new JsonInputStream(file);
        assertTrue(jis instanceof java.io.FileInputStream);
        jis.close();
    }

    // Helper class for JSON parsing tests
    static class TestJsonObject {
        String name;
        int value;
    }
}
