package com.google.inject.internal;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.google.inject.Singleton;

/**
 * One instance per {@link Injector}. Also see {@code @}{@link Singleton}.
 */
public class SingletonScope implements Scope {

  /** A sentinel value representing null. */
  private static final Object NULL = new Object();

  public <T> Provider<T> scope(final Key<T> key, final Provider<T> creator) {
    return new Provider<T>() {
      /*
       * The lazily initialized singleton instance. Once set, this will either have type T or will
       * be equal to NULL.
       */
      private volatile Object instance;

      // DCL on a volatile is safe as of Java 5, which we obviously require.
      @SuppressWarnings("DoubleCheckedLocking")
      public T get() {
        if (instance == null) {
          /*
           * This block is re-entrant for circular dependencies.
           */
          synchronized (this) {
            if (instance == null) {
              T provided = creator.get();

              // don't remember proxies; these exist only to serve circular dependencies
              if (Scopes.isCircularProxy(provided)) {
                return provided;
              }

              Object providedOrSentinel = (provided == null) ? NULL : provided;
              if (instance != null && instance != providedOrSentinel) {
                throw new ProvisionException(
                    "Provider was reentrant while creating a singleton");
              }

              instance = providedOrSentinel;
            }
          }
        }

        Object localInstance = instance;
        // This is safe because instance has type T or is equal to NULL
        @SuppressWarnings("unchecked")
        T returnedInstance = (localInstance != NULL) ? (T) localInstance : null;
        return returnedInstance;
      }

      @Override
      public String toString() {
        return String.format("%s[%s]", creator, Scopes.SINGLETON);
      }
    };
  }

  @Override public String toString() {
    return "Scopes.SINGLETON";
  }
}
