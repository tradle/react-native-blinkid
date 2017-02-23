package io.tradle;

import android.graphics.Bitmap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RNBlinkIDImageManager {

  private static Map<String, Bitmap> images = new HashMap<>();

  /**
   * Add an image to current operation
   */
  protected static void put (String name, Bitmap image) {
    images.put(name, image);
  }

  /**
   * return saved images
   */
  protected static Map<String, Bitmap> get() {
    return Collections.unmodifiableMap(images);
  }

  protected static void clear () {
    images.clear();
  }
}
