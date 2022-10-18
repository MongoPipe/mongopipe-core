/*
 * Copyright (c) 2022 Cristian Donoiu, Ionut Sergiu Peschir
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.mongopipe.core.runner;

import org.mongopipe.core.PipelineRunner;
import org.mongopipe.core.PipelineStore;
import org.mongopipe.core.Pipelines;
import org.mongopipe.core.annotation.Pipeline;
import org.mongopipe.core.annotation.PipelineRepository;
import org.mongopipe.core.config.PipelineRunConfig;
import org.mongopipe.core.exception.MongoPipeConfigException;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.*;

public class PipelineRepositoriesLoader {
  private static final Logger LOG = LoggerFactory.getLogger(PipelineRepositoriesLoader.class);
  private final static Map<Class, Object> REPOSITORIES = new HashMap();

  public static Set<Method> getPipelineAnnotatedMethods(PipelineRunConfig pipelineRunConfig){

    Collection<URL> urls;
    if (pipelineRunConfig.getRepositoriesScanPackage() == null) {
      //urls = ClasspathHelper.forJavaClassPath();
      throw new MongoPipeConfigException("Need to provide scan package for pipeline repositories");
    } else {
      urls = ClasspathHelper.forPackage(pipelineRunConfig.getRepositoriesScanPackage());
    }
    // Works with both 0.9.12 and 0.10.2
    Reflections reflections = new Reflections(new ConfigurationBuilder()
            .setUrls(urls)
            .addScanners(new MethodAnnotationsScanner(), new TypeAnnotationsScanner())
    );
    //reflections.getTypesAnnotatedWith(PipelineRepository.class);
    Set<Method> pipelineRunMethods = reflections.getMethodsAnnotatedWith(Pipeline.class);
    if (pipelineRunMethods.size() == 0) {
      throw new MongoPipeConfigException("No pipeline annotated methods found in packages under \""
          + pipelineRunConfig.getRepositoriesScanPackage() + "\". Check that the configuration");
    }
    return pipelineRunMethods;
  }

  public static void createPipelineRepositoriesProxies(PipelineRunConfig pipelineRunConfig) {
    Set<Class> pipelineRepositories = new HashSet<>();

    for (Method method : getPipelineAnnotatedMethods(pipelineRunConfig)) {
      if (!pipelineRepositories.contains(method.getDeclaringClass())) {
        pipelineRepositories.add(method.getDeclaringClass());
      }
    }

    for (Class pipelineRepositoryClass : pipelineRepositories) {
      String configurationId = Pipelines.DEFAULT_CONFIG_ID;

      if (pipelineRepositoryClass.isAnnotationPresent(PipelineRepository.class)) {
        configurationId = ((PipelineRepository)pipelineRepositoryClass.getAnnotation(PipelineRepository.class)).configurationId();
        //LOG.info("{}", configurationId);
      }
      PipelineRunner pipelineRunner = Pipelines.getRunner(configurationId);
      PipelineStore pipelineStore = Pipelines.getStore(configurationId);

      PipelineRepositoryInvocationHandler invocationHandler = new PipelineRepositoryInvocationHandler(pipelineStore, pipelineRunner);
      // https://docs.oracle.com/javase/8/docs/technotes/guides/reflection/proxy.html
      REPOSITORIES.put(pipelineRepositoryClass, Proxy.newProxyInstance(
          pipelineRepositoryClass.getClassLoader(),
          new Class[]{pipelineRepositoryClass},
          invocationHandler));
    }
  }


  public static <T> T getRepository(Class<T> pipelineRepositoryInterface) {
    Object repository = REPOSITORIES.get(pipelineRepositoryInterface);
    if (repository == null) {
      // Load it now, test if it has annotated methods or class annotation
      String configId = Pipelines.DEFAULT_CONFIG_ID;
      if (pipelineRepositoryInterface.isAnnotationPresent(PipelineRepository.class)) {
        configId = pipelineRepositoryInterface.getAnnotation(PipelineRepository.class).configurationId();
      }
      PipelineRunConfig pipelineRunConfig = Pipelines.getConfig(configId);

      //getPipelineAnnotatedMethods(pipelineRunConfig);
      createPipelineRepositoriesProxies(pipelineRunConfig);
    }

    repository = REPOSITORIES.get(pipelineRepositoryInterface);
    if (repository == null) {
      throw new MongoPipeConfigException("Repository was not configured, probably because interface is missing @Pipeline annotated methods");
    }
    return (T)repository;
  }
}
