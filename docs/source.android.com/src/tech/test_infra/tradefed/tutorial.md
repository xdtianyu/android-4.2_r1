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

# Tutorial

This tutorial guides you through the construction of a "hello world" Trade Federation test
configuration, and gives you a hands-on introduction to the Trade Federation framework.  Starting
from the Tf development environment, it guides you through the process of creating a simple Trade
Federation config and gradually adding more features to it.

The tutorial presents the TF test development process as a set of exercises, each consisting of
several steps.  The exercises demonstrate how to gradually build and refine your configuration, and
provide all the sample code you need to complete the test configuration.

When you are finished with the tutorial, you will have created a functioning TF configuration and
will have learned many of the most important concepts in the TF framework.


## Set up TradeFederation development environment

See (FIXME: link) for how to setup the development environment. The rest of this tutorial assumes you have a shell open that has been initialized to the TradeFederation environment. 

For simplicity, this tutorial will illustrate adding a configuration and its classes to the TradeFederation framework core library. Later tutorials/documentation will show how to create your own library that extends TradeFederation.


## Creating a test class

Lets create a hello world test that just dumps a message to stdout. A TradeFederation test must
implement the (FIXME: link) IRemoteTest interface.

Here's an implementation for the HelloWorldTest:

    package com.android.tradefed.example;

    import com.android.tradefed.device.DeviceNotAvailableException;
    import com.android.tradefed.result.ITestInvocationListener;
    import com.android.tradefed.testtype.IRemoteTest;


    public class HelloWorldTest implements IRemoteTest {
        @Override
        public void run(ITestInvocationListener listener) throws DeviceNotAvailableException {
            System.out.println("Hello, TF World!");
        }
    }

FIXME: prod-tests
Save this sample code to
`<git home>/tools/tradefederation/prod-tests/src/com/android/tradefed/example/HelloWorldTest.java`
and rebuild tradefed from your shell:

    m -j6

If the build does not succeed, please consult the (FIXME: link)Development Environment page to
ensure you did not miss any steps.


## Creating a configuration

Trade Federation tests are defined in a "Configuration". A Configuration is an XML file that
instructs tradefed which test (or set of tests) to run.

Lets create a new Configuration for our HelloWorldTest.

    <configuration description="Runs the hello world test">
        <test class="com.android.tradefed.example.HelloWorldTest" />
    </configuration>

TF will parse the Configuration XML file, load the specified class using reflection, instantiate it,
cast it to a IRemoteTest, and call its 'run' method.

Note that we've specified the full class name of the HelloWorldTest. Save this data to a
`helloworld.xml` file anywhere on your local filesystem (eg `/tmp/helloworld.xml`).


## Running the configuration

From your shell, launch the tradefed console

    $ ./tradefed.sh

Ensure a device is connected to the host machine that is visible to tradefed

    tf> list devices

Configurations can be run using the `run <config>` console command.  Try this now

FIXME: redo this

    tf> run /tmp/helloworld.xml
    05-12 13:19:36 I/TestInvocation: Starting invocation for target stub on build 0 on device 30315E38655500EC
    Hello, TF World!

You should see "Hello, TF World!" outputted on the terminal.


## Adding the configuration to the classpath
FIXME: prod-tests
For convenience of deployment, you can also bundle configuration files into the TradeFederation jars
themselves. Tradefed will automatically recognize all configurations placed in 'config' folders on
the classpath.

Lets illustrate this now by moving the helloworld.xml into the tradefed core library.

Move the `helloworld.xml` file into 
`<git root>/tools/tradefederation/prod-tests/res/config/example/helloworld.xml`.

Rebuild tradefed, and restart the tradefed console. 

Ask tradefed to display the list of configurations on the classpath:

    tf> list configs
    […]
    example/helloworld: Runs the hello world test

You can now run the helloworld config via the following command

    tf >run example/helloworld
    05-12 13:21:21 I/TestInvocation: Starting invocation for target stub on build 0 on device 30315E38655500EC
    Hello, TF World!


## Interacting with a device

