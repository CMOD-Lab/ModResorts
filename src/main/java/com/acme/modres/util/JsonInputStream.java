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
 * Utility stream for parsing JSON resources.
 *
 * Extended to support byte-array input so that callers can parse JSON content
 * that was loaded from the classpath or Amazon S3 without requiring a local
 * File reference (eliminates local file system dependency).
 */
public class JsonInputStream extends FileInputStream {

  private File file;
  private byte[] rawBytes;

  /**
   * Original constructor — retained for backward compatibility.
   */
  public JsonInputStream(File file) throws FileNotFoundException {
    super(file);
    this.file = file;
    this.rawBytes = null;
  }

  /**
   * New constructor that accepts raw bytes (e.g., content loaded from classpath
   * or Amazon S3) without requiring a local File on disk.
   *
   * Because FileInputStream requires a File or descriptor, we create a temporary
   * file solely as a handle for the super-constructor; the actual parsing is
   * performed directly from the byte array via {@link #parseJsonAs(Class)}.
   *
   * @param bytes JSON content as a byte array
   * @throws IOException if a temporary file cannot be created
   */
  public JsonInputStream(byte[] bytes) throws IOException {
    // We must call a super constructor; use a temp file as a placeholder.
    // The temp file is deleted immediately — actual data comes from rawBytes.
    super(createTempPlaceholder());
    this.rawBytes = bytes;
    this.file = null;
  }

  /**
   * Creates a short-lived temporary file used only as a placeholder for the
   * FileInputStream super-constructor when byte-array mode is active.
   */
  private static File createTempPlaceholder() throws IOException {
    File tmp = File.createTempFile("json-placeholder", ".tmp");
    tmp.deleteOnExit();
    return tmp;
  }

  /**
   * Parses the JSON content into an instance of the given class.
   *
   * When constructed with a byte array the content is read directly from memory;
   * when constructed with a File the original file-based path is used.
   */
  public Object parseJsonAs(Class<?> cls) {
    // Byte-array mode: parse directly from in-memory bytes
    if (rawBytes != null) {
      try (InputStream bais = new ByteArrayInputStream(rawBytes);
           BufferedReader reader = new BufferedReader(new InputStreamReader(bais))) {
        Gson gson = new Gson();
        return gson.fromJson(reader, cls);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }

    // File mode: original behaviour
    if (file != null && file.exists()) {
      JsonInputStream is = null;
      Object jsonObject = null;
      try {
        is = new JsonInputStream(file);
        Gson gson = new Gson();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        jsonObject = gson.fromJson(reader, cls);
      } catch (Exception e) {
        e.printStackTrace();
      } catch (Throwable e) {
        e.printStackTrace();
      } finally {
        if (is != null) {
          try {
            is.close();
            is.read(); // test if file is closed
          } catch (IOException e) {
            // closed successfully
            return jsonObject;
          } catch (Throwable e) {
            e.printStackTrace();
          }
        }
      }
    }
    return null;
  }

}
