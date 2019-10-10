package org.reactnative.camera.tasks;

import org.tensorflow.lite.Interpreter;
import android.os.SystemClock;
import java.nio.ByteBuffer;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;

import android.util.Log;
import com.indigoviolet.posenet.PosenetDecoder;

public class ModelProcessorAsyncTask extends android.os.AsyncTask<Void, Void, List<Map<String, Object>>> {

  private ModelProcessorAsyncTaskDelegate mDelegate;
  private Interpreter mModelProcessor;
  private String mModelType;
  private ByteBuffer mInputBuf;
  private Map<Integer, Object> mOutputBuf;
  private int mModelMaxFreqms;
  private int mModelOutputStride;
  private int mWidth;
  private int mHeight;
  private int mDeviceRotation;
  private int mCameraOrientation;
  private int mRotation;
  private long mImageTime;
  private Map<String, Long> mTiming;

  public ModelProcessorAsyncTask(
      ModelProcessorAsyncTaskDelegate delegate,
      String modelType,
      Interpreter modelProcessor,
      ByteBuffer inputBuf,
      Map<Integer, Object> outputBuf,
      int modelMaxFreqms,
      int modelOutputStride,
      int width,
      int height,
      int rotation,
      int cameraOrientation,
      int deviceRotation,
      long imageTime
  ) {
    mDelegate = delegate;
    mModelType = modelType;
    mModelProcessor = modelProcessor;
    mInputBuf = inputBuf;
    mOutputBuf = outputBuf;
    mModelMaxFreqms = modelMaxFreqms;
    mModelOutputStride = modelOutputStride;
    mWidth = width;
    mHeight = height;
    mRotation = rotation;
    mCameraOrientation = cameraOrientation;
    mDeviceRotation = deviceRotation;
    mTiming = new HashMap<>();
    mImageTime = imageTime;
    // Log.i("ReactNative", String.format("mWidth=%d, mHeight=%d, mDeviceRotation=%d", mWidth, mHeight, mDeviceRotation));
  }

  @Override
  protected List<Map<String, Object>> doInBackground(Void... ignored) {
    if (isCancelled() || mDelegate == null || mModelProcessor == null) {
      return null;
    }
    long startTime = SystemClock.elapsedRealtime();
    try {
      mTiming.put("imageTime", mImageTime);
      mTiming.put("inferenceBeginTime", Calendar.getInstance().getTimeInMillis());

      mInputBuf.rewind();
      mModelProcessor.runForMultipleInputsOutputs(new Object[]{mInputBuf}, mOutputBuf);
      mTiming.put("inference_ns", mModelProcessor.getLastNativeInferenceDurationNanoseconds());
      mTiming.put("inferenceEndTime", Calendar.getInstance().getTimeInMillis());
      Log.i("ReactNative", String.format("Model Processed in %d ms (%d ns)",
                                         SystemClock.elapsedRealtime() - startTime,
                                         mModelProcessor.getLastNativeInferenceDurationNanoseconds()));
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

    List<Map<String, Object>> poses = new ArrayList<>();
    if (mModelType == "posenet") {
      // Decode pose and return if found
      PosenetDecoder pd = new PosenetDecoder(mModelOutputStride);
      poses = pd.decode(mOutputBuf, 1, 0.5f, 20);
    }
    return poses;
  }

  @Override
  protected void onPostExecute(List<Map<String, Object>> data) {
    super.onPostExecute(data);
    mDelegate.onModelProcessed(data, mWidth, mHeight, mRotation, mCameraOrientation, mDeviceRotation, mTiming);
    mDelegate.onModelProcessorTaskCompleted();
  }
}
