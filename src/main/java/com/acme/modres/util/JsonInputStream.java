package com.acme.modres.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.gson.Gson;

/**
 * JsonInputStream - supports both File-based and InputStream-based JSON parsing.
 * The InputStream-based constructor enables cloud-native usage without local temp files.
 */
public class JsonInputStream implements Closeable {

  private InputStream inputStream;

  /**
   * Construct from a File (legacy support).
   */
  public JsonInputStream(File file) throws FileNotFoundException {
    this.inputStream = new FileInputStream(file);
  }

  /**
   * Construct from an InputStream (cloud-native: no local file required).
   */
  public JsonInputStream(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  public Object parseJsonAs(Class<?> cls) {
    Object jsonObject = null;
    try {
      Gson gson = new Gson();
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      jsonObject = gson.fromJson(reader, cls);
    } catch (Exception e) {
      e.printStackTrace();
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return jsonObject;
  }

  @Override
  public void close() throws IOException {
    if (inputStream != null) {
      inputStream.close();
    }
  }

}
