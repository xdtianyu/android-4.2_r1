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

# Building Kernels #

If you are only interested in the kernel, you may use this guide
to download and build the appropriate kernel.

The following instructions assume that you have not downloaded all
of AOSP.  If you have downloaded all of AOSP, you may skip the git
clone steps other than the step to download the actual kernel sources.

We will use the Pandaboard kernel in all the following examples.


## Figuring out which kernel to build ##

You will want to look at the git log for the kernel in the device project that
you are interested in.
Device projects are of the form device/&lt;vendor&gt;/&lt;name&gt;.

    $ git clone https://android.googlesource.com/device/ti/panda
    $ cd panda
    $ git log kernel

The log should contain notes of the commit SHA1 for the appropriate
kernel project.  Keep this value at hand so that you can use it in
a later step.

## Downloading sources ##

Depending on which kernel you want,

    $ git clone https://android.googlesource.com/kernel/common.git
    $ git clone https://android.googlesource.com/kernel/exynos.git
    $ git clone https://android.googlesource.com/kernel/goldfish.git
    $ git clone https://android.googlesource.com/kernel/msm.git
    $ git clone https://android.googlesource.com/kernel/omap.git
    $ git clone https://android.googlesource.com/kernel/samsung.git
    $ git clone https://android.googlesource.com/kernel/tegra.git

  - The `goldfish` project contains the kernel sources for the emulated
platforms.
  - The `msm` project has the sources for ADP1, ADP2, Nexus One, and
can be used as a starting point for work on Qualcomm MSM chipsets.
  - The `omap` project is used for PandaBoard and Galaxy Nexus, and
can be used as a starting point for work on TI OMAP chipsets.
  - The `samsung` project is used for Nexus S and can be used as a
starting point for work on Samsung Hummingbird chipsets.
  - The `tegra` project is for Xoom and Nexus 7, and can be used as
a starting point for work on NVIDIA Tegra chipsets.
  - The `exynos` project can be used as a starting point for work
on Samsung Exynos chipsets.

## Downloading a prebuilt gcc ##

Ensure that the prebuilt toolchain is in your path.

    $ git clone https://android.googlesource.com/platform/prebuilt
    $ export PATH=$(pwd)/prebuilt/linux-x86/toolchain/arm-eabi-4.4.3/bin:$PATH


## Building ##

As an example, we would build the panda kernel using the following commands:

    $ export ARCH=arm
    $ export SUBARCH=arm
    $ export CROSS_COMPILE=arm-eabi-
    $ cd omap
    $ git checkout <commit_from_first_step>
    $ make panda_defconfig
    $ make

To build the tuna kernel, you may run the previous commands replacing all
instances of "panda" with "tuna".

  - The kernel for maguro and toro is `device/samsung/tuna/kernel`
  - The kernel for crespo and crespo4g is `device/samsung/crespo/kernel`
  - The kernel for stingray and wingray is `device/moto/wingray/kernel`

The image is output as `arch/arm/boot/zImage`.  You may copy it as
`device/<vendor>/<name>/kernel` or `device/ti/panda/kernel` in the case of this
example.
