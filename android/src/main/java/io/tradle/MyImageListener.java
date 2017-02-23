package io.tradle;

import android.os.Parcel;

import com.microblink.image.Image;
import com.microblink.image.ImageListener;

public class MyImageListener implements ImageListener {


  /**
   * Called when library has image available.
   */
  @Override
  public void onImageAvailable(Image image) {
    RNBlinkIDImageManager.put(image.getImageName(), image.convertToBitmap());
  }

  /**
   * ImageListener interface extends Parcelable interface, so we also need to implement
   * that interface. The implementation of Parcelable interface is below this line.
   */

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
  }

  public static final Creator<MyImageListener> CREATOR = new Creator<MyImageListener>() {
    @Override
    public MyImageListener createFromParcel(Parcel source) {
      return new MyImageListener();
    }

    @Override
    public MyImageListener[] newArray(int size) {
      return new MyImageListener[size];
    }
  };
}
