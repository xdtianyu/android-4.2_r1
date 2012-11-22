#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>

// --------------
// Register Usage
// --------------
// $0       zero
// $1       at
// $2-$3    function return value registers
// $4-$7    function argument registers
// $8-$15   temporary
// $16-$23  saved register
// $24-$25  temporary
// $26-$27  os kernel
// $28      global pointer
// $29      stack pointer
// $30      saved register
// $31      return addres reigster

// --------------------
// Instruction Encoding
// --------------------
// lui: 0011 1100 000t tttt iiii iiii iiii iiii
// ori: 0011 01ss ssst tttt iiii iiii iiii iiii
// jr:  0000 00ss sss0 0000 0000 0000 0000 1000
// nop:  0000 0000 0000 0000 0000 0000 0000 0000

uint32_t stub[] = {
  0x3c190000ul,
  0x37390000ul,
  0x03200008ul,
  0x00000000ul
};

int test() {
  printf("hello world!\n");
  return 5;
}

int main() {
  int (*f)() = (int (*)())stub;
  stub[0] |= (((uint32_t)(uintptr_t)test) >> 16) & 0xffff;
  stub[1] |= (((uint32_t)(uintptr_t)test)) & 0xffff;

  printf("return = %d\n", f());
  return EXIT_SUCCESS;
}
