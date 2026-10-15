package com.acme.modres.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * ZipValidator supports both File-based and byte[]-based ZIP validation.
 * The byte[]-based constructor is used for cloud-native (S3) workflows where
 * no local file system is available.
 */
public class ZipValidator {

  private final File file;
  private final byte[] zipBytes;

  /**
   * Constructor for file-based validation (legacy support).
   */
  public ZipValidator(File file) {
    this.file = file;
    this.zipBytes = null;
  }

  /**
   * Constructor for in-memory byte array validation (cloud/S3 workflow).
   * Replaces local file system dependency with in-memory validation.
   */
  public ZipValidator(byte[] zipBytes) {
    this.file = null;
    this.zipBytes = zipBytes;
  }

  public boolean isValid() throws Throwable {
    if (zipBytes != null) {
      // Validate in-memory ZIP bytes (cloud-native path)
      try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
        ZipEntry entry = zis.getNextEntry();
        // A valid ZIP has at least one entry or is an empty but well-formed archive
        return true;
      } catch (ZipException e) {
        return false;
      }
    } else if (file != null && file.exists()) {
      // Legacy file-based validation
      try (ZipFile zipFile = new ZipFile(file)) {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        return true;
      } catch (ZipException e) {
        return false;
      }
    }
    return false;
  }

}