So far our hello world test isn't doing anything interesting. Tradefed is intended to run tests using Android devices, so lets add an Android device to the test.

Tests can get a reference to an Android device by implementing the IDeviceTest interface. 

Here's a sample implementation of what this looks like:

    public class HelloWorldTest implements IRemoteTest, IDeviceTest {
        private ITestDevice mDevice;
        @Override
        public void setDevice(ITestDevice device) {
            mDevice = device;
        }

        @Override
        public ITestDevice getDevice() {
            return mDevice;
        }
    …
    }

The TradeFederation framework will inject the ITestDevice reference into your test via the
IDeviceTest#setDevice method, before the IRemoteTest#run method is called.

Lets add an additional print message to the HelloWorldTest displaying the serial number of the
device.

    @Override
    public void run(ITestInvocationListener listener) throws DeviceNotAvailableException {
        System.out.println("Hello, TF World! I have a device " + getDevice().getSerialNumber());
    }

Now rebuild tradefed, and do (FIXME: update)

    $ tradefed.sh
    tf> list devices
    Available devices:   [30315E38655500EC]
    …

Take note of the serial number listed in Available devices above. That is the device that should be allocated to HelloWorld.

    tf >run example/helloworld
    05-12 13:26:18 I/TestInvocation: Starting invocation for target stub on build 0 on device 30315E38655500EC
    Hello world, TF! I have a device 30315E38655500EC

You should see the new print message displaying the serial number of the device.


## Sending test results

IRemoteTests report results by calling methods on the ITestInvocationListener instance provided to
their `#run` method.

The TradeFederation framework is responsible for reporting the start and end of an Invocation (via
the ITestInvocationListener#invocationStarted and ITestInvocationListener#invocationEnded methods
respectively).

A `test run` is a logical collection of tests. To report test results, IRemoteTests are responsible
for reporting the start of a test run, the start and end of each test, and the end of the test run.

Here's what the HelloWorldTest implementation looks like with a single failed test result.

    @SuppressWarnings("unchecked")
    @Override
    public void run(ITestInvocationListener listener) throws DeviceNotAvailableException {
        System.out.println("Hello, TF World! I have a device " + getDevice().getSerialNumber());

        TestIdentifier testId = new TestIdentifier("com.example.MyTestClassName", "sampleTest");
        listener.testRunStarted("helloworldrun", 1);
        listener.testStarted(testId);
        listener.testFailed(TestFailure.FAILURE, testId, "oh noes, test failed");
        listener.testEnded(testId, Collections.EMPTY_MAP);
        listener.testRunEnded(0, Collections.EMPTY_MAP);
    }

Note that TradeFederation also includes several IRemoteTest implementations that you can reuse
instead of writing your own from scratch. (such as InstrumentationTest, which can run an Android
application's tests remotely on an Android device, parse the results, and forward them to the
ITestInvocationListener). See the Test Types documentation for more details.


## Storing test results

By default, a TradeFederation configuration will use the TextResultReporter as the test listener
implementation for the configuration.  TextResultReporter will dump the results of an invocation to
stdout. To illustrate, try running the hello-world config from previous section now:

    $ ./tradefed.sh
    tf >run example/helloworld
    05-16 20:03:15 I/TestInvocation: Starting invocation for target stub on build 0 on device 30315E38655500EC
    Hello world, TF! I have a device 30315E38655500EC
    05-16 20:03:15 I/InvocationToJUnitResultForwarder: run helloworldrun started: 1 tests
    Test FAILURE: com.example.MyTestClassName#sampleTest 
     stack: oh noes, test failed 
    05-16 20:03:15 I/InvocationToJUnitResultForwarder: run ended 0 ms

If you want to store the results of an invocation elsewhere, say to a file, you would need to
specify a custom "result_reporter" in your configuration, that specifies the custom
ITestInvocationListener class you want to use.

The TradeFederation framework includes a result_reporter (XmlResultReporter)  that will write test
results to an XML file, in a format similar to the ant JUnit XML writer. 

