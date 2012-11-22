#!/bin/bash

GdkRoot=""
AndroidRoot=""

function echoHelp {
  echo "Build tools into <GDK>/toolchins/llvm/prebuilt/"
  echo "$0"
  echo "  --gdk-root=         GDK root absolute path"
  echo "  --android-root=     Android source tree root absolute path"
  echo
}

# Parse --Name=Value
function parseArgs {
  Name=$(echo $1 | awk -F '--' '{print $2}' | awk -F '=' '{print $1}')
  Value=$(echo $1 | awk -F '--' '{print $2}' | awk -F '=' '{print $2}')
  if [ $Name = "gdk-root" ]; then
    GdkRoot=$Value
  elif [ $Name = "android-root" ]; then
    AndroidRoot=$Value
  fi
}

function assertVar {
  if [ -z $2 ]; then
    echo "You must define --$1 in command line."
    exit 1
  fi
}

function main {
  assertVar gdk-root $1
  assertVar android-root $2

  if [ -z `echo $GdkRoot | awk -F $AndroidRoot '{print $2}'` ]; then
    echo "Please put <GDK> under <AndroidSrcRoot>."
    echo "Since building <GDK>/sources/llvm-ndk-cc/ needs Android source building system."
    echo
    exit 1
  fi

  cd $1/sources/llvm-ndk-cc/ && \
    . $2/build/envsetup.sh && \
    mm && \
    mkdir -p $1/toolchains/llvm/prebuilt/bin && \
    cp -f -p $2/out/host/linux-x86/bin/llvm-ndk-cc $1/toolchains/llvm/prebuilt/bin/ && \
    cp -f -p $2/out/host/linux-x86/bin/llvm-ndk-link $1/toolchains/llvm/prebuilt/bin/

  if [ $? -eq 0 ]; then
    echo
    echo "===================================================================="
    echo "Congradulation! You can check $1/toolchains/llvm/prebuilt/ now."
    echo "===================================================================="
    echo
  fi
}


# Entry
if [ $# -eq 0 ]; then
  echoHelp $0
  exit 1
fi

while [ $# -gt 0 ]; do
  parseArgs $1
  shift 1
done

main $GdkRoot $AndroidRoot
