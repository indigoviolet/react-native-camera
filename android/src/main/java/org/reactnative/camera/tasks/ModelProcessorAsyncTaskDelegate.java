package org.reactnative.camera.tasks;

import java.util.List;
import java.util.Map;

public interface ModelProcessorAsyncTaskDelegate {
  void onModelProcessed(List<Map<String, Object>> data, int sourceWidth, int sourceHeight, int sourceRotation, Map<String, Long> timing);
  void onModelProcessorTaskCompleted();
}
