package com.connectforever;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;

public class PickArrowActivity extends Activity {
  public static final String TAG = "PickArrowActivity";
  
  ImageAdapter adapter;
  int currentItem;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.v(TAG, "onCreate");
    setContentView(R.layout.gallery);
    Gallery gallery = (Gallery) findViewById(R.id.pickarrow_gallery);
    adapter = new ImageAdapter(this);
    currentItem = (Integer) adapter.getItem(0);
    gallery.setAdapter(adapter);
  
    gallery.setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        currentItem = (Integer) adapter.getItem(position);
        ImageView iv = (ImageView) findViewById(R.id.pickarrow_detail);
        //iv.getDrawingCache().recycle();
        iv.setImageResource(currentItem);
      }
    });
    
    Button choose = (Button) findViewById(R.id.pickarrow_choose);
    choose.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        setResult(RESULT_OK, new Intent().putExtra("arrow", currentItem));
        finish();
      }
    });
    
    Button cancel = (Button) findViewById(R.id.pickarrow_cancel);
    cancel.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        setResult(RESULT_CANCELED);
        finish();
      }
    });
  }
  
  @Override
  public void onResume() {
    super.onResume();
    Log.v(TAG, "onResume");
  }
  
  
  public class ImageAdapter extends BaseAdapter {
    int mGalleryItemBackground;
    private Context mContext;

    private Integer[] mImageIds = {
      R.drawable.arrow_classic,
      R.drawable.arrow_classic_circle,
      R.drawable.arrow_classic_small,
      R.drawable.arrow_modern,
      R.drawable.arrow_modern_circle,
      R.drawable.arrow_modern_small
    };
  
    private ImageView[] views = new ImageView[mImageIds.length];

    public ImageAdapter(Context c) {
      mContext = c;
      TypedArray attr = mContext.obtainStyledAttributes(R.styleable.HelloGallery);
      mGalleryItemBackground = attr.getResourceId(
          R.styleable.HelloGallery_android_galleryItemBackground, 0);
      attr.recycle();
    }

    public int getCount() {
      return mImageIds.length;
    }

    public Object getItem(int position) {
      return mImageIds[position];
    }

    public long getItemId(int position) {
      return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      ImageView imageView;
      if (convertView == null) {
        if (views[position] == null) {
          imageView = new ImageView(mContext);
          views[position] = imageView;
        }
        else {
          return views[position];
        }
      }
      else {
        return (ImageView) convertView;
      }
      Display display = getWindowManager().getDefaultDisplay(); 
      int width = display.getWidth();
      
      Options opts = new Options();
      opts.inSampleSize = getSampleSize(width / 4, 800);
      Bitmap b = BitmapFactory.decodeResource(getResources(), mImageIds[position], opts);
      
      
      imageView.setLayoutParams(new Gallery.LayoutParams(width / 4, width / 4));
      imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
      
      imageView.setImageBitmap(b);
      //imageView.setImageResource(mImageIds[position]);
      imageView.setBackgroundResource(mGalleryItemBackground);

      return imageView;
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
  }
}
