<!--
   Copyright 2010 The Android Open Source Project 

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

# Android Code-Lines #

The Android Open Source Project maintains a complete software stack intended
to be ported by OEMs and other device implementors to run on actual hardware.
Accordingly, we maintain a number of "code lines" to clearly separate the
current stable version of Android from unstable experimental work.

The chart below depicts at a conceptual level how AOSP manages code and
releases. We're referring to these as "code lines" instead of "branches"
simply because at any given moment there may be more than one branch extant
for a given "code line".  For instance, when a release is cut, sometimes that
will become a new branch in git, and sometimes not, based on the needs of the
moment.

<img src="/images/code-lines.png" alt="code-line diagram"/>

## Notes and Explanations ##

- A *release* corresponds to a formal version of the Android platform, such
as 1.5, 2.1, and so on. Generally speaking, a release of the platform
corresponds to a version of the `SdkVersion` field used in
AndroidManifest.xml files, and defined in `frameworks/base/api` in
the source tree.

- An *upstream* project is an open-source project from which the Android
stack is pulling code. These include obvious projects such as the Linux kernel
and WebKit, but over time we are migrating some of the semi-autonomous
Android projects (such as Dalvik, the Android SDK tools, Bionic, and so on) to
work as "upstream" projects. Generally, these projects are developed entirely in
the public tree. For some upstream projects, development is done by contributing
directly to the upstream project itself. See [Upstream Projects](submit-patches.html#upstream-projects)
for details. In both cases, snapshots will be periodically pulled into releases.

- The diagram refers to "Eclair" and "FroYo"; however, they are simply
placeholders, and the diagram actually reflects the overall release and
branching strategy.

- At all times, a release code-line (which may actually consist of
more than one actual branch in git) is considered the sole canonical source
code for a given Android platform version. OEMs and other groups building devices
should pull only from a release branch.

- We will set up "experimental" code-lines to capture changes from
the community, so that they can be iterated on, with an eye toward stability.

- Changes that prove stable will eventually be pulled into a release
branch. Note that this will only apply to bug fixes, app improvements, and
other things that do not affect the APIs of the platform.

- Changes will be pulled into release branches from upstream projects
(including the Android "upstream" projects) as necessary.

- The "n+1"th version (that is, next major version of the framework and
platform APIs) will be developed by Google internally. See below for
details.

- Changes will be pulled from upstream, release, and experimental branches
into Google's private branch as necessary.

- When the platform APIs for the next version have stabilized and been fully
tested, Google will cut a release of the next platform version. (This
specifically refers to a new `SdkVersion`.) This will also
correspond to the internal code-line being made a public release branch, and the
new current platform code-line.

- When a new platform version is cut, a corresponding experimental
code-line will be created at the same time.

## About Private Code-Lines ##

The source management strategy above includes a code-line that Google will
keep private. The reason for this is to focus attention on the current public
version of Android.

OEMs and other device builders naturally want to ship devices with the
latest version of Android. Similarly, application developers don't want to
deal with more extant platform versions than strictly necessary.  Meanwhile,
Google retains responsibility for the strategic direction of Android as a
platform and a product. Our approach is based on focusing on a small number of
flagship devices to drive features, and secure protections of Android-related
intellectual property.

As a result, Google frequently has possession of confidential
information of third parties, and we must refrain from revealing sensitive
features until we've secured the appropriate protections. Meanwhile, there are
real risks to the platform arising from having too many platform versions
extant at once. For these reasons, we have structured the open-source project
-- including third-party contributions -- to focus on the currently-public
stable version of Android. "Deep development" on the next version of the
platform will happen in private, until it's ready to become an official
release.

We recognize that many contributors will disagree with this approach. We
respect that others may have a different point of view; however, this is the
approach that we feel is best, and the one we've chosen to implement.


