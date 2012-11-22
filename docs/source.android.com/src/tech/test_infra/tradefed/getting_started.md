<!--
   Copyright 2012 The Android Open Source Project

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->

# Getting Started

## Using the console

TF is based around an interactive console.  You can fire up the console by going to the
`tools/tradefederation/` directory and running

    $ ./tradefed.sh

You should end up at a `tf >` prompt.

The console is self-documenting.  Try entering "help"

    tf >help
    Enter 'q' or 'exit' to exit
    Enter 'kill' to attempt to forcibly exit, by shutting down adb

    Enter 'help list'  for help with 'list' commands
    [...]

As the help text suggests, the help menus are organized hierarchically

    tf >help list
    l(?:ist)? help:
        i[nvocations]  List all invocation threads
        d[evices]      List all detected or known devices
    [...]

The majority of commands have a convenient short form, which the help text displays.  The
`l(?:ist)?` is a regular expression.  As an example, here are the four equivalent ways to list
invocations:
* `list invocations`
* `list i`
* `l invocations`
* `l i`


## Running a config/command

This is documented by the `help run` command in the console.

As a reminder, a command is a config along with all of its command-line arguments.  A command *must*
begin with the name of the respective config.

As a quick example, you could run the calculator unit tests like so:

    $./tradefed.sh
    tf >run instrument --package com.android.calculator2.tests

As a shortcut, if you specify any arguments to `tradefed.sh`, it will attempt to execute them as if
they were typed on the commandline.  So the short version of the above would be

    $./tradefed.sh run instrument --package com.android.calculator2.tests

In both of these cases, the name of the config is "instrument", and
"--class com.android.calculator2.tests" is a command-line argument.  The command that is being run
is "instrument --class com.android.calculator2.tests".

TF can run both configs that are compiled in (such as the "instrument" config above), as well as
configs that exist as xml files on the local filesystem.  You can see a list of compiled-in configs
with the `list configs` console command.  Furthermore, you can investigate any config (compiled-in
or local) by passing the "--help" or "--help-all" command-line arguments.  The "--help" argument
will only show "important" arguments, and "--help-all" will show all arguments, regardless of
whether they've been marked as "important" or not.  To take the final step, you can tell TF to print
the contents of any config (compiled-in or local) with the `dump config <configname>` console
command.

### So, let's say you want to run the calculator instrumentation tests, but don't know where to start.

You could try something like this sequence of steps.  First, look for a config that looks like it
might do what you want:

    tf >list configs
    Use 'run command --help <configuration_name>' to get list of options for a configuration
    Use 'dump config <configuration_name>' to display the configuration's XML content.

    Available configurations include:
    [...]
      instrument: Runs a single Android instrumentation test on an existing device
    [...]

Now that you've found something reasonable-looking, see what options it takes.  The `list configs` output suggests trying `run command instrument --help`

    tf >run command --help instrument
    'instrument' configuration: Runs a single Android instrumentation test on an existing device

    Printing help for only the important options. To see help for all options, use the --help-all flag
    [...]
      'instrumentation' test options:
        -p, --package        The manifest package name of the Android test application to run.

As the message suggests, if you need more options, use the "--help-all" flag instead of "--help".  In this case, we've got all we need.  You could figure out the package by checking with `runtest`, or reading testdefs.xml directly.  We use `runtest -n` to simply show what would be run without actually running it:

    $runtest -n calculator
    adb root
    ONE_SHOT_MAKEFILE="packages/apps/Calculator/Android.mk" make -j4 -C "/srv/xsdg/master2" files
    adb sync
    adb  shell am instrument -w com.android.calculator2.tests/android.test.InstrumentationTestRunner

The argument to `am instrument` that comes before the slash is the manifest package.  `android.test.InstrumentationTestRunner` is the default runner, so no need to set it if that's the
right one.  Otherwise, using "--help-all" will tell you about the "--runner" argument, which you can
use to specify an alternate runner.  Ok, so at this point, we've got the following command, which
you'll recognize from above

    tf >run instrument --package com.android.calculator2.tests


## Interacting with a device

### Generic device behavior in TF

