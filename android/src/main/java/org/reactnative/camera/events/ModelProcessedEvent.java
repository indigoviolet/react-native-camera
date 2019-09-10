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

import com.indigoviolet.react.ArrayUtil;
import com.indigoviolet.react.MapUtil;

public class ModelProcessedEvent extends Event<ModelProcessedEvent> {

  private static final Pools.SynchronizedPool<ModelProcessedEvent> EVENTS_POOL =
      new Pools.SynchronizedPool<>(3);


  private double mScaleX;
  private double mScaleY;
  private Map<Integer, Object> mData;
  private Map<String, Long> mTiming;
  private ImageDimensions mImageDimensions;

  private ModelProcessedEvent() {}

  public static ModelProcessedEvent obtain(
      int viewTag,
      Map<Integer, Object> data,
      ImageDimensions dimensions,
      double scaleX,
      double scaleY,
      Map<String, Long> timing) {
    ModelProcessedEvent event = EVENTS_POOL.acquire();
    if (event == null) {
      event = new ModelProcessedEvent();
    }
    event.init(viewTag, data, dimensions, scaleX, scaleY, timing);
    return event;
  }

  private void init(
      int viewTag,
      Map<Integer, Object> data,
      ImageDimensions dimensions,
      double scaleX,
      double scaleY,
      Map<String, Long> timing) {
    super.init(viewTag);
    mData = data;
    mImageDimensions = dimensions;
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
    WritableMap event = Arguments.createMap();
    event.putString("type", "pose");
    event.putArray("data", ArrayUtil.toWritableArray(mData.values().toArray()));

    WritableMap scaleMap = Arguments.createMap();
    scaleMap.putDouble("scaleX", mScaleX);
    scaleMap.putDouble("scaleY", mScaleY);
    event.putMap("scale", scaleMap);

    WritableMap dimMap = Arguments.createMap();
    dimMap.putInt("height", mImageDimensions.getHeight());
    dimMap.putInt("width", mImageDimensions.getWidth());
    dimMap.putInt("rotation", mImageDimensions.getRotation());
    dimMap.putInt("facing", mImageDimensions.getFacing());
    event.putMap("dimensions", dimMap);

    Map<String, Object> timingObj = new HashMap<>();
    timingObj.putAll(mTiming);
    event.putMap("timing", MapUtil.toWritableMap(timingObj));

    event.putInt("target", getViewTag());
    return event;
  }

}
