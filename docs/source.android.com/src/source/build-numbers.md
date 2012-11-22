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

# Codenames, Tags, and Build Numbers #

At a high level, Android development happens around families of
releases, which use code names ordered alphabetically after tasty
treats.

## Platform code names, versions, API levels, and NDK releases ##

The code names match the following version numbers, along with
API levels and NDK releases provided for convenience:

Code name        | Version       | API level
-----------------|---------------|--------------------
(no code name)   | 1.0           | API level 1
(no code name)   | 1.1           | API level 2
Cupcake          | 1.5           | API level 3, NDK 1
Donut            | 1.6           | API level 4, NDK 2
Eclair           | 2.0           | API level 5
Eclair           | 2.0.1         | API level 6
Eclair           | 2.1           | API level 7, NDK 3
Froyo            | 2.2.x         | API level 8, NDK 4
Gingerbread      | 2.3 - 2.3.2   | API level 9, NDK 5
Gingerbread      | 2.3.3 - 2.3.7 | API level 10
Honeycomb        | 3.0           | API level 11
Honeycomb        | 3.1           | API level 12, NDK 6
Honeycomb        | 3.2.x         | API level 13
Ice Cream Sandwich | 4.0.1 - 4.0.2 | API level 14, NDK 7
Ice Cream Sandwich | 4.0.3 - 4.0.4 | API level 15, NDK 8
Jelly Bean       | 4.1.x         | API level 16

Starting with Cupcake, individual builds are identified with a short
build code, e.g. FRF85B.

The first letter is the code name of the release family, e.g. F is
Froyo.

The second letter is a branch code that allows Google to identify
the exact code branch that the build was made from, and R is by
convention the primary release branch.

The next letter and two digits are a date code. The letter counts
quarters, with A being Q1 2009. Therefore, F is Q2 2010. The two
digits count days within the quarter, so F85 is June 24 2010.

Finally, the last letter identifies individual versions related to
the same date code, sequentially starting with A; A is actually
implicit and usually omitted for brevity.

The date code is not guaranteed to be the exact date at which a build
was made, and it is common that minor variations added to an existing
build re-use the same date code as that existing build.

## Source code tags and builds ##

Starting with Donut, the exact list of tags and builds is in the
following table:

Build  | Tag                | Notes
-------|--------------------|-----------------------------------
DRC83  | android-1.6_r1.1   | earliest Donut version, ADP1, ADP2
DRC92  | android-1.6_r1.2
DRD08  | android-1.6_r1.3
DRD20  | android-1.6_r1.4
DMD64  | android-1.6_r1.5   | latest Donut version
ESD20  | android-2.0_r1     | earliest Eclair version
ESD56  | android-2.0.1_r1
ERD79  | android-2.1_r1     | Nexus One
ERE27  | android-2.1_r2     | Nexus One
EPE54B | android-2.1_r2.1p  | Nexus One
ESE81  | android-2.1_r2.1s
EPF21B | android-2.1_r2.1p2 | latest Eclair version
FRF85B | android-2.2_r1     | earliest Froyo version, Nexus One
FRF91  | android-2.2_r1.1   | Nexus One
FRG01B | android-2.2_r1.2
FRG22D | android-2.2_r1.3
FRG83  | android-2.2.1_r1   | Nexus One
FRG83D | android-2.2.1_r2   | Nexus One
FRG83G | android-2.2.2_r1   | Nexus One
FRK76  | android-2.2.3_r1
FRK76C | android-2.2.3_r2   | latest Froyo version
GRH55  | android-2.3_r1     | earliest Gingerbread version, Nexus S
GRH78  | android-2.3.1_r1   | Nexus S
GRH78C | android-2.3.2_r1   | Nexus S
GRI40  | android-2.3.3_r1   | Nexus One, Nexus S
GRI54  | android-2.3.3_r1.1 | Nexus S
GRJ06D | android-2.3.4_r0.9 | Nexus S 4G
GRJ22  | android-2.3.4_r1   | Nexus One, Nexus S, Nexus S 4G
GRJ90  | android-2.3.5_r1   | Nexus S 4G
GRK39C | android-2.3.6_r0.9 | Nexus S
GRK39F | android-2.3.6_r1   | Nexus One, Nexus S
GWK74  | android-2.3.7_r1   | latest Gingerbread version, Nexus S 4G
ITL41D | android-4.0.1_r1   | earliest Ice Cream Sandwich version, Galaxy Nexus
ITL41D | android-4.0.1_r1.1 | Galaxy Nexus
ITL41F | android-4.0.1_r1.2 | Galaxy Nexus
ICL53F | android-4.0.2_r1   | Galaxy Nexus
IML74K | android-4.0.3_r1   | Nexus S
IML77  | android-4.0.3_r1.1 |
IMM76  | android-4.0.4_r1   |
IMM76D | android-4.0.4_r1.1 | Nexus S, Nexus S 4G, Galaxy Nexus
IMM76I | android-4.0.4_r1.2 | Galaxy Nexus
IMM76K | android-4.0.4_r2   | Galaxy Nexus
IMM76L | android-4.0.4_r2.1 | latest Ice Cream Sandwich version
JRO03C | android-4.1.1_r1   | earliest Jelly Bean version, Galaxy Nexus
JRO03D | android-4.1.1_r1.1 | Nexus 7
JRO03E | android-4.1.1_r2   | Nexus S
JRO03H | android-4.1.1_r3   |
JRO03L | android-4.1.1_r4   | latest Jelly Bean version, Nexus S

The branches froyo, gingerbread, ics-mr0, ics-mr1, jb-dev,
represent development
branches that do not exactly match configurations that were tested
by Google. They might contain a variety of changes in addition to
the official tagged releases, and those haven't been as thoroughly
tested.

## Honeycomb GPL modules ##

For Honeycomb, the entire platform source code isn't available.
However, the parts of Honeycomb licensed under the GPL and LGPL
are available under the following tags:

Build  | Tag                | Notes
-------|--------------------|-----------------------------------
HRI39  | android-3.0_r1     | earliest Honeycomb version
HRI66  | android-3.0_r1.1
HWI69  | android-3.0_r1.2
HRI93  | android-3.0_r1.3
HMJ37  | android-3.1_r1
HTJ85B | android-3.2_r1
HTK55D | android-3.2.1_r1
HTK75D | android-3.2.1_r2
HLK75C | android-3.2.2_r1
HLK75D | android-3.2.2_r2
HLK75F | android-3.2.4_r1
HLK75H | android-3.2.6_r1   | latest Honeycomb version

There is no manifest that contains exactly those. However, there
are manifests that allow building those components. The following
commands work for 3.0_r1.1, and using other versions can be done by
switching the git checkout paramater, and if necessary the -m parameter in
repo init. The git checkout command outputs an error for the non-GPL
projects, where it can't find the tag in question.

    $ repo init -b master -m base-for-3.0-gpl.xml
    $ repo sync
    $ repo forall -c git checkout android-3.0_r1.1
