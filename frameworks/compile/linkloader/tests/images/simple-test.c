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

#include <stdlib.h>
#include <stdio.h>
#include <time.h>

int main() {
  srand(time(NULL));

  unsigned int ans = rand() % 100;
  unsigned int user = 100;
  unsigned int left = 0;
  unsigned int right = 99;

  printf("Hello, droid!  Let's play a number guessing game!\n");

  while (user != ans) {
    printf("Please input a number [%d-%d]:\n", left, right);

    if (scanf("%u", &user) != 1) {
      break;
    }

    if (user < left || user > right) {
      /* Out of range, ignore this answer. */
      continue;
    } else if (user == ans) {
      printf("You got it!\n");
      break;
    } else if (user < ans) {
      left = user;
    } else {
      right = user;
    }
  }

  return 0;
}
