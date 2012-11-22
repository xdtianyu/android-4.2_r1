#!/bin/bash
# See usage() below for the description.

function usage() {
  cat <<EOF
# This script copies the .jar files that each plugin depends on into the plugins libs folder.
# By default, on Mac & Linux, this script creates symlinks from the libs folder to the jar file.
# Since Windows does not support symlinks, the jar files are copied.
#
# Options:
# -f : to copy files rather than creating symlinks on the Mac/Linux platforms.
# -d : print make dependencies instead of running make; doesn't copy files.
# -c : copy files expected after make dependencies (reported by -d) have been built.
#
# The purpose of -d/-c is to include the workflow in a make file:
# - the make rule should depend on \$(shell create_all_symlinks -d)
# - the rule body should perform   \$(shell create_all_symlinks -c [-f])
EOF
}

# CD to the top android directory
PROG_DIR=`dirname "$0"`
cd "${PROG_DIR}/../../../"

HOST=`uname`
USE_COPY=""        # force copy dependent jar files rather than creating symlinks
ONLY_SHOW_DEPS=""  # only report make dependencies but don't build them nor copy.
ONLY_COPY_DEPS=""  # only copy dependencies built by make; uses -f as needed.

function die() {
  echo "Error: $*" >/dev/stderr
  exit 1
}

function warn() {
  # Only print something if not in show-deps mode
  if [[ -z $ONLY_SHOW_DEPS ]]; then
    echo "$*"
  fi
}

## parse arguments
while [ $# -gt 0 ]; do
  case "$1" in
    "-f" )
      USE_COPY="1"
      ;;
    "-d" )
      ONLY_SHOW_DEPS="1"
      ;;
    "-c" )
      ONLY_COPY_DEPS="1"
      ;;
    * )
      usage
      exit 2
  esac
  shift
done

warn "## Running $0"

if [[ "${HOST:0:6}" == "CYGWIN" || "$USE_MINGW" == "1" ]]; then
  # This is either Cygwin or Linux/Mingw cross-compiling to Windows.
  PLATFORM="windows-x86"
  if [[ "${HOST:0:6}" == "CYGWIN" ]]; then
    # We can't use symlinks under Cygwin
    USE_COPY="1"
  fi
elif [[ "$HOST" == "Linux" ]]; then
  PLATFORM="linux-x86"
elif [[ "$HOST" == "Darwin" ]]; then
  PLATFORM="darwin-x86"
else
  die "Unsupported platform ($HOST). Aborting."
fi

if [[ "$USE_COPY" == "1" ]]; then
  function cpfile { # $1=source $2=dest
    cp -fv $1 $2/
  }

  function cpdir() { # $1=source $2=dest
    rsync -avW --delete-after $1 $2
  }
else
  # computes the "reverse" path, e.g. "a/b/c" => "../../.."
  function back() {
    echo $1 | sed 's@[^/]*@..@g'
  }

  function cpfile { # $1=source $2=dest
    ln -svf `back $2`/$1 $2/
  }

  function cpdir() { # $1=source $2=dest
    ln -svf `back $2`/$1 $2
  }
fi

DEST="sdk/eclipse/scripts"

set -e # fail early

LIBS=""
CP_FILES=""


### BASE ###

BASE_PLUGIN_DEST="sdk/eclipse/plugins/com.android.ide.eclipse.base/libs"
BASE_PLUGIN_LIBS="common sdkstats sdklib dvlib layoutlib_api sdk_common"
BASE_PLUGIN_PREBUILTS="\
    prebuilts/misc/common/kxml2/kxml2-2.3.0.jar \
    prebuilts/tools/common/commons-compress/commons-compress-1.0.jar \
    prebuilts/tools/common/guava-tools/guava-13.0.1.jar \
    prebuilts/tools/common/http-client/commons-logging-1.1.1.jar \
    prebuilts/tools/common/http-client/commons-codec-1.4.jar \
    prebuilts/tools/common/http-client/httpclient-4.1.1.jar \
    prebuilts/tools/common/http-client/httpcore-4.1.jar \
    prebuilts/tools/common/http-client/httpmime-4.1.1.jar"

LIBS="$LIBS $BASE_PLUGIN_LIBS"
CP_FILES="$CP_FILES @:$BASE_PLUGIN_DEST $BASE_PLUGIN_LIBS $BASE_PLUGIN_PREBUILTS"


### ADT ###

ADT_DEST="sdk/eclipse/plugins/com.android.ide.eclipse.adt/libs"
ADT_LIBS="ant-glob assetstudio lint_api lint_checks ninepatch propertysheet rule_api sdkuilib swtmenubar manifmerger"
ADT_PREBUILTS="\
    prebuilts/tools/common/freemarker/freemarker-2.3.19.jar \
    prebuilts/tools/common/asm-tools/asm-4.0.jar \
    prebuilts/tools/common/asm-tools/asm-tree-4.0.jar \
    prebuilts/tools/common/asm-tools/asm-analysis-4.0.jar \
    prebuilts/tools/common/lombok-ast/lombok-ast-0.2.jar"

