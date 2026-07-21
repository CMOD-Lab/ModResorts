package com.acme.modres.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.gson.Gson;

/**
 * JSON parsing utility that supports both File-based and byte-array-based input.
 * The byte-array constructor enables cloud-native usage without local temp files.
 */
public class JsonInputStream implements AutoCloseable {

  private InputStream inputStream;
  private File file; // kept for backward compatibility with File-based constructor

  /**
   * File-based constructor (legacy path — kept for backward compatibility).
   */
  public JsonInputStream(File file) throws FileNotFoundException {
    this.file = file;
    this.inputStream = new FileInputStream(file);
  }

  /**
   * Byte-array constructor — cloud-native path that avoids local temp file creation.
   * Used by IOUtils when reading classpath resources directly.
   */
  public JsonInputStream(byte[] data) {
    this.inputStream = new ByteArrayInputStream(data);
  }

  /**
   * Parses the underlying stream as the given class using Gson.
   */
  public Object parseJsonAs(Class<?> cls) {
    // For File-based path, verify file exists first
    if (file != null && !file.exists()) {
      return null;
    }
    try {
      Gson gson = new Gson();
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      return gson.fromJson(reader, cls);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public void close() throws IOException {
    if (inputStream != null) {
      inputStream.close();
    }
  }
}
