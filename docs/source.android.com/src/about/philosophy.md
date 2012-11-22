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

# Philosophy and Goals #

Android is an open-source software stack for mobile phones and other
devices.

## Origin and Goal ##

Android was originated by a group of companies known as the Open Handset
Alliance, led by Google. Today, many companies -- both original members of the
OHA and others -- have invested heavily in Android, typically in the form of
allocating significant engineering resources to improve Android and bring
Android devices to Market.

We created Android in response to our own experiences launching mobile
apps. We wanted to make sure that there would always be an open platform
available for carriers, OEMs, and developers to use to make their innovative
ideas a reality. We wanted to make sure that there was no central point of
failure, where one industry player could restrict or control the innovations
of any other. The solution we chose was an open and open-source platform.

The goal of the Android Open Source Project is to create a successful
real-world product that improves the mobile experience for end users.

## Governance Philosophy ##

The companies that have invested in Android have done so on its merits,
because we believe that an open platform is necessary. Android is
intentionally and explicitly an open-source -- as opposed to free software --
effort: a group of organizations with shared needs has pooled
resources to collaborate on a single implementation of a shared product. 
The Android philosophy is pragmatic, first and foremost. The objective is
a shared product that each contributor can tailor and customize.

Uncontrolled customization can, of course, lead to incompatible
implementations. To prevent this, the AOSP also maintains the Android
Compatibility Program, which spells out what it means to be "Android
compatible", and what is required of device builders to achieve that status.
Anyone can (and will!) use the Android source code for any purpose, and we
welcome all such uses. However, in order to take part in the shared
ecosystem of applications that we are building around Android, device builders
must participate in the Compatibility Program.

Though Android consists of multiple sub-projects, this is strictly a
project-management technique. We view and manage Android as a single,
holistic software product, not a "distribution", specification, or collection
of replaceable parts. Our intent is that device builders port
Android to a device; they don't implement a specification or curate a
distribution.

## How We Work ##

We know that quality does not come without hard work. Along with many
partners, Google has contributed full-time engineers, product managers, UI
designers, Quality Assurance, and all the other roles required to bring
modern devices to market.  We roll the open source administration and
maintenance into the larger product development cycle.

- At any given moment, there is a current latest release of the Android
platform. This typically takes the form of a branch in the tree.

- Device builders and Contributors work with the current
latest release, fixing bugs, launching new devices, experimenting with new
features, and so on.

- In parallel, Google works internally on the next version of the
Android platform and framework, working according to the product's needs and
goals. We develop the next version of Android by working with a device partner
on a flagship device whose specifications are chosen to push Android
in the direction we believe it should go.

- When the "n+1"th version is ready, it will be published to the public
source tree, and become the new latest release.

