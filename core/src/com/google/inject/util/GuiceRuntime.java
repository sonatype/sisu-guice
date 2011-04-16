/**
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.inject.util;

/**
 * Use "-Dguice.executor.class=Clazz" where Clazz implements java.util.concurrent.Executor.
 */
@Deprecated
public final class GuiceRuntime {

  private static String executorClassName;

  private static boolean canSetExecutor = true;

  private GuiceRuntime() {}

  @Deprecated
  public static synchronized void setExecutorClassName(String name) {
    if (canSetExecutor) {
      executorClassName = name;
    } else {
      throw new IllegalStateException("Cannot set 'guice.executor.class' after it has been used.");
    }
  }

  @Deprecated
  public static synchronized String getExecutorClassName() {
    canSetExecutor = false;
    return executorClassName;
  }
}
