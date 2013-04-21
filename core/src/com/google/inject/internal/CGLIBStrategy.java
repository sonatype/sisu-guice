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

/*if[AOP]*/
// workaround for missing TraceClassVisitor in CGLIB 3.0 
public final class CGLIBStrategy
  implements net.sf.cglib.core.GeneratorStrategy {
  public static final CGLIBStrategy INSTANCE = new CGLIBStrategy();
  public byte[] generate(net.sf.cglib.core.ClassGenerator cg) throws Exception {
    org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(
        org.objectweb.asm.ClassWriter.COMPUTE_MAXS);
    cg.generateClass(cw);
    return cw.toByteArray();
  }
}
/*end[AOP]*/
