#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>


// Note: The first instruction stands for ldr, which loads the data from
// memory to the specified register.  Notice that due to the pipeline design,
// when ldr is executed, the program will be advanced by 8.  So, to get our
// address we should substract it by 4.

uint32_t stub[] = {
  0xe51ff004ul, // ldr pc, [pc, #-4]
  0x00000000ul  // address
};

int test() {
  printf("hello world!\n");
  return 5;
}

int main() {
  int (*f)() = (int (*)())stub;
  stub[1] = (uint32_t)(uintptr_t)test;

  printf("return = %d\n", f());
  return EXIT_SUCCESS;
}