Lets specify the result_reporter in the configuration now. Edit the
`tools/tradefederation/res/config/example/helloworld.xml` like this:

    <configuration description="Runs the hello world test">
        <test class="com.android.tradefed.example.HelloWorldTest" />
        <result_reporter class="com.android.tradefed.result.XmlResultReporter" />
    </configuration> 

Now rebuild tradefed and re-run the hello world sample:
FIXME: paths

    tf >run example/helloworld
    05-16 21:07:07 I/TestInvocation: Starting invocation for target stub on build 0 on device 30315E38655500EC
    Hello world, TF! I have a device 30315E38655500EC
    05-16 21:07:07 I/XmlResultReporter: Saved device_logcat log to /var/folders/++/++2Pz+++6+0++4RjPqRgNE+-4zk/-Tmp-/0/inv_2991649128735283633/device_logcat_6999997036887173857.txt
    05-16 21:07:07 I/XmlResultReporter: Saved host_log log to /var/folders/++/++2Pz+++6+0++4RjPqRgNE+-4zk/-Tmp-/0/inv_2991649128735283633/host_log_6307746032218561704.txt
    05-16 21:07:07 I/XmlResultReporter: XML test result file generated at /var/folders/++/++2Pz+++6+0++4RjPqRgNE+-4zk/-Tmp-/0/inv_2991649128735283633/test_result_536358148261684076.xml. Total tests 1, Failed 1, Error 0

Notice the log message stating an XML file has been generated. The generated file should look like this:

    <?xml version='1.0' encoding='UTF-8' ?>
    <testsuite name="stub" tests="1" failures="1" errors="0" time="9" timestamp="2011-05-17T04:07:07" hostname="localhost">
      <properties />
      <testcase name="sampleTest" classname="com.example.MyTestClassName" time="0">
        <failure>oh noes, test failed
        </failure>
      </testcase>
    </testsuite>

Note that you can write your own custom result_reporter. It just needs to implement the
ITestInvocationListener interface. 

Also note that Tradefed supports multiple result_reporters, meaning that you can send test results
to multiple independent destinations. Just specify multiple <result_reporter> tags in your config to
do this.


## Logging

TradeFederation includes two logging facilities:

1. ability to capture logs from the device (aka device logcat)
2. ability to record logs from the TradeFederation framework running on the host machine (aka the
    host log)

Lets focus on 2 for now. Trade Federation's host logs are reported using the CLog wrapper for the
ddmlib Log class. 

