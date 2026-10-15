package com.acme.modres.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.gson.Gson;

/**
 * JsonInputStream wraps an InputStream and provides JSON parsing capability.
 *
 * Cloud-native change: Refactored from extending FileInputStream (which required
 * a local java.io.File) to wrapping a generic InputStream. This removes the
 * dependency on local file system paths and allows reading directly from
 * classpath resources or cloud storage streams.
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