LIBS="$LIBS $ADT_LIBS"
CP_FILES="$CP_FILES @:$ADT_DEST $ADT_LIBS $ADT_PREBUILTS"


### DDMS ###

DDMS_DEST="sdk/eclipse/plugins/com.android.ide.eclipse.ddms/libs"
DDMS_LIBS="ddmlib ddmuilib swtmenubar uiautomatorviewer"

DDMS_PREBUILTS="\
    prebuilts/tools/common/jfreechart/jcommon-1.0.12.jar \
    prebuilts/tools/common/jfreechart/jfreechart-1.0.9.jar \
    prebuilts/tools/common/jfreechart/jfreechart-1.0.9-swt.jar"

LIBS="$LIBS $DDMS_LIBS"
CP_FILES="$CP_FILES @:$DDMS_DEST $DDMS_LIBS $DDMS_PREBUILTS"


### TEST ###

TEST_DEST="sdk/eclipse/plugins/com.android.ide.eclipse.tests"
TEST_LIBS="easymock"
TEST_PREBUILTS="prebuilts/misc/common/kxml2/kxml2-2.3.0.jar"

LIBS="$LIBS $TEST_LIBS"
CP_FILES="$CP_FILES @:$TEST_DEST $TEST_LIBS $TEST_PREBUILTS"


### BRIDGE ###

if [[ $PLATFORM != "windows-x86" ]]; then
  # We can't build enough of the platform on Cygwin to create layoutlib
  BRIDGE_LIBS="layoutlib ninepatch"

  LIBS="$LIBS $BRIDGE_LIBS"
fi


### HIERARCHYVIEWER ###

HV_DEST="sdk/eclipse/plugins/com.android.ide.eclipse.hierarchyviewer/libs"
HV_LIBS="hierarchyviewerlib swtmenubar"

LIBS="$LIBS $HV_LIBS"
CP_FILES="$CP_FILES @:$HV_DEST $HV_LIBS"


### TRACEVIEW ###

TV_DEST="sdk/eclipse/plugins/com.android.ide.eclipse.traceview/libs"
TV_LIBS="traceview"

LIBS="$LIBS $TV_LIBS"
CP_FILES="$CP_FILES @:$TV_DEST $TV_LIBS"


### MONITOR ###

MONITOR_DEST="sdk/eclipse/plugins/com.android.ide.eclipse.monitor/libs"
MONITOR_LIBS="sdkuilib"

LIBS="$LIBS $MONITOR_LIBS"
CP_FILES="$CP_FILES @:$MONITOR_DEST $MONITOR_LIBS"


### SDKMANAGER ###

SDKMAN_LIBS="swtmenubar"

LIBS="$LIBS $SDKMAN_LIBS"


### GL DEBUGGER ###

if [[ $PLATFORM != "windows-x86" ]]; then
  # liblzf doesn't build under cygwin. If necessary, this should be fixed first.
  
  GLD_DEST="sdk/eclipse/plugins/com.android.ide.eclipse.gldebugger/libs"
  GLD_LIBS="host-libprotobuf-java-2.3.0-lite liblzf"

  LIBS="$LIBS $GLD_LIBS"
  CP_FILES="$CP_FILES @:$GLD_DEST $GLD_LIBS"
fi

# In the mode to only echo dependencies, output them and we're done
if [[ -n $ONLY_SHOW_DEPS ]]; then
  echo $LIBS
  exit 0
fi

if [[ -z $ONLY_COPY_DEPS ]]; then
  # Make sure we have lunch sdk-<something>
  if [[ ! "$TARGET_PRODUCT" ]]; then
    warn "## TARGET_PRODUCT is not set, running build/envsetup.sh"
    . build/envsetup.sh
    warn "## lunch sdk-eng"
    lunch sdk-eng
  fi

  # Run make on all libs

  J="4"
  [[ $(uname) == "Darwin" ]] && J=$(sysctl hw.ncpu | cut -d : -f 2 | tr -d ' ')
  [[ $(uname) == "Linux"  ]] && J=$(cat /proc/cpuinfo | grep processor | wc -l)

  warn "## Building libs: make -j$J $LIBS"
  make -j${J} $LIBS
fi

# Copy resulting files
DEST=""
for SRC in $CP_FILES; do
  if [[ "${SRC:0:2}" == "@:" ]]; then
    DEST="${SRC:2}"
    mkdir -vp "$DEST"
    continue
  fi
  if [[ ! -f "$SRC" ]]; then
    ORIG_SRC="$SRC"
    SRC="out/host/$PLATFORM/framework/$SRC.jar"
  fi
  if [[ -f "$SRC" ]]; then
    if [[ ! -d "$DEST" ]]; then
      die "Invalid cp_file dest directory: $DEST"
    fi

    cpfile "$SRC" "$DEST"
  else
    die "## Unknown source '$ORIG_SRC' to copy in '$DEST'"
  fi
done

# OS-specific post operations

if [ "${HOST:0:6}" == "CYGWIN" ]; then
  chmod -v a+rx "$ADT_DEST"/*.jar
fi

echo "### $0 done"
