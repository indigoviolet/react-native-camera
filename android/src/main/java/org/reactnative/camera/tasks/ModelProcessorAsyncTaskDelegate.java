package org.reactnative.camera.tasks;

import java.util.Map;

public interface ModelProcessorAsyncTaskDelegate {
  void onModelProcessed(Map<Integer, Object> data, int sourceWidth, int sourceHeight, int sourceRotation, Map<String, Long> timing);
  void onModelProcessorTaskCompleted();
}
