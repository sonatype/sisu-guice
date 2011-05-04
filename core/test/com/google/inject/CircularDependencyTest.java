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

package com.google.inject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import junit.framework.TestCase;
import static com.google.inject.Asserts.assertContains;

/**
 * @author crazybob@google.com (Bob Lee)
 */
public class CircularDependencyTest extends TestCase {

  @Override protected void setUp() throws Exception {
    super.setUp();
    Chicken.nextInstanceId = 0;
    Egg.nextInstanceId = 0;
  }

  public void testCircularlyDependentConstructors()
      throws CreationException {
    Injector injector = Guice.createInjector(new AbstractModule() {
      protected void configure() {
        bind(A.class).to(AImpl.class);
        bind(B.class).to(BImpl.class);
      }
    });

    A a = injector.getInstance(A.class);
    assertNotNull(a.getB().getA());
  }

  interface A {
    B getB();
  }

  @Singleton
  static class AImpl implements A {
    final B b;
    @Inject public AImpl(B b) {
      this.b = b;
    }
    public B getB() {
      return b;
    }
  }

  interface B {
    A getA();
  }

  static class BImpl implements B {
    final A a;
    @Inject public BImpl(A a) {
      this.a = a;
    }
    public A getA() {
      return a;
    }
  }

  public void testUnresolvableCircularDependency() {
    try {
      Guice.createInjector().getInstance(C.class);
      fail();
    } catch (ProvisionException expected) {
      assertContains(expected.getMessage(),
          "Tried proxying " + C.class.getName() + " to support a circular dependency, ",
          "but it is not an interface.");
    }
  }

  static class ACountingImpl implements A {
    static int[] counters = new int[3];
    B b;
    ACountingImpl() {
      synchronized(counters){counters[0]++;}
    }
    public void setB(B b) {
      synchronized(counters){counters[1]++;}
      this.b = b;
    }
    public B getB() {
      synchronized(counters){counters[2]++;}
      return b;
    }
    public int id() {
      return counters[0];
    }
  }

  static class BCountingImpl implements B {
    static int[] counters = new int[3];
    A a;
    BCountingImpl() {
      synchronized(counters){counters[0]++;}
    }
    public void setA(A a) {
      synchronized(counters){counters[1]++;}
      this.a = a;
    }
    public A getA() {
      synchronized(counters){counters[2]++;}
      return a;
    }
    public int id() {
      return counters[0];
    }
  }
  
  public void testCircularDependenciesAndInjectionListeners() {
    Guice.createInjector(new AbstractModule() {

        @Provides
        @Singleton
        A provideA(ACountingImpl o) {
          return o;
        }

        @Provides
        @Singleton
        B provideB(BCountingImpl o) {
          return o;
        }

        @Override
        protected void configure() {

          final AtomicBoolean isInjecting = new AtomicBoolean();

          final Provider<A> providerA = getProvider(A.class);
          final Provider<B> providerB = getProvider(B.class);
          
          final MembersInjector<?> beanInjector = new MembersInjector<Object>() {
              public void injectMembers(Object instance) {
                boolean wasInjecting = isInjecting.getAndSet(true); // begin custom injection
                try{
                  for (Method m : instance.getClass().getMethods()) { // process setter methods
                    if (m.getName().equals("setA")) {
                      m.invoke(instance, providerA.get());
                    } else if (m.getName().equals("setB")) {
                      m.invoke(instance, providerB.get());
                    }
                  }
                } catch (Throwable e) {
                  throw new ProvisionException("Error in MembersInjector", e);
                } finally {
                  isInjecting.set(wasInjecting); // end custom injection
                }
              }
            };

          final InjectionListener<?> beanListener = new InjectionListener<Object>() {
              List<Object> instances = new ArrayList<Object>();
              public void afterInjection(Object injectee) {
                instances.add(injectee);
                if (!isInjecting.get()) { // wait until onion is fully injected :)
                  for (Object o : instances) {
                    if (o instanceof A) {
                      assertNotNull(((A)o).getB().getA().getB());
                    }
                    if (o instanceof B) {
                      assertNotNull(((B)o).getA().getB().getA());
                    }
                  }
                  instances.clear();
                }
              }
            };

          bindListener(Matchers.any(), new TypeListener() {
            public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
              encounter.register((MembersInjector<I>)beanInjector);
              encounter.register((InjectionListener<I>)beanListener);
            }
          });
        }

    }).getInstance(A.class).getB().getA().getB().getA();

