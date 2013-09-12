/**
 * Copyright (C) 2006 Google Inc.
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

package com.google.inject.internal.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.MapMaker;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Creates stack trace elements for members.
 *
 * @author crazybob@google.com (Bob Lee)
 */
public class StackTraceElements {

  /*if[AOP]*/
  static final LoadingCache<Class<?>, LineNumbers> lineNumbersCache = CacheBuilder.newBuilder().weakKeys().softValues()
      .build(new CacheLoader<Class<?>, LineNumbers>() {
        public LineNumbers load(Class<?> key) {
          try {
            return new LineNumbers(key);
          }
          catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      });
  /*end[AOP]*/
  
  private static Map<Object, Object> cache = new MapMaker().makeMap();
  private static final String UNKNOWN_SOURCE = "Unknown Source";
  
  public static Object forMember(Member member) {
    if (member == null) {
      return SourceProvider.UNKNOWN_SOURCE;
    }

    Class declaringClass = member.getDeclaringClass();

    /*if[AOP]*/
    LineNumbers lineNumbers;
    try {
      lineNumbers = lineNumbersCache.get(declaringClass);
    } catch (ExecutionException e) {
      throw new RuntimeException("Failed to load from the cache", e.getCause());
    }
    String fileName = lineNumbers.getSource();
    Integer lineNumberOrNull = lineNumbers.getLineNumber(member);
    int lineNumber = lineNumberOrNull == null ? lineNumbers.getFirstLine() : lineNumberOrNull;
    /*end[AOP]*/
    /*if[NO_AOP]
    String fileName = null;
    int lineNumber = -1;
    end[NO_AOP]*/

    Class<? extends Member> memberType = Classes.memberType(member);
    String memberName = memberType == Constructor.class ? "<init>" : member.getName();
    return new StackTraceElement(declaringClass.getName(), memberName, fileName, lineNumber);
  }

  public static Object forType(Class<?> implementation) {
    /*if[AOP]*/
    LineNumbers lineNumbers;
    try {
      lineNumbers = lineNumbersCache.get(implementation);
    } catch (ExecutionException e) {
      throw new RuntimeException("Failed to load from the cache", e.getCause());
    }
    int lineNumber = lineNumbers.getFirstLine();
    String fileName = lineNumbers.getSource();
    /*end[AOP]*/
    /*if[NO_AOP]
    String fileName = null;
    int lineNumber = -1;
    end[NO_AOP]*/

    return new StackTraceElement(implementation.getName(), "class", fileName, lineNumber);
  }
  
  /**
   * Clears the internal cache for {@link StackTraceElement StackTraceElements}.
   */
  public static void clearCache() {
    cache.clear();
  }
  
  /**
   * Returns encoded in-memory version of {@link StackTraceElement StackTraceElements}.
   */
  public static InMemoryStackTraceElement[] convertToInMemoryStackTraceElement(
      StackTraceElement[] stackTraceElements) {
    InMemoryStackTraceElement[] inMemoryStackTraceElements = 
        new InMemoryStackTraceElement[stackTraceElements.length];
    for (int i = 0; i < stackTraceElements.length; i++) {
      inMemoryStackTraceElements[i] = 
          weakIntern(new InMemoryStackTraceElement(stackTraceElements[i]));
    }
    return inMemoryStackTraceElements;
  }
  
  /**
   * Decodes in-memory stack trace elements to regular {@link StackTraceElement StackTraceElements}.
   */
  public static StackTraceElement[] convertToStackTraceElement(
      InMemoryStackTraceElement[] inMemoryStackTraceElements) {
    StackTraceElement[] stackTraceElements = 
        new StackTraceElement[inMemoryStackTraceElements.length];
    for (int i = 0; i < inMemoryStackTraceElements.length; i++) {
      String declaringClass = inMemoryStackTraceElements[i].getClassName();
      String methodName = inMemoryStackTraceElements[i].getMethodName();
      int lineNumber = inMemoryStackTraceElements[i].getLineNumber();
      stackTraceElements[i] = 
          new StackTraceElement(declaringClass, methodName, UNKNOWN_SOURCE, lineNumber);
    }
    return stackTraceElements;
  }
  
  private static InMemoryStackTraceElement weakIntern(
      InMemoryStackTraceElement inMemoryStackTraceElement) {
    InMemoryStackTraceElement cached = 
        (InMemoryStackTraceElement) cache.get(inMemoryStackTraceElement);
    if (cached != null) {
      return cached;
    }
    inMemoryStackTraceElement = new InMemoryStackTraceElement(
        weakIntern(inMemoryStackTraceElement.getClassName()), 
        weakIntern(inMemoryStackTraceElement.getMethodName()), 
        inMemoryStackTraceElement.getLineNumber());
    cache.put(inMemoryStackTraceElement, inMemoryStackTraceElement);
    return inMemoryStackTraceElement;
  }
  
  private static String weakIntern(String s) {
    String cached = (String) cache.get(s);
    if (cached != null) {
      return cached;
    }
    cache.put(s, s);
    return s;  
  }
  
  /**
   * In-Memory version of {@link StackTraceElement} that does not store the file name. 
   */
  public static class InMemoryStackTraceElement {
    private String declaringClass;
    private String methodName;
    private int lineNumber;

    InMemoryStackTraceElement(StackTraceElement ste) {
      this(ste.getClassName(), ste.getMethodName(), ste.getLineNumber());
    }

    InMemoryStackTraceElement(String declaringClass, String methodName, int lineNumber) {
      this.declaringClass = declaringClass;
      this.methodName = methodName;
      this.lineNumber = lineNumber;
    }

    String getClassName() {
      return declaringClass;
    }
    
    String getMethodName() {
      return methodName;
    }
    
    int getLineNumber() {
      return lineNumber;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof InMemoryStackTraceElement)) {
        return false;
      }
      InMemoryStackTraceElement e = (InMemoryStackTraceElement) obj;
      return e.declaringClass.equals(declaringClass) && e.lineNumber == lineNumber && 
          methodName.equals(e.methodName);
    }

    @Override
    public int hashCode() {
      int result = 31 * declaringClass.hashCode() + methodName.hashCode();
      result = 31 * result + lineNumber;
      return result;
    }
    
    @Override
    public String toString() {
      return declaringClass + "." + methodName + "(" + lineNumber + ")";
    }
  }
}
