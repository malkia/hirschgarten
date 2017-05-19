/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.base.logging;

import com.intellij.openapi.extensions.ExtensionPointName;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Forwards the event logs to an applicable receiver extension or discard them if no applicable
 * receivers exist.
 */
public interface EventLogger {
  ExtensionPointName<EventLogger> EP_NAME =
      new ExtensionPointName<>("com.google.idea.blaze.EventLogger");

  static EventLogger getInstance() {
    for (EventLogger logger : EP_NAME.getExtensions()) {
      if (logger.isApplicable()) {
        return logger;
      }
    }
    return NullEventLogger.SINGLETON;
  }

  boolean isApplicable();

  default void log(Class<?> loggingClass, String eventType, Map<String, String> keyValues) {
    log(loggingClass, eventType, keyValues, null);
  }

  void log(
      Class<?> loggingClass,
      String eventType,
      Map<String, String> keyValues,
      @Nullable Long durationInNanos);
}
