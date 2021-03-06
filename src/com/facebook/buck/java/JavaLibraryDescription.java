/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.java;

import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.rules.BuildRule;
import com.facebook.buck.rules.BuildRuleParams;
import com.facebook.buck.rules.BuildRuleType;
import com.facebook.buck.rules.ConstructorArg;
import com.facebook.buck.rules.Description;
import com.facebook.buck.rules.SourcePath;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

import java.nio.file.Path;

public class JavaLibraryDescription implements Description<JavaLibraryDescription.Arg> {

  public static final BuildRuleType TYPE = new BuildRuleType("java_library");
  public static final String ANNOTATION_PROCESSORS = "annotation_processors";
  private final JavaCompilerEnvironment javacEnv;

  public JavaLibraryDescription(JavaCompilerEnvironment javacEnv) {
    this.javacEnv = Preconditions.checkNotNull(javacEnv);
  }

  @Override
  public BuildRuleType getBuildRuleType() {
    return TYPE;
  }

  @Override
  public Arg createUnpopulatedConstructorArg() {
    return new Arg();
  }

  @Override
  public <A extends Arg> JavaLibrary createBuildable(BuildRuleParams params, A args) {
    JavacOptions.Builder javacOptions = JavaLibraryDescription.getJavacOptions(args, javacEnv);

    AnnotationProcessingParams annotationParams =
        args.buildAnnotationProcessingParams(params.getBuildTarget());
    javacOptions.setAnnotationProcessingData(annotationParams);

    return new DefaultJavaLibrary(
        params,
        args.srcs.get(),
        args.resources.get(),
        args.proguardConfig,
        args.postprocessClassesCommands.get(),
        args.exportedDeps.get(),
        javacOptions.build());
  }

  public static JavacOptions.Builder getJavacOptions(Arg args, JavaCompilerEnvironment javacEnv) {
    JavacOptions.Builder javacOptions = JavacOptions.builder();

    String sourceLevel = args.source.or(javacEnv.getSourceLevel());
    String targetLevel = args.target.or(javacEnv.getTargetLevel());

    JavaCompilerEnvironment javacEnvToUse = new JavaCompilerEnvironment(
        javacEnv.getJavacPath(),
        javacEnv.getJavacVersion(),
        sourceLevel,
        targetLevel);

    javacOptions.setJavaCompilerEnviornment(javacEnvToUse);

    return javacOptions;
  }

  public static class Arg implements ConstructorArg {
    public Optional<ImmutableSortedSet<SourcePath>> srcs;
    public Optional<ImmutableSortedSet<SourcePath>> resources;
    public Optional<String> source;
    public Optional<String> target;
    public Optional<Path> proguardConfig;
    public Optional<ImmutableSortedSet<BuildRule>> annotationProcessorDeps;
    public Optional<ImmutableList<String>> annotationProcessorParams;
    public Optional<ImmutableSet<String>> annotationProcessors;
    public Optional<Boolean> annotationProcessorOnly;
    public Optional<ImmutableList<String>> postprocessClassesCommands;

    public Optional<ImmutableSortedSet<BuildRule>> exportedDeps;
    public Optional<ImmutableSortedSet<BuildRule>> deps;

    public AnnotationProcessingParams buildAnnotationProcessingParams(BuildTarget owner) {
      ImmutableSet<String> annotationProcessors =
          this.annotationProcessors.or(ImmutableSet.<String>of());

      if (annotationProcessors.isEmpty()) {
        return AnnotationProcessingParams.EMPTY;
      }

      AnnotationProcessingParams.Builder builder = new AnnotationProcessingParams.Builder();
      builder.setOwnerTarget(owner);
      builder.addAllProcessors(annotationProcessors);
      ImmutableSortedSet<BuildRule> processorDeps =
          annotationProcessorDeps.or(ImmutableSortedSet.<BuildRule>of());
      for (BuildRule processorDep : processorDeps) {
        builder.addProcessorBuildTarget(processorDep);
      }
      for (String processorParam : annotationProcessorParams.or(ImmutableList.<String>of())) {
        builder.addParameter(processorParam);
      }
      builder.setProcessOnly(annotationProcessorOnly.or(Boolean.FALSE));

      return builder.build();
    }
  }
}
