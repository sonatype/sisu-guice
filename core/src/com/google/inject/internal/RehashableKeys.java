/**
 * Copyright (C) 2013 Google Inc.
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

package com.google.inject.internal;

import com.google.inject.Key;

/**
 * Interface for types that can rehash their {@link Key} instances.
 *
 * @author chrispurcell@google.com (Chris Purcell)
 */
public interface RehashableKeys {
  void rehashKeys();

  /** Utility methods for key rehashing. */
  public static class Keys {

    /**
     * Returns true if the cached hashcode and toString for the given key is out-of-date. This will
     * only occur if the key contains a mutable annotation.
     */
    public static boolean needsRehashing(Key<?> key) {
      if (!key.hasAttributes()) {
        return false;
      }
      int newHashCode = key.getTypeLiteral().hashCode() * 31 + key.getAnnotation().hashCode();
      return (key.hashCode() != newHashCode);
    }

    /** Returns a copy of the given key with an up-to-date hashcode and toString. */
    public static <T> Key<T> rehash(Key<T> key) {
      if (key.hasAttributes()) {
        // This will recompute the hashcode for us.
        return Key.get(key.getTypeLiteral(), key.getAnnotation());
      } else {
        return key;
      }
    }

    private Keys() { }
  }
}
