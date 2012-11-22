#include <cassert>
#include <cstdlib>
#include <list>
#include <set>
#include <string>
#include <utility>
#include <vector>

#include "clang/Driver/Arg.h"
#include "clang/Driver/ArgList.h"
#include "clang/Driver/DriverDiagnostic.h"
#include "clang/Driver/Option.h"
#include "clang/Driver/OptTable.h"

#include "clang/Frontend/DiagnosticOptions.h"
#include "clang/Frontend/TextDiagnosticPrinter.h"

#include "llvm/ADT/SmallVector.h"
#include "llvm/ADT/StringRef.h"
#include "llvm/ADT/IntrusiveRefCntPtr.h"
#include "llvm/ADT/OwningPtr.h"

#include "llvm/Support/CommandLine.h"
#include "llvm/Support/ManagedStatic.h"
#include "llvm/Support/MemoryBuffer.h"
#include "llvm/Support/Path.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/Support/system_error.h"

#include "Compiler.h"

// FIXME: Add parameter feature '-D macro=xxx'
static llvm::cl::opt<std::string>
InputFilename(llvm::cl::Positional, llvm::cl::Required,
              llvm::cl::desc("<input file>"));

static llvm::cl::list<std::string>
HeaderSearchDirs("I", llvm::cl::desc("Header search directory"), llvm::cl::Prefix);

static llvm::cl::list<std::string>
PreDefinedSymbols("D", llvm::cl::desc("Pre-define symbol"));

static llvm::cl::opt<std::string>
OutputFilename(llvm::cl::Required, "o", llvm::cl::desc("Override output filename"));

// split "xxx"     => "xxx" ""
// split "xxx=yyy" => "xxx" "yyy"
static void splitPreDefinedSymbol(const std::string& In,
                             std::string& Key, std::string& Value) {
  size_t FoundPos = In.find("=");
  if (FoundPos == std::string::npos) {
    Key = In;
    Value = "";
  } else {
    Key = In.substr(0, FoundPos);
    Value = In.substr(FoundPos+1, std::string::npos);
  }
}


int main(int argc, char** argv) {
  llvm::llvm_shutdown_obj _ShutdownObj;
  llvm::cl::ParseCommandLineOptions(argc, argv, "P-NDK Compile Tool");

  clang::TextDiagnosticPrinter* DiagClient =
      new clang::TextDiagnosticPrinter(llvm::errs(), clang::DiagnosticOptions());
  DiagClient->setPrefix(argv[0]);

  llvm::IntrusiveRefCntPtr<clang::DiagnosticIDs>
      DiagIDs(new clang::DiagnosticIDs());
  clang::DiagnosticsEngine Diags(DiagIDs, DiagClient, true);

  if (Diags.hasErrorOccurred())
    return 1;

  std::vector<std::string> IncludePaths;
  for(unsigned i = 0, e = HeaderSearchDirs.size(); i<e; ++i) {
    IncludePaths.push_back(HeaderSearchDirs[i]);
  }

  std::map<std::string, std::string> PreDefinedSymbolMap;
  for(unsigned i = 0, e = PreDefinedSymbols.size(); i<e; ++i) {
    std::string Key;
    std::string Value;
    splitPreDefinedSymbol(PreDefinedSymbols[i], Key, Value);
    PreDefinedSymbolMap.insert(
        std::pair<std::string, std::string>(Key,Value));
  }

  ndkpc::Compiler Compiler;
  Compiler.init(std::string(),
                std::string(),
                std::vector<std::string>(),
                llvm::StringRef(InputFilename).endswith(".cpp"));
  Compiler.setInputSource(InputFilename);
  Compiler.setIncludePaths(IncludePaths);
  Compiler.setOutput(OutputFilename.c_str());
  Compiler.setPreDefinedSymbol(PreDefinedSymbolMap);

  int ret = Compiler.compile();
  return ret;
}
