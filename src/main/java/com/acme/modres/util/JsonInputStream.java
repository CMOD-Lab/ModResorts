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
 * JsonInputStream supports parsing JSON from either a byte array (cloud-native, S3-backed)
 * or a File (legacy/local fallback).
 *
 * Updated to accept byte[] to eliminate java.io.File dependency for cloud storage
 * compatibility (supports IOUtils S3-based resource loading).
 */
public class JsonInputStream extends InputStream implements AutoCloseable {

  private final InputStream delegate;

  /**
   * Construct from a byte array (cloud-native path: data read from S3 or classpath).
   */
  public JsonInputStream(byte[] data) {
    this.delegate = new ByteArrayInputStream(data);
  }

  /**
   * Construct from a File (legacy/local fallback).
   */
  public JsonInputStream(File file) throws FileNotFoundException {
    this.delegate = new FileInputStream(file);
  }

  @Override
  public int read() throws IOException {
    return delegate.read();
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return delegate.read(b, off, len);
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }

  public Object parseJsonAs(Class<?> cls) {
    try {
      Gson gson = new Gson();
      BufferedReader reader = new BufferedReader(new InputStreamReader(delegate));
      return gson.fromJson(reader, cls);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

}