    // expect to see: constructed once, set once, got five times
    assertTrue(Arrays.equals(new int[]{1, 1, 5}, ACountingImpl.counters));
    assertTrue(Arrays.equals(new int[]{1, 1, 5}, BCountingImpl.counters));
  }

  static class C {
    @Inject C(D d) {}
  }

  static class D {
    @Inject D(C c) {}
  }

  /**
   * As reported by issue 349, we give a lousy trace when a class is circularly
   * dependent on itself in multiple ways.
   */
  public void testCircularlyDependentMultipleWays() {
    Injector injector = Guice.createInjector(new AbstractModule() {
      protected void configure() {
        binder.bind(A.class).to(E.class);
        binder.bind(B.class).to(E.class);
      }
    });
    injector.getInstance(A.class);
  }
  
  public void testDisablingCircularProxies() {
    Injector injector = Guice.createInjector(new AbstractModule() {
      protected void configure() {
        binder().disableCircularProxies();
        binder.bind(A.class).to(E.class);
        binder.bind(B.class).to(E.class);
      }
    });
    
    try {
      injector.getInstance(A.class);
      fail("expected exception");
    } catch(ProvisionException expected) {
      assertContains(expected.getMessage(),
          "Tried proxying " + A.class.getName() + " to support a circular dependency, but circular proxies are disabled", 
          "Tried proxying " + B.class.getName() + " to support a circular dependency, but circular proxies are disabled");
    }
  }

  @Singleton
  static class E implements A, B {
    @Inject
    public E(A a, B b) {}

    public B getB() {
      return this;
    }

    public A getA() {
      return this;
    }
  }

  static class Chicken {
    static int nextInstanceId;
    final int instanceId = nextInstanceId++;
    @Inject Egg source;
  }

  static class Egg {
    static int nextInstanceId;
    final int instanceId = nextInstanceId++;
    @Inject Chicken source;
  }

  public void testCircularlyDependentSingletonsWithProviders() {
    Injector injector = Guice.createInjector(new AbstractModule() {
      protected void configure() {
        bind(Chicken.class).in(Singleton.class);
      }

      @Provides @Singleton Egg provideEgg(Chicken chicken) {
        Egg egg = new Egg();
        egg.source = chicken;
        return egg;
      }
    });

    try {
      injector.getInstance(Egg.class);
      fail();
    } catch (ProvisionException e) {
      assertContains(e.getMessage(),
          "Provider was reentrant while creating a singleton",
          " at " + CircularDependencyTest.class.getName(), "provideEgg(",
          " while locating " + Egg.class.getName());
    }
  }

  public void testCircularDependencyProxyDelegateNeverInitialized() {
    Injector injector = Guice.createInjector(new AbstractModule() {
      protected void configure() {
        bind(F.class).to(RealF.class);
        bind(G.class).to(RealG.class);
      }
    });
    F f = injector.getInstance(F.class);
    assertEquals("F", f.g().f().toString());
    assertEquals("G", f.g().f().g().toString());

  }

  public interface F {
    G g();
  }

  @Singleton
  public static class RealF implements F {
    private final G g;
    @Inject RealF(G g) {
      this.g = g;
    }

    public G g() {
      return g;
    }

    @Override public String toString() {
      return "F";
    }
  }

  public interface G {
    F f();
  }

  @Singleton
  public static class RealG implements G {
    private final F f;
    @Inject RealG(F f) {
      this.f = f;
    }

    public F f() {
      return f;
    }

    @Override public String toString() {
      return "G";
    }
  }

}
