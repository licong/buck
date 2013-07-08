/*
 * Copyright 2013-present Facebook, Inc.
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

package com.facebook.buck.cli;

import com.facebook.buck.event.BuckEvent;
import com.facebook.buck.model.BuildTarget;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public abstract class InstallEvent extends BuckEvent {
  private final BuildTarget buildTarget;

  protected InstallEvent(BuildTarget buildTarget) {
    this.buildTarget = Preconditions.checkNotNull(buildTarget);
  }

  public BuildTarget getBuildTarget() {
    return buildTarget;
  }

  @Override
  protected String getValueString() {
    return buildTarget.getFullyQualifiedName();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof InstallEvent)) {
      return false;
    }

    InstallEvent that = (InstallEvent)o;

    return Objects.equal(getClass(), o.getClass()) &&
        Objects.equal(getBuildTarget(), that.getBuildTarget());
  }

  @Override
  public int hashCode() {
    return getBuildTarget().hashCode();
  }

  public static Started started(BuildTarget buildTarget) {
    return new Started(buildTarget);
  }

  public static Finished finished(BuildTarget buildTarget, boolean success) {
    return new Finished(buildTarget, success);
  }

  public static class Started extends InstallEvent {
    protected Started(BuildTarget buildTarget) {
      super(buildTarget);
    }

    @Override
    protected String getEventName() {
      return "InstallStarted";
    }
  }

  public static class Finished extends InstallEvent {
    private final boolean success;

    protected Finished(BuildTarget buildTarget, boolean success) {
      super(buildTarget);
      this.success = success;
    }

    public boolean isSuccess() {
      return success;
    }

    @Override
    protected String getEventName() {
      return "InstallFinished";
    }

    @Override
    public boolean equals(Object o) {
      if (!super.equals(o)) {
        return false;
      }

      Finished that = (Finished) o;
      return isSuccess() == that.isSuccess();
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(getBuildTarget(), isSuccess());
    }
  }
}