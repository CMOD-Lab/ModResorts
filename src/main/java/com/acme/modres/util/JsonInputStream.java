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
 * JsonInputStream supports both File-based and byte[]-based JSON parsing.
 * The byte[]-based constructor is used for cloud-native (S3) workflows where
 * data is read from Amazon S3 as byte arrays rather than local files.
 */
public class JsonInputStream implements AutoCloseable {

  private final InputStream inputStream;
  private final File file;

  /**
   * Constructor for file-based JSON parsing (legacy support).
   */
  public JsonInputStream(File file) throws FileNotFoundException {
    this.file = file;
    this.inputStream = new FileInputStream(file);
  }

  /**
   * Constructor for in-memory byte array JSON parsing (cloud/S3 workflow).
   * Eliminates local file system dependency.
   */
  public JsonInputStream(byte[] content) {
    this.file = null;
    this.inputStream = new ByteArrayInputStream(content);
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
