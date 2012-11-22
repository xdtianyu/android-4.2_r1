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

#include<stdio.h>

static char const *const test_str[] = {
  "string 1",
  "string 2",
  "string 3",
  "long long long long long long string"
};

static size_t test_str_count = sizeof(test_str) / sizeof(char const *const);

int main(){
  int i;
  printf("test_str: %p\n", &test_str);
  for (i = 0; i < test_str_count; ++i) {
    printf("%p\n", test_str[i]);
    printf("%s\n", test_str[i]);
  }

  return 0;
}
