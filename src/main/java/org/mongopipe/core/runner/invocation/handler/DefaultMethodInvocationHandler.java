/*
 * Copyright (c) 2022 - present Cristian Donoiu, Ionut Sergiu Peschir
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongopipe.core.runner.invocation.handler;

import org.mongopipe.core.Pipelines;
import org.mongopipe.core.logging.CustomLogFactory;
import org.mongopipe.core.logging.Log;
import org.mongopipe.core.util.ReflectionUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

/**
 * Method interceptor to invoke default methods on the repository proxy.
 * NOTE: MethodHandles API can be much faster than the Reflection API since the access checks are made at creation time rather than at execution
 * time.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Johannes Englmeier
 */
public class DefaultMethodInvocationHandler implements StoreMethodHandler {
  private static final Log LOG = CustomLogFactory.getLogger(DefaultMethodInvocationHandler.class);
  private final MethodHandleLookup methodHandleLookup = MethodHandleLookup.getMethodHandleLookup();

  // https://stackoverflow.com/questions/37812393/how-to-explicitly-invoke-default-method-from-a-dynamic-proxy
  // https://stackoverflow.com/questions/26206614/java8-dynamic-proxy-and-default-methods
  // http://netomi.github.io/2020/04/17/default-methods.html
  public Object run(Object proxy, @SuppressWarnings("null") Method method, Object[] arguments) throws Throwable {
    return getMethodHandle(method).bindTo(proxy).invokeWithArguments(arguments);
  }

  private MethodHandle getMethodHandle(Method method) throws Exception {
    MethodHandle handle = methodHandleLookup.lookup(method);
    return handle;
  }

  /**
   * Strategies for {@link MethodHandle} lookup.
   */
  enum MethodHandleLookup {

    /**
     * Encapsulated {@link MethodHandle} lookup working on Java 9.
     */
    ENCAPSULATED {
      private final Method privateLookupIn = ReflectionUtil.findMethod(MethodHandles.class, "privateLookupIn", Class.class,
          MethodHandles.Lookup.class);
      @Override
      MethodHandle lookup(Method method) throws ReflectiveOperationException {
        if (privateLookupIn == null) {
          throw new IllegalStateException("Could not obtain MethodHandles.privateLookupIn");
        }
        return doLookup(method, getLookup(method.getDeclaringClass(), privateLookupIn));
      }

      @Override
      boolean isAvailable() {
        return privateLookupIn != null;
      }

      private MethodHandles.Lookup getLookup(Class<?> declaringClass, Method privateLookupIn) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
          return (MethodHandles.Lookup) privateLookupIn.invoke(MethodHandles.class, declaringClass, lookup);
        } catch (ReflectiveOperationException e) {
          return lookup;
        }
      }
    },

    /**
     * Open (via reflection construction of {@link MethodHandles.Lookup}) method handle lookup. Works with Java 8 and
     * with Java 9 permitting illegal access.
     */
    OPEN {
      private final Constructor<MethodHandles.Lookup> constructor = MethodHandleLookup.getLookupConstructor();
      @Override
      MethodHandle lookup(Method method) throws ReflectiveOperationException {
        if (!isAvailable()) {
          throw new IllegalStateException("Could not obtain MethodHandles.lookup constructor");
        }
        Constructor<MethodHandles.Lookup> constructor = this.constructor;
        return constructor.newInstance(method.getDeclaringClass()).unreflectSpecial(method, method.getDeclaringClass());
      }

      @Override
      boolean isAvailable() {
        return Optional.ofNullable(constructor).isPresent();
      }
    },

    /**
     * Fallback {@link MethodHandle} lookup using {@link MethodHandles#lookup() public lookup}.
     */
    FALLBACK {
      @Override
      MethodHandle lookup(Method method) throws ReflectiveOperationException {
        return doLookup(method, MethodHandles.lookup());
      }

      @Override
      boolean isAvailable() {
        return true;
      }
    };

    private static MethodHandle doLookup(Method method, MethodHandles.Lookup lookup) throws NoSuchMethodException, IllegalAccessException {
      MethodType methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
      if (Modifier.isStatic(method.getModifiers())) {
        return lookup.findStatic(method.getDeclaringClass(), method.getName(), methodType);
      }
      return lookup.findSpecial(method.getDeclaringClass(), method.getName(), methodType, method.getDeclaringClass());
    }

    /**
     * Lookup a {@link MethodHandle} given {@link Method} to look up.
     *
     * @param method must not be {@literal null}.
     * @return the method handle.
     * @throws ReflectiveOperationException
     */
    abstract MethodHandle lookup(Method method) throws ReflectiveOperationException;

    /**
     * @return {@literal true} if the lookup is available.
     */
    abstract boolean isAvailable();

    /**
     * Obtain the first available {@link MethodHandleLookup}.
     *
     * @return the {@link MethodHandleLookup}
     * @throws IllegalStateException if no {@link MethodHandleLookup} is available.
     */
    public static MethodHandleLookup getMethodHandleLookup() {
      for (MethodHandleLookup it : MethodHandleLookup.values()) {
        if (it.isAvailable()) {
          return it;
        }
      }
      throw new IllegalStateException("No MethodHandleLookup available");
    }

    private static Constructor<MethodHandles.Lookup> getLookupConstructor() {
      try {
        Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
        makeAccessible(constructor);
        return constructor;
      } catch (Exception ex) {
        // this is the signal that we are on Java 9 (encapsulated) and can't use the accessible constructor approach.
        if (ex.getClass().getName().equals("java.lang.reflect.InaccessibleObjectException")) {
          return null;
        }
        throw new IllegalStateException(ex);
      }
    }
  }

  public static void makeAccessible(Constructor<?> ctor) {
    if ((!Modifier.isPublic(ctor.getModifiers()) ||
        !Modifier.isPublic(ctor.getDeclaringClass().getModifiers())) && !ctor.isAccessible()) {
      ctor.setAccessible(true);
    }
  }
}
