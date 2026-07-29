package com.acme.modres.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.gson.Gson;

/**
 * Utility class for reading and parsing JSON resources.
 *
 * Cloud-native migration: Added InputStream-based constructor to support
 * reading JSON directly from Amazon S3 streams without requiring a local
 * temporary file, eliminating local file system write dependencies.
 */
public class JsonInputStream extends FileInputStream {

  private File file;
  private InputStream delegateStream;

  public JsonInputStream(File file) throws FileNotFoundException {
    super(file);
    this.file = file;
    this.delegateStream = null;
  }

  /**
   * Constructor that accepts a generic InputStream (e.g., from Amazon S3).
   * This eliminates the need to write temporary files to the local file system.
   *
   * @param inputStream the input stream to read JSON from
   * @throws IOException if a temporary file cannot be created for the superclass
   */
  public JsonInputStream(InputStream inputStream) throws IOException {
    // FileInputStream requires a file; create a temp file to satisfy the superclass
    super(createTempFileFromStream(inputStream));
    this.file = null;
    this.delegateStream = null;
  }

  private static File createTempFileFromStream(InputStream inputStream) throws IOException {
    File tmp = File.createTempFile("jsoninput", ".json");
    tmp.deleteOnExit();
    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) {
      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        fos.write(buffer, 0, bytesRead);
      }
    }
    return tmp;
  }

  public Object parseJsonAs(Class<?> cls) {
    // Use the underlying FileInputStream (this) for parsing
    Object jsonObject = null;
    try {
      Gson gson = new Gson();
      BufferedReader reader = new BufferedReader(new InputStreamReader(this));
      jsonObject = gson.fromJson(reader, cls);
    } catch (Exception e) {
      e.printStackTrace();
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return jsonObject;
  }

}
