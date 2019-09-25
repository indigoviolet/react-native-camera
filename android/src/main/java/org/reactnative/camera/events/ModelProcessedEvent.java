package org.reactnative.camera.events;

import androidx.core.util.Pools;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import org.reactnative.camera.CameraViewManager;
import org.reactnative.camera.utils.ImageDimensions;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Calendar;

import com.indigoviolet.react.ArrayUtil;
import com.indigoviolet.react.MapUtil;

public class ModelProcessedEvent extends Event<ModelProcessedEvent> {

  private static final Pools.SynchronizedPool<ModelProcessedEvent> EVENTS_POOL =
      new Pools.SynchronizedPool<>(3);


  private double mScaleX;
  private double mScaleY;
  private List<Map<String, Object>> mData;
  private Map<String, Long> mTiming;
  private ImageDimensions mImageDimensions;
  private int mCameraOrientation;
  private int mDeviceRotation;

  private ModelProcessedEvent() {}

  public static ModelProcessedEvent obtain(
      int viewTag,
      List<Map<String, Object>> data,
      ImageDimensions dimensions,
      int cameraOrientation,
      int deviceRotation,
      double scaleX,
      double scaleY,
      Map<String, Long> timing) {
    ModelProcessedEvent event = EVENTS_POOL.acquire();
    if (event == null) {
      event = new ModelProcessedEvent();
    }
    event.init(viewTag, data, dimensions, cameraOrientation, deviceRotation, scaleX, scaleY, timing);
    return event;
  }

  private void init(
      int viewTag,
      List<Map<String, Object>> data,
      ImageDimensions dimensions,
      int cameraOrientation,
      int deviceRotation,
      double scaleX,
      double scaleY,
      Map<String, Long> timing) {
    super.init(viewTag);
    mData = data;
    mImageDimensions = dimensions;
    mCameraOrientation = cameraOrientation;
    mDeviceRotation = deviceRotation;
    mScaleX = scaleX;
    mScaleY = scaleY;
    mTiming = timing;
  }

  @Override
  public String getEventName() {
    return CameraViewManager.Events.EVENT_ON_MODEL_PROCESSED.toString();
  }

  @Override
  public void dispatch(RCTEventEmitter rctEventEmitter) {
    rctEventEmitter.receiveEvent(getViewTag(), getEventName(), serializeEventData());
  }


  private WritableMap serializeEventData() {
    mTiming.put("serializationBeginTime", Calendar.getInstance().getTimeInMillis());

    WritableMap event = Arguments.createMap();
    event.putString("type", "pose");
    event.putArray("data", ArrayUtil.toWritableArray(mData.toArray()));

    WritableMap scaleMap = Arguments.createMap();
    scaleMap.putDouble("scaleX", mScaleX);
    scaleMap.putDouble("scaleY", mScaleY);
    event.putMap("scale", scaleMap);

    WritableMap dimMap = Arguments.createMap();
    dimMap.putInt("height", mImageDimensions.getHeight());
    dimMap.putInt("width", mImageDimensions.getWidth());
    dimMap.putInt("rotation", mImageDimensions.getRotation());
    dimMap.putInt("cameraOrientation", mCameraOrientation);
    dimMap.putInt("deviceRotation", mDeviceRotation);
    dimMap.putInt("facing", mImageDimensions.getFacing());
    event.putMap("dimensions", dimMap);

    Map<String, Object> timingObj = new HashMap<>();
    mTiming.put("serializationEndTime", Calendar.getInstance().getTimeInMillis());
    timingObj.putAll(mTiming);
    event.putMap("timing", MapUtil.toWritableMap(timingObj));

    event.putInt("target", getViewTag());
    return event;
  }

}
