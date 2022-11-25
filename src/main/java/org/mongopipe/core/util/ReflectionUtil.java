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

package org.mongopipe.core.util;

import org.mongopipe.core.exception.MongoPipeConfigException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Take a look also at https://github.com/spring-projects/spring-framework/blob/main/spring-core/src/main/java/org/springframework/util/ReflectionUtils.java
public class ReflectionUtil {

  public static Class getMethodGenericType(Method method) {
    // https://stackoverflow.com/questions/3403909/get-generic-type-of-class-at-runtime
    Class returnPojoClass = method.getReturnType();
    if (method.getGenericReturnType() instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType)method.getGenericReturnType();
      Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
      if (actualTypeArguments.length > 0 && actualTypeArguments[0] instanceof Class) {
        returnPojoClass = (Class) actualTypeArguments[0];
      }
    }
    return returnPojoClass;
  }

  public static Class getClassGenericType(Class clazz) {
    // https://stackoverflow.com/questions/3403909/get-generic-type-of-class-at-runtime
    Type[] actualTypeArguments = clazz.getTypeParameters();
    if (actualTypeArguments.length > 1) {
      throw new MongoPipeConfigException("Can only recognize 1 type as the default item handled by a @Store annotated class");
    }
    if (actualTypeArguments.length == 1) {
      Type type = actualTypeArguments[0];
      if (type instanceof ParameterizedType) {
        return (Class)((ParameterizedType) type).getRawType();
      }
    }
    return null;
  }

  private static void getClassFields(Class clazz, List<Field> accumulation) {
    accumulation.addAll(Arrays.asList(clazz.getDeclaredFields()));
    if (clazz.getSuperclass() != null) {
      getClassFields(clazz.getSuperclass(), accumulation);
    }
  }

  public static List<Field> getClassFields(Class clazz) {
    List<Field> fields = new ArrayList();
    getClassFields(clazz, fields);
    return fields;
  }

  private static void getClassMethodsIncludingInherited(Class clazz, List<Method> accumulation) {
    if (clazz.equals(Object.class)) {
      return;
    }
    accumulation.addAll(Arrays.asList(clazz.getDeclaredMethods()));
    if (clazz.getSuperclass() != null) {
      getClassMethodsIncludingInherited(clazz.getSuperclass(), accumulation);
    }
    Arrays.stream(clazz.getInterfaces()).forEach((interfaceClass) -> getClassMethodsIncludingInherited(interfaceClass, accumulation));
  }

  public static List<Method> getClassMethodsIncludingInherited(Class clazz) {
    List<Method> methods = new ArrayList();
    getClassMethodsIncludingInherited(clazz, methods);
    return methods;
  }

  public static List<Field> getFieldsAnnotatedWith(Class clazz, Class annotationClass) {
    return (List<Field>)getClassFields(clazz).stream()
        .filter((field) -> field.getAnnotation(annotationClass) != null)
        .collect(Collectors.toList());
  }

  public static List<Method> getMethodsAnnotatedWith(Class clazz, Class annotationClass) {
    return (List<Method>) getClassMethodsIncludingInherited(clazz).stream()
        .filter((method) -> method.getAnnotation(annotationClass) != null)
        .collect(Collectors.toList());
  }

  public static String getSimpleMethodId(Method method, Class actualClass) {
    //return actualClass.getSimpleName() + "." + method.getName(); // friendlier than hashCode().
    return method.toString();
  }

  private static boolean hasSameParams(Method method, Class<?>[] paramTypes) {
    return (paramTypes.length == method.getParameterCount() &&
        Arrays.equals(paramTypes, method.getParameterTypes()));
  }
  public static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
    Optional<Method> methodOptional = getClassMethodsIncludingInherited(clazz).stream()
        .filter((method) -> method.getName().equalsIgnoreCase(name) && hasSameParams(method, paramTypes))
        .findFirst();
    return methodOptional.orElse(null);
  }
}
