package org.reactnative.camera.tasks;

import org.tensorflow.lite.Interpreter;
import android.os.SystemClock;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;
import android.view.TextureView;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;

public class ModelProcessorAsyncTask extends android.os.AsyncTask<Void, Void, Map<Integer, Object>> {

  private ModelProcessorAsyncTaskDelegate mDelegate;
  private Interpreter mModelProcessor;
  private ByteBuffer mInputBuf;
  private Map<Integer, Object> mOutputBuf;
  private int mModelMaxFreqms;
  private int mWidth;
  private int mHeight;
  private int mRotation;

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
  }

  @Override
  protected Map<Integer, Object> doInBackground(Void... ignored) {
    if (isCancelled() || mDelegate == null || mModelProcessor == null) {
      return null;
    }
    long startTime = SystemClock.uptimeMillis();
    try {
      mInputBuf.rewind();
      // mOutputBuf.rewind();
      mModelProcessor.runForMultipleInputsOutputs(new Object[]{mInputBuf}, mOutputBuf);
    } catch (Exception e) {}
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
      mDelegate.onModelProcessed(data, mWidth, mHeight, mRotation);
    }
    mDelegate.onModelProcessorTaskCompleted();
  }
}
