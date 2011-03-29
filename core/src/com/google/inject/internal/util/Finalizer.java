/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.inject.internal.util;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread that finalizes referents. All references should implement
 * {@code com.google.inject.internal.FinalizableReference}.
 *
 * <p>While this class is public, we consider it to be *internal* and not part
 * of our published API. It is public so we can access it reflectively across
 * class loaders in secure environments.
 *
 * <p>This class can't depend on other Google Collections code. If we were
 * to load this class in the same class loader as the rest of
 * Google Collections, this thread would keep an indirect strong reference
 * to the class loader and prevent it from being garbage collected. This
 * poses a problem for environments where you want to throw away the class
 * loader. For example, dynamically reloading a web application or unloading
 * an OSGi bundle.
 *
 * <p>{@code com.google.inject.internal.FinalizableReferenceQueue} loads this class
 * in its own class loader. That way, this class doesn't prevent the main
 * class loader from getting garbage collected, and this class can detect when
 * the main class loader has been garbage collected and stop itself.
 */
public class Finalizer implements Runnable {

  private static final Logger logger
      = Logger.getLogger(Finalizer.class.getName());

  /** Name of FinalizableReference.class. */
  private static final String FINALIZABLE_REFERENCE
      = "com.google.inject.internal.util.FinalizableReference";

  /** Use "-Dguice.executor.class=Clazz" where Clazz implements java.util.concurrent.Executor. */
  private static final String EXECUTOR_CLASS_NAME;

  static {
    String executorClassName = null;
    try {
      executorClassName = System.getProperty("guice.executor.class");
    } catch (Throwable t) {}
    EXECUTOR_CLASS_NAME = executorClassName;
  }

  /**
   * Starts the Finalizer thread. FinalizableReferenceQueue calls this method
   * reflectively.
   *
   * @param finalizableReferenceClass FinalizableReference.class
   * @param frq reference to instance of FinalizableReferenceQueue that started
   *  this thread
   * @return ReferenceQueue which Finalizer will poll
   */
  public static ReferenceQueue<Object> startFinalizer(
      Class<?> finalizableReferenceClass, Object frq) {
    /*
     * We use FinalizableReference.class for two things:
     *
     * 1) To invoke FinalizableReference.finalizeReferent()
     *
     * 2) To detect when FinalizableReference's class loader has to be garbage
     * collected, at which point, Finalizer can stop running
     */
    if (!finalizableReferenceClass.getName().equals(FINALIZABLE_REFERENCE)) {
      throw new IllegalArgumentException(
          "Expected " + FINALIZABLE_REFERENCE + ".");
    }

    if ("NONE".equalsIgnoreCase(EXECUTOR_CLASS_NAME)) {
      return null;
    }

    Finalizer finalizer = new Finalizer(finalizableReferenceClass, frq);

    try {
      if (EXECUTOR_CLASS_NAME == null || EXECUTOR_CLASS_NAME.length() == 0) {
        Thread thread = new Thread(finalizer, Finalizer.class.getName());
        thread.setDaemon(true);
        // TODO: Priority?
        thread.start();
      } else {
        Class<?> executorClass;
        try {
          executorClass = Thread.currentThread().getContextClassLoader().loadClass(EXECUTOR_CLASS_NAME);
        } catch (Throwable ignore) {
          executorClass = Class.forName(EXECUTOR_CLASS_NAME);
        }
        // use custom Executor supplied by an external container
        ((Executor)executorClass.newInstance()).execute(finalizer);
      }
      return finalizer.queue;
    } catch (Throwable t) {
      logger.log(Level.WARNING, "Cannot start Finalizer thread.", t);
      return null;
    }
  }

  private final WeakReference<Class<?>> finalizableReferenceClassReference;
  private final PhantomReference<Object> frqReference;
  private final ReferenceQueue<Object> queue = new ReferenceQueue<Object>();

  /** Constructs a new finalizer thread. */
  private Finalizer(Class<?> finalizableReferenceClass, Object frq) {

    this.finalizableReferenceClassReference
        = new WeakReference<Class<?>>(finalizableReferenceClass);

    // Keep track of the FRQ that started us so we know when to stop.
    this.frqReference = new PhantomReference<Object>(frq, queue);
  }

  /**
   * Loops continuously, pulling references off the queue and cleaning them up.
   */
  @SuppressWarnings("InfiniteLoopStatement")
  public void run() {
    try {
      while (true) {
        try {
          cleanUp(queue.remove());
        } catch (InterruptedException e) { /* ignore */ }
      }
    } catch (CancellationException shutDown) { /* ignore */ }
  }

  /**
   * Cleans up a single reference. Catches and logs all throwables.
   */
  private void cleanUp(Reference<?> reference) {
    Method finalizeReferentMethod = getFinalizeReferentMethod();
    do {
      /*
       * This is for the benefit of phantom references. Weak and soft
       * references will have already been cleared by this point.
       */
      reference.clear();

      if (reference == frqReference) {
        /*
         * The client no longer has a reference to the
         * FinalizableReferenceQueue. We can stop.
         */
        throw new CancellationException();
      }

      try {
        finalizeReferentMethod.invoke(reference);
      } catch (Throwable t) {
        logger.log(Level.SEVERE, "Error cleaning up after reference.", t);
      }

      /*
       * Loop as long as we have references available so as not to waste
       * CPU looking up the Method over and over again.
       */
    } while ((reference = queue.poll()) != null);
  }

  /**
   * Looks up FinalizableReference.finalizeReferent() method.
   */
  private Method getFinalizeReferentMethod() {
    Class<?> finalizableReferenceClass
        = finalizableReferenceClassReference.get();
    if (finalizableReferenceClass == null) {
      /*
       * FinalizableReference's class loader was reclaimed. While there's a
       * chance that other finalizable references could be enqueued
       * subsequently (at which point the class loader would be resurrected
       * by virtue of us having a strong reference to it), we should pretty
       * much just shut down and make sure we don't keep it alive any longer
       * than necessary.
       */
      throw new CancellationException();
    }
    try {
      return finalizableReferenceClass.getMethod("finalizeReferent");
    } catch (NoSuchMethodException e) {
      throw new AssertionError(e);
    }
  }
}