Lets convert the previous System.out.println call in HelloWorldTest to a CLog call:

    @Override
    public void run(ITestInvocationListener listener) throws DeviceNotAvailableException {
        CLog.i("Hello world, TF! I have a device " + getDevice().getSerialNumber());

Now rebuild and rerun. You should see the log message on stdout. 

    tf> run example/helloworld
    …
    05-16 21:30:46 I/HelloWorldTest: Hello world, TF! I have a device 30315E38655500EC
    …

By default, TradeFederation will output host log messages to stdout. TradeFederation also includes a
log implementation that will write messages to a file: FileLogger. To add file logging, add a
'logger' tag to the configuration xml, specifying the full class name of FileLogger.

    <configuration description="Runs the hello world test">
        <test class="com.android.tradefed.example.HelloWorldTest" />
        <result_reporter class="com.android.tradefed.result.XmlResultReporter" />
        <logger class="com.android.tradefed.log.FileLogger" />
    </configuration> 

Now rebuild and run the helloworld example again.

    tf >run example/helloworld 
    …
    05-16 21:38:21 I/XmlResultReporter: Saved device_logcat log to /var/folders/++/++2Pz+++6+0++4RjPqRgNE+-4zk/-Tmp-/0/inv_6390011618174565918/device_logcat_1302097394309452308.txt
    05-16 21:38:21 I/XmlResultReporter: Saved host_log log to /tmp/0/inv_6390011618174565918/host_log_4255420317120216614.txt
    …

Note the log message indicating the path of the host log. View the contents of that file, and you
should see your HelloWorldTest log message

    $ more /tmp/0/inv_6390011618174565918/host_log_4255420317120216614.txt
    …
    05-16 21:38:21 I/HelloWorldTest: Hello world, TF! I have a device 30315E38655500EC

The TradeFederation framework will also automatically capture the logcat from the allocated device,
and send it the the result_reporter for processing. XmlResultReporter will save the captured device
logcat as a file.


## Command line options
Objects loaded from a TradeFederation Configuration (aka "Configuration objects") also have the
ability to receive data from command line arguments.

This is accomplished via the `@Option` annotation. To participate, a Configuration object class
would apply the `@Option` annotation to a member field, and provide it a unique name. This would
allow that member field's value to be populated via a command line option, and would also
automatically add that option to the configuration help system (Note: not all field types are
supported: see the OptionSetter javadoc for a description of supported types).

Lets add an Option to the HelloWorldTest.

    @Option(name="my_option",
            shortName='m',
            description="this is the option's help text",
            // always display this option in the default help text
            importance=Importance.ALWAYS)
    private String mMyOption = "thisisthedefault";

And lets add a log message to display the value of the option in HelloWorldTest, so we can
demonstrate that it was received correctly.

    @SuppressWarnings("unchecked")
    @Override
    public void run(ITestInvocationListener listener) throws DeviceNotAvailableException {
        …
        Log.logAndDisplay(LogLevel.INFO, "HelloWorldTest", "I received this option " + mMyOption);

Rebuild TF and run helloworld: you should see a log message with the my_option's default value.

    tf> run example/helloworld
    …
    05-24 18:30:05 I/HelloWorldTest: I received this option thisisthedefault

Now pass in a value for my_option: you should see my_option getting populated with that value

    tf> run example/helloworld --my_option foo
    …
    05-24 18:33:44 I/HelloWorldTest: I received this option foo

TF configurations also include a help system, which automatically displays help text for @Option
fields. Try it now, and you should see the help text for 'my_option':

    tf> run --help example/helloworld
    Printing help for only the important options. To see help for all options, use the --help-all flag

      cmd_options options:
        --[no-]help          display the help text for the most important/critical options. Default: false.
        --[no-]help-all      display the full help text for all options. Default: false.
        --[no-]loop          keep running continuously. Default: false.

      test options:
        -m, --my_option      this is the option's help text Default: thisisthedefault.

      'file' logger options:
        --log-level-display  the minimum log level to display on stdout. Must be one of verbose, debug, info, warn, error, assert. Default: error.
FIXME: redo with enum help

Note the message at the top about 'printing only the important options'. To reduce option help
clutter, TF uses the Option#importance attribute to determine whether to show an Option's help text
when '--help' is specified. '--help-all' will always show all options' help regardless of
importance. See Option.Importance javadoc for details.

You can also specify an Option's value within the configuration xml by adding a
`<option name="" value="">` element. Lets see how this looks in the helloworld.xml:

    <test class="com.android.tradefed.example.HelloWorldTest" >
        <option name="my_option" value="fromxml" />
    </test>

Re-building and running helloworld should now produce this output:

    05-24 20:38:25 I/HelloWorldTest: I received this option fromxml

The configuration help should also be updated to indicate my_option's new default value:

    tf> run --help example/helloworld
      test options:
        -m, --my_option      this is the option's help text Default: fromxml.

Also note that other configuration objects included in the helloworld config, like FileLogger, also have options. '--log-level-display' is of interest because it filters the logs that show up on stdout. You may have noticed from earlier in the tutorial the 'Hello world, TF! I have a device ..' log message stopped getting displayed on stdout once we switched to using FileLogger. You can increase the verbosity of logging to stdout by passing in log-level-display arg.

Try this now, and you should see the 'I have a device' log message reappear on stdout, in addition to getting logged to a file.

    tf >run --log-level-display info example/helloworld
    …
    05-24 18:53:50 I/HelloWorldTest: Hello world, TF! I have a device XXXXXX

<!-- To make future debugging in this tutorial easier, edit the helloworld.xml to default log-level-display to debug:

    <logger class="com.android.tradefed.log.FileLogger" >
        <option name="log-level-display" value="debug" />
    </logger>
-->
