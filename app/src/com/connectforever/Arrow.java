package com.connectforever;

import android.content.Context;
import android.util.AttributeSet;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.widget.ImageView;
import android.util.Log;
import java.lang.Runnable;

/**
 * ImageView that shows a rotated bitmap. The rotation can be set, once set the
 * image will slowly move to the given rotation value.
 * The bitmap can be changed by calling setArrowDrawable. The last selected bitmap
 * is saved persistently using sharedpreferences.
 */
public class Arrow extends ImageView implements Runnable {
  private static final String TAG = "Arrow";

  /** The interval with which the arrow is moved by one degree towards its goal. */
  private static final int DRAW_INTERVAL = 100;
  
  /** If the arrow is already within THRESHOLD degrees of the goal rotation, don't rotate. */
  private static final int THRESHOLD = 1;
  
  /** The last given rotation received by setRotation(). */
  private float goalRotation = 0;
  /** The current rotation of the bitmap. */
  private float bitmapRotation = 0;
  /** If the arrow is currently being updated. */
  private boolean running = true;
  
  /** The bitmap that is used to draw the arrow (can be changed by calling setArrowDrawable). */
  private Bitmap arrow;
  /** The width of the ImageView. */
  private int width = 0;
  /** The height of the ImageView. */
  private int height = 0;
  
  public Arrow(Context context) {
    super(context);
    init(context);
  }
  
  public Arrow(Context context, AttributeSet set) {
    super(context, set);
    init(context);
  }
  
  /**
   * Set a new bitmap for the arrow. This is saved persistently, and set immediately.
   * @param resourceId  The id of the chosen bitmap
   * @param context
   */
  public void setArrowDrawable(int resourceId, Context context) {
    SharedPrefs.getInstance().setArrowDrawable(resourceId);
    destroy();
    init(context);
  }
  
  @Override
  public void onDraw(Canvas canvas) {
    calculateRotation();
    // Calculate the scaling factor for the canvas
    float scale1 = Math.min(canvas.getWidth() / (float) arrow.getWidth(), 
        canvas.getHeight() / (float) arrow.getHeight());
    float scale2 = Math.min(canvas.getHeight() / (float) arrow.getWidth(),
        canvas.getWidth() / (float) arrow.getHeight());
    float scale = Math.min(scale1, scale2);
    // calculate the scaled width and height of the arrow
    float scaledWidth = (1 / scale) * canvas.getWidth();
    float scaledHeight = (1 / scale) * canvas.getHeight();
    // calculate the offset of the arrow in the canvas (top left)
    int startX = (int) (scaledWidth / 2 - arrow.getWidth() / 2);
    int startY = (int) (scaledHeight / 2 - arrow.getHeight() / 2);
    // draw the arrow
    canvas.save();
    canvas.rotate(bitmapRotation, width / 2, height / 2);
    canvas.scale(scale, scale);
    canvas.drawBitmap(arrow, startX, startY, null);
    canvas.restore();
  }
  
  /** Sets the new goal rotation. The arrow will rotate slowly to match this rotation value. */
  public void setRotation(float rotation) {
    goalRotation = rotation;
  }
  
  /** Sets the device's screen size. This is used to decide how to draw the arrow. */
  public void setScreenSize(int width, int height) {
    this.width = width;
    this.height = height;
  }
  
  /**
   * Removes the arrow drawable. Call in onDestroy() to prevent memory leaks.
   */
  public void destroy() {
    if (arrow != null) arrow.recycle();
    arrow = null;
  }
  
  /**
   * Stops the updating of the arrow. Start it again by making a new thread.
   */
  public void stopRunning() {
    running = false;
  }
  
  /**
   * Background thread that moves the arrow by one degree every DRAW_INTERVAL ms.
   */
  public void run() {
    while(running) {
      calculateRotation();
      postInvalidate();
      try {
        Thread.sleep(DRAW_INTERVAL);
      }
      catch (InterruptedException e) {
        Log.e(TAG, "Thread interrupted: " + e.getMessage());
      } 
    }
  }
  
  /** Initialises the arrow drawable. */
  private void init(Context context) {
    if (arrow == null) {
      int arrowId = SharedPrefs.getInstance().getArrowDrawable();
      if (arrowId == -1) arrowId = R.drawable.arrow_classic;
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inPreferredConfig = Bitmap.Config.RGB_565;
      arrow = BitmapFactory.decodeResource(getResources(), arrowId, options);
    }
  }
  
  int getSampleSize(int dstWidth, int orgWidth) {
    int sampleSize = 1;
    orgWidth /= 2;
    while (dstWidth < orgWidth) {
      sampleSize *= 2;
      orgWidth /= 2;
    }
    return sampleSize;
  }
  /**
   * Calculate the new rotation value.
   * Move one degree to the newly observed value, unless you're already close enough.
   */
  private void calculateRotation() {
    float diff = bitmapRotation - goalRotation;
    float norm_diff = normalize(diff);
    if (norm_diff < - THRESHOLD) {
      bitmapRotation += 1;
    }
    else if (norm_diff > THRESHOLD) {
      bitmapRotation -= 1;
    }
  }
  
  private float normalize(float diff) {
    while (diff < -180) {
      diff += 360;
    }
    while (diff > 180) {
      diff -= 360;
    }
    return diff;
  }
  
}
