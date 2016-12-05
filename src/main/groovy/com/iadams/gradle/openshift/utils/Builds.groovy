/*
 * Gradle Openshift Plugin
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Iain Adams
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.iadams.gradle.openshift.utils

class Builds {

  public static class Status {
    public static final String COMPLETE = "Complete";
    public static final String FAIL = "Fail";
    public static final String ERROR = "Error";
    public static final String CANCELLED = "Cancelled";
  }

  public static boolean isCompleted(String status) {
    return Objects.equals(Status.COMPLETE, status);
  }

  public static boolean isCancelled(String status) {
    return Objects.equals(Status.CANCELLED, status);
  }

  public static boolean isFailed(String status) {
    if (status != null) {
      return status.startsWith(Status.FAIL) || status.startsWith(Status.ERROR);
    }
    return false;
  }

  public static boolean isFinished(String status) {
    return isCompleted(status) || isFailed(status) || isCancelled(status);
  }
}
