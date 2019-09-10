package org.reactnative.camera.tasks;

import org.tensorflow.lite.Interpreter;
import android.os.SystemClock;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;
import android.view.TextureView;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;

import android.util.Log;

public class ModelProcessorAsyncTask extends android.os.AsyncTask<Void, Void, Map<Integer, Object>> {

  private ModelProcessorAsyncTaskDelegate mDelegate;
  private Interpreter mModelProcessor;
  private ByteBuffer mInputBuf;
  private Map<Integer, Object> mOutputBuf;
  private int mModelMaxFreqms;
  private int mWidth;
  private int mHeight;
  private int mRotation;
  private Map<String, Long> mTiming;

  public ModelProcessorAsyncTask(
      ModelProcessorAsyncTaskDelegate delegate,
      Interpreter modelProcessor,
      ByteBuffer inputBuf,
      Map<Integer, Object> outputBuf,
      int modelMaxFreqms,
      int width,
      int height,
      int rotation
  ) {
    mDelegate = delegate;
    mModelProcessor = modelProcessor;
    mInputBuf = inputBuf;
    mOutputBuf = outputBuf;
    mModelMaxFreqms = modelMaxFreqms;
    mWidth = width;
    mHeight = height;
    mRotation = rotation;
    mTiming = new HashMap<>();
    Log.i("ReactNative", String.format("mWidth=%d, mHeight=%d, mRotation=%d", mWidth, mHeight, mRotation));
  }

  @Override
  protected Map<Integer, Object> doInBackground(Void... ignored) {
    if (isCancelled() || mDelegate == null || mModelProcessor == null) {
      return null;
    }
    long startTime = SystemClock.uptimeMillis();
    try {
      mInputBuf.rewind();
      mModelProcessor.runForMultipleInputsOutputs(new Object[]{mInputBuf}, mOutputBuf);
      mTiming.put("inference_ns", mModelProcessor.getLastNativeInferenceDurationNanoseconds());
      Log.i("ReactNative", String.format("Model Processed in %d ms (%d ns)",
                                         SystemClock.uptimeMillis() - startTime,
                                         mModelProcessor.getLastNativeInferenceDurationNanoseconds()));
    } catch (Exception e) {
      Log.e("ReactNative", "Exception occurred in mModelProcessor", e);
    }

    // Run the task max every mModelMaxFreqms by blocking until then
    try {
      if (mModelMaxFreqms > 0) {
        long endTime = SystemClock.uptimeMillis();
        long timeTaken = endTime - startTime;
        if (timeTaken < mModelMaxFreqms) {
          TimeUnit.MILLISECONDS.sleep(mModelMaxFreqms - timeTaken);
        }
      }
    } catch (Exception e) {}
    return mOutputBuf;
  }

  @Override
  protected void onPostExecute(Map<Integer, Object> data) {
    super.onPostExecute(data);

    if (data != null) {
      mDelegate.onModelProcessed(data, mWidth, mHeight, mRotation, mTiming);
    }
    mDelegate.onModelProcessorTaskCompleted();
  }
}
