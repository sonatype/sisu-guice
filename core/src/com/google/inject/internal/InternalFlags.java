package com.google.inject.internal;

import java.util.Arrays;
import java.util.logging.Logger;
/**
 * Contains flags for Guice.
 */
public class InternalFlags {
  private final static Logger logger = Logger.getLogger(InternalFlags.class.getName());

  private static final IncludeStackTraceOption INCLUDE_STACK_TRACES;
  static {
    IncludeStackTraceOption includeStackTraces;
    try {
      includeStackTraces = parseIncludeStackTraceOption();
    } catch (Throwable e) {
      includeStackTraces = IncludeStackTraceOption.ONLY_FOR_DECLARING_SOURCE;
    }
    INCLUDE_STACK_TRACES = includeStackTraces;
  }

  /**
   * The options for Guice stack trace collection.
   */
  public enum IncludeStackTraceOption {
    /** No stack trace collection */
    OFF,
    /**  Minimum stack trace collection (Default) */
    ONLY_FOR_DECLARING_SOURCE,
    /** Full stack trace for everything */
    COMPLETE
  }


  public static IncludeStackTraceOption getIncludeStackTraceOption() {
    return INCLUDE_STACK_TRACES;
  }

  private static IncludeStackTraceOption parseIncludeStackTraceOption() {
    String flag = System.getProperty("guice_include_stack_traces");
    try {
      return (flag == null || flag.length() == 0)
          ? IncludeStackTraceOption.ONLY_FOR_DECLARING_SOURCE
          : IncludeStackTraceOption.valueOf(flag);
    } catch (IllegalArgumentException e) {
      logger.warning(flag
          + " is not a valid flag value for guice_include_stack_traces. "
          + " Values must be one of " + Arrays.asList(IncludeStackTraceOption.values()));
      return IncludeStackTraceOption.ONLY_FOR_DECLARING_SOURCE;
    }
  }
}
