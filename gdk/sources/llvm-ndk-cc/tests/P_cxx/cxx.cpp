#include "cxx.h"

void foo() {}
void foo(int a) {}
int bar;

namespace {
  void foo() {}
  void foo(double a) {}
  int bar;
}

namespace kerker {
  void foo() {}
  void foo(char* a) {}
  int bar;
}

extern "C" void c_interface() {
  kerker::foo();
}
