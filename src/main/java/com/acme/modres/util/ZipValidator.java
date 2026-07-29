package com.acme.modres.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ZipValidator extends ZipFile {

  public ZipValidator(File file) throws ZipException, IOException {
    super(file);
    this.file = file;
    this.zipBytes = null;
  }

  /**
   * Constructor that accepts raw zip bytes for cloud-native (S3-based) validation
   * without requiring a local file system.
   */
  public ZipValidator(byte[] zipBytes) throws ZipException, IOException {
    // Delegate to a temporary in-memory check; we still need a valid ZipFile
    // for the superclass, so we use a minimal workaround by calling the
    // File-based constructor with a temp file created from the bytes.
    super(createTempFileFromBytes(zipBytes));
    this.file = null;
    this.zipBytes = zipBytes;
  }

  private static File createTempFileFromBytes(byte[] bytes) throws IOException {
    File tmp = File.createTempFile("zipvalidator", ".zip");
    tmp.deleteOnExit();
    java.nio.file.Files.write(tmp.toPath(), bytes);
    return tmp;
  }

  private File file;
  private byte[] zipBytes;

  public boolean isValid() throws Throwable {
    if (zipBytes != null) {
      // Validate from in-memory bytes (cloud-native path)
      try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
        ZipEntry entry = zis.getNextEntry();
        return entry != null;
      } catch (ZipException e) {
        return false;
      }
    }
    if (file != null && file.exists()) {
      try (ZipValidator zipFile = new ZipValidator(file)) {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        return entries.hasMoreElements();
      }
    }
    return false;
  }

}