The running version of a command is called in `invocation`.  First and foremost, every invocation
requires a device before it can run.  In addition to physical Android devices, TF can run tests with
a mock device (by specifying the "-n" argument for the command), or with the Android emulator (by
specifying the "-e" argument").

The primary console command to figure out what devices are up to is `list devices`:

    $./tradefed.sh
    06-07 17:03:22 I/: Detected new device 016B756E03018007
    06-07 17:03:22 I/: Detected new device 1700614743c14397
    06-07 17:03:22 I/: Detected new device 3531C342606300EC
    tf >l d
    Serial            State      Product   Variant   Build   Battery
    016B756E03018007  Available  tuna      toro      MASTER  100
    1700614743c14397  Available  stingray  stingray  MASTER  100
    3531C342606300EC  Available  herring   crespo4g  MASTER  92

As far as the invocations are concerned, there are three device states: available, unavailable, and
allocated.  An `Available` device is ready to be allocated for an invocation.  An `Unavailable`
device is not ready for allocation, for any of a variety of reasons — TF may have deemed to the
device as unstable, the device may be critically low on storage, or something else may be amiss.
Finally, an `Allocated` device is a device that is already being used by an invocation.

When you start TF, all detected physical devices will be checked for responsiveness with a simple
shell command.  If the command completes successfully, the device will be listed as Available.  If
the command fails, the device state will be shown as Unavailable.  Thereafter, a device will typically bounce between the Available and Allocated states as invocation requirements dictate.

Finally, once invocations are already underway, you can see what's going on with the `list
invocations` command

    tf >run instrument --package com.android.calculator2.tests
    06-07 17:18:31 I/TestInvocation: Starting invocation for 'stub' on build '0' on device 1700614743c14397
    [...]
    tf >l d
    Serial            State      Product   Variant   Build   Battery
    1700614743c14397  Allocated  stingray  stingray  MASTER  100
    3531C342606300EC  Available  herring   crespo4g  JRN11   93
    016B756E03018007  Available  tuna      toro      MASTER  100

    tf >l i
    Command Id  Exec Time  Device            State
    1           0m:02      1700614743c14397  running stub on build 0


### Running invocations on specific devices

TF supports a number of filtering mechanisms for specifying which device or devices to use for a
particular invocation.  Since the filtering mechanisms are run before a command turns into an
invocation, you can find all of the filtering options in the help for any config:

tf >run instrument --help-all
[...]
  device_requirements options:
    -s, --serial         run this test on a specific device with given serial number(s).
    --exclude-serial     run this test on any device except those with this serial number(s).
    --product-type       run this test on device with this product type(s).  May also filter by variant using product:variant.
    --property           run this test on device with this property value. Expected format <propertyname>=<propertyvalue>.
    -e, --[no-]emulator  force this test to run on emulator. Default: false.
    -d, --[no-]device    force this test to run on a physical device, not an emulator. Default: false.
    --[no-]new-emulator  allocate a placeholder emulator. Should be used when config intends to launch an emulator Default: false.
    -n, --[no-]null-device
                         do not allocate a device for this test. Default: false.
    --min-battery        only run this test on a device whose battery level is at least the given amount. Scale: 0-100
    --max-battery        only run this test on a device whose battery level is strictly less than the given amount. Scale: 0-100
[...]

The built-in help should be pretty self-explanatory.  All of the filtering options excluding "-n",
"-e", and "-d" may be specified as many times as needed.  So, for instance, to run an invocation
using any Verizon Galaxy Nexus, you could do the following:

    tf >run instrument --package com.android.calculator2.tests --product-type tuna:toro

As another example, to run on a GSM device with a SIM, you could do the following:

    tf >run instrument --package com.android.calculator2.tests --property gsm.sim.state=READY

The filtering works by exclusion from the pool of Available devices, so the "--serial" option simply
excludes devices that aren't in the list of required serials, and --exclude-serial excludes devices
that *are* in its list.  As such, an argument like --exclude-serial XXX --serial XXX will simply
make the respective command un-runnable — it will never match any device, since all devices are
excluded.


## Logging

There are a few different aspects to logging in TF.  First and foremost, TF has a built-in logging
infrastructure that's based on DDMLib's Log class.  For the common case, where the log tag is just
the classname of the current class, you can use our CLog convenience shim.  In short, if you might
have originally done this:

    class ClassName {
    private static final LOG_TAG = "ClassName";
    [...]
    Log.v(LOG_TAG, "This is a simple verbose log message");
    Log.w(LOG_TAG, String.format("This warning message brought to you by the number %d", 17));

You can now accomplish the same thing with the shim like this:

    class ClassName {
    [...]
    CLog.v("This is a simple verbose log message");
    CLog.w("This warning message brought to you by the number %d", 17);

Each Invocation has its own ThreadGroup.  Any host-side logging that happens inside of that thread
group is associated with the Invocation, and will be reported as that invocation's "host_log" after
the Invocation completes.

Device logging is performed as part of TradeFed's device wrapper.  We keep a buffer of up to 20 MB
that captures log data as the device churns it out.  In particular, we are not limited by the size
of the on-device logcat buffer.

The next important piece is the ITestInvocationListener.  This is one of the components of an
Invocation that handles results reporting.  Each reporter has the option to implement the #testLog
method, which will be used to pass logfiles to that result reporter.  Among the files that are
passed by TF itself will be the aforementioned host_log, as well as the device logcat for the device
associated with the Invocation.

<!--
FIXME: discuss test result reporting, retrieving builds, doing things continuously, target prep and
FIXME: flashing builds, extending tradefed (like CTS).
-->

