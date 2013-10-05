package com.google.inject.internal;


/**
 * Contains flags for Guice.
 */
public class InternalFlags {

  private static final IncludeStackTraceOption INCLUDE_STACK_TRACES;
  static {
    IncludeStackTraceOption includeStackTraces;
    try {
      includeStackTraces = IncludeStackTraceOption.valueOf(
          System.getProperty("guice_include_stack_traces"));
    } catch (Throwable e) {
      includeStackTraces = IncludeStackTraceOption.ONLY_FOR_DECLARING_SOURCE;
    }
    INCLUDE_STACK_TRACES = includeStackTraces;
  }

  /**
   * The options for Guice stack trace collection.
   */
  public enum IncludeStackTraceOption {
    // No stack trace collection
    OFF,
    // Minimum stack trace collection (Default)
    ONLY_FOR_DECLARING_SOURCE,
    // Full stack trace for everything
    COMPLETE
  }


  public static IncludeStackTraceOption getIncludeStackTraceOption() {
    return INCLUDE_STACK_TRACES;
  }
}
