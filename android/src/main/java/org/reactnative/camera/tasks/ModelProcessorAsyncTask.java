package org.reactnative.camera.tasks;

import android.graphics.Bitmap;
import android.os.SystemClock;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;

import android.util.Log;
import com.indigoviolet.posedecoding.Decoder;
import com.indigoviolet.posedecoding.DecoderParams;
import com.indigoviolet.posedecoding.PosenetDecoder;
import com.indigoviolet.posedecoding.CPMHourglassDecoder;
import com.indigoviolet.posedecoding.TfliteModel;

public class ModelProcessorAsyncTask extends android.os.AsyncTask<Void, Void, List<Map<String, Object>>> {

  private ModelProcessorAsyncTaskDelegate mDelegate;
  private String mModelType;
  private TfliteModel mModel;
  private DecoderParams mDecoderParams;
  private int mModelMaxFreqms;
  private Bitmap mBitmap;
  private int mWidth;
  private int mHeight;
  private int mDeviceRotation;
  private int mCameraOrientation;
  private int mRotation;
  private Map<String, Long> mTiming;

  public ModelProcessorAsyncTask(
      ModelProcessorAsyncTaskDelegate delegate,
      String modelType,
      TfliteModel model,
      DecoderParams decoderParams,
      int modelMaxFreqms,
      Bitmap bitmap,
      int width,
      int height,
      int rotation,
      int cameraOrientation,
      int deviceRotation,
      Map<String, Long> timing
  ) {
    mDelegate = delegate;
    mModelType = modelType;
    mModel = model;
    mDecoderParams = decoderParams;
    mModelMaxFreqms = modelMaxFreqms;
    mBitmap = bitmap;
    mWidth = width;
    mHeight = height;
    mRotation = rotation;
    mCameraOrientation = cameraOrientation;
    mDeviceRotation = deviceRotation;
    mTiming = timing;
    // Log.i("ReactNative", String.format("mWidth=%d, mHeight=%d, mDeviceRotation=%d", mWidth, mHeight, mDeviceRotation));
  }

  @Override
  protected List<Map<String, Object>> doInBackground(Void... ignored) {
    if (isCancelled() || mDelegate == null) {
      return null;
    }
    long startTime = SystemClock.elapsedRealtime();

    Decoder decoder;

    mTiming.put("decoderBeginTime", Calendar.getInstance().getTimeInMillis());
    if (mModelType.equals("posenet")) {
      decoder = PosenetDecoder.getInstance(mModel, mDecoderParams);
    } else if (mModelType.equals("cpm") || mModelType.equals("hourglass")){
      decoder = CPMHourglassDecoder.getInstance(mModel, mDecoderParams);
    } else {
      throw new IllegalArgumentException(String.format("Unknown model type %s", mModelType));
    }
    mTiming.put("decoderEndTime", Calendar.getInstance().getTimeInMillis());
    decoder.setTiming(mTiming);

    try {
      decoder.run(mBitmap);
    } catch (Exception e) {
      Log.e("ReactNative", "Exception occurred in mModelProcessor", e);
    }

    // Run the task max every mModelMaxFreqms by blocking until then
    try {
      if (mModelMaxFreqms > 0) {
        long endTime = SystemClock.elapsedRealtime();
        long timeTaken = endTime - startTime;
        if (timeTaken < mModelMaxFreqms) {
          TimeUnit.MILLISECONDS.sleep(mModelMaxFreqms - timeTaken);
        }
      }
    } catch (Exception e) {}

    // Decode pose and return if found
    mTiming.put("decodingBeginTime", Calendar.getInstance().getTimeInMillis());
    List<Map<String, Object>> poses = decoder.getPoses();
    mTiming.put("decodingEndTime", Calendar.getInstance().getTimeInMillis());
    return poses;
  }

  @Override
  protected void onPostExecute(List<Map<String, Object>> data) {
    super.onPostExecute(data);
    mDelegate.onModelProcessed(data, mWidth, mHeight, mRotation, mCameraOrientation, mDeviceRotation, mTiming);
    mDelegate.onModelProcessorTaskCompleted();
  }
}
