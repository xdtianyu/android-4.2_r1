<!--
   Copyright 2011 The Android Open Source Project

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

# Trade Federation Overview #

TradeFederation (tradefed or TF for short) is a continuous test framework designed for running tests
on Android devices. Its a Java application which runs on a host computer, and communicates to one or
more Android devices using ddmlib (the library behind DDMS) over adb.

## Features

- modular, flexible design
- has built in support for running many different types of Android tests: instrumentation, native/gtest, host-based JUnit, etc
- provides reliability and recovery mechanism on top of adb
- supports scheduling and running tests on multiple devices in parallel

## Fundamentals
The lifecycle of a test executed using TradeFederation is composed of four separate stages, designed
around formally defined interfaces.

- [Build provider](bp.html): Provides a build to test, downloading appropriate files if necessary
- [Target preparer](tp.html): Prepares the test environment, e.g. software installation and setup
- [Test](test.html): Executes test(s) and gathers test results
- [Result reporter](result.html): Listens for test results, usually for the purpose of forwarding
  test results to a repository

The fundamental entity in TradeFederation is a Configuration. A Configuration is an XML file that
declares the lifecycle components of a test.

This separation of the test's lifecycle is intended to allow for reuse.  Using this design, you can
create a Test, and then different Configurations to run it in different environments. For example,
you could create a Configuration that will run a test on your local machine, and dump the result to
stdout.  You could then create a second Configuration that would execute that same test, but use a
different Result reporter to store the test results in a database.

### Additional components of a configuration

- [Device recovery](recovery.html): mechanism to recover device communication if lost
- [Logger](logger.html): collects tradefed logging data

A complete TradeFederation test execution, across its entire lifecycle, is referred to as an
Invocation.
