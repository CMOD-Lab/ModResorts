package com.acme.modres.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.gson.Gson;

/**
 * Utility class for parsing JSON from an InputStream.
 * Refactored from FileInputStream-based implementation to support
 * cloud-native storage sources (Amazon S3, classpath) without
 * requiring local file system access.
 */
public class JsonInputStream implements Closeable {

  private final InputStream inputStream;

  public JsonInputStream(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  public Object parseJsonAs(Class<?> cls) {
    if (inputStream == null) {
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
