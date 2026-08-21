package com.acme.modres.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Tests for ZipValidator class.
 */
public class ZipValidatorTest {

    @TempDir
    Path tempDir;

    private File createValidZipFile(String filename) throws IOException {
        File zipFile = tempDir.resolve(filename).toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            ZipEntry entry = new ZipEntry("test.txt");
            zos.putNextEntry(entry);
            zos.write("test content".getBytes());
            zos.closeEntry();
        }
        return zipFile;
    }

    private File createEmptyZipFile(String filename) throws IOException {
        File zipFile = tempDir.resolve(filename).toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // empty zip
        }
        return zipFile;
    }

    @Test
    void testConstructor_withValidZipFile_createsInstance() throws Exception {
        File zipFile = createValidZipFile("test.zip");
        ZipValidator validator = new ZipValidator(zipFile);
        assertNotNull(validator);
        validator.close();
    }

    @Test
    void testConstructor_withNonZipFile_throwsZipException() throws Exception {
        File file = tempDir.resolve("notazip.zip").toFile();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write("not a zip file".getBytes());
        }
        assertThrows(java.util.zip.ZipException.class, () ->
            new ZipValidator(file)
        );
    }

    @Test
    void testIsValid_withEmptyZip_returnsTrue() throws Exception {
        File zipFile = createEmptyZipFile("empty.zip");
        ZipValidator validator = new ZipValidator(zipFile);
        try {
            boolean result = validator.isValid();
            assertTrue(result);
        } catch (Throwable t) {
            fail("Unexpected throwable: " + t.getMessage());
        } finally {
            validator.close();
        }
    }

    @Test
    void testIsValid_withNonExistentFile_returnsFalse() throws Exception {
        // Create a valid zip first to get a ZipValidator instance
        File zipFile = createEmptyZipFile("test.zip");
        ZipValidator validator = new ZipValidator(zipFile);
        validator.close();
        // Delete the file
        zipFile.delete();
        // Now isValid should return false since file doesn't exist
        try {
            boolean result = validator.isValid();
            assertFalse(result);
        } catch (Throwable t) {
            fail("Unexpected throwable: " + t.getMessage());
        }
    }

    @Test
    void testZipValidator_extendsZipFile() throws Exception {
        File zipFile = createEmptyZipFile("test.zip");
        ZipValidator validator = new ZipValidator(zipFile);
        assertTrue(validator instanceof java.util.zip.ZipFile);
        validator.close();
    }

    @Test
    void testIsValid_withZipContainingEntries_returnsFalse() throws Exception {
        File zipFile = createValidZipFile("withentries.zip");
        ZipValidator validator = new ZipValidator(zipFile);
        try {
            // The isValid() method creates a new ZipValidator internally and checks entries
            // If entries exist, it doesn't return true (only returns true if no elements)
            boolean result = validator.isValid();
            assertFalse(result);
        } catch (Throwable t) {
            fail("Unexpected throwable: " + t.getMessage());
        } finally {
            validator.close();
        }
    }
}
