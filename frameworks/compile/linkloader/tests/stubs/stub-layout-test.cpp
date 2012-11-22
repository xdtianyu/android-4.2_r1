/*
 * Copyright 2011, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <stdio.h>
#include <stdlib.h>

#include "StubLayout.h"

void function1() {
  printf("hello ");
}

void function2() {
  printf("world!\n");
}

int main() {
  StubLayout stubs;

  void (*func1)() = (void (*)())stubs.allocateStub((void *)&function1);
  void (*func2)() = (void (*)())stubs.allocateStub((void *)&function2);

  if (!func1) {
    fprintf(stderr, "ERROR: Unable to allocate stub for function1\n");
    exit(EXIT_FAILURE);
  }

  if (!func2) {
    fprintf(stderr, "ERROR: Unable to allocate stub for function2\n");
    exit(EXIT_FAILURE);
  }

  function1();
  function2();

  func1();
  func2();

  return EXIT_SUCCESS;
}
