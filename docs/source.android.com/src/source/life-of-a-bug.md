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

# Life of a Bug #

The Android Open Source project maintains a public issue tracker where you
can report bugs and request features for the Android software stack. (For
details on this issue tracker, please see the [Reporting Bugs](report-bugs.html) page).
Reporting bugs is great (thank you!), but what happens to a bug report once
you file it? This page describes the Life of a Bug.

*Please note: the Android Open Source Project (AOSP) issue tracker is
intended only for bugs and feature requests related to the Android software
stack. Because many users find their way here looking for the Google apps for
Android (such as Gmail and so on), we have components set up for their
convenience. However, these apps are not part of Android, and any issues
reported on these components are not guaranteed to to receive attention.
Most notably, to report issues related to Google Play, you should visit the
[Google Play Support Forum](https://support.google.com/googleplay/).

Here's the life of a bug, in a nutshell:

1. A bug is filed, and has the state "New".

1. An AOSP contributor periodically reviews and triages bugs. Bugs are
triaged into one of four "buckets": New, Open, No-Action, or Resolved.

1. Each bucket includes a number of states that provide more detail on the
fate of the issue.

1. Bugs in the "Resolved" bucket will eventually be included in a future
release of the Android software.

# Bucket Details #

Here is some additional information on each bucket, what it means, and how
it's handled.

## New Issues ##

New issues include bug reports that are not yet being acted upon. The two
states are:

- *New:*
    The bug report has not yet been triaged (that is, reviewed by an AOSP contributor.)

- *NeedsInfo:*
    The bug report has insufficient information to act
upon. The person who reported the bug needs to provide additional detail
before it can be triaged. If enough time passes and no new information is
provided, the bug may be closed by default, as one of the No-Action
states.

## Open Issues ##

This bucket contains bugs that need action, but which are still
unresolved, pending a change to the source code.

- *Unassigned:*
    The bug report has been recognized as an adequately
detailed report of a legitimate issue, but has not yet been assigned to an
AOSP contributor to be fixed. Typically, bugs in this state are considered low
priority, at least insofar that if they were high priority, they'd be assigned
to a contributor.

- *Reviewed:*
    Like *Unassigned*, but the issue
represented is being tracked in a separate bug database. For example, the bug
might have been reported via an internal bug-tracking system,
which is considered the "master" copy. (For instance, Google maintains one
such private issue tracker, intended primarily for bugs which contain
sensitive information which can't be revealed publicly.)

- *Assigned:*
    Like *Unassigned*, but the bug has been
actually assigned to a specific contributor to fix.

Typically, a given bug will start in *Unassigned*, where it
will remain until it is associated with a specific upcoming release, at which
point it will enter *Reviewed* or *Assigned*. However,
note that this isn't a guarantee, and it's not uncommon for bugs to go from
*Unassigned* to one of the Resolved states.

In general, if a bug is in one of these Open states, the AOSP team has
recognized it as a legitimate issue and will fix it according to the product
priorities and milestones. However, it's impossible to guarantee a fix in time 
for any particular release.

## No-Action Issues ##

This bucket contains bugs that have for one reason or another been
determined to not require any action.

- *Spam:* 
    A kind soul sent us some delicious pork products, that we,
regrettably, do not want.

- *Question:*
    Someone mistook the issue tracker for a help forum.
(This is not as uncommon as you might think: many users whose native language
isn't English misunderstand the site and make this mistake.)

- *Unreproducible:*
    An AOSP contributor attempted to reproduce the
behavior described, and was unable to do so. This sometimes means that the bug
is legitimate but simply rare or difficult to reproduce, and sometimes means
that the bug was fixed in a later release.

- *WorkingAsIntended:*
    An AOSP contributor has determined that the
behavior described isn't a bug, but is the intended behavior. This state is
also commonly referred to as "WAI".

- *Declined:*
    This is like *WorkingAsIntended*, except
typically used for feature requests instead of bugs.  That is, an AOSP
contributor has determined that the request is not going to be implemented in
Android.

## Resolved Issues ##

This bucket contains bugs that have had action taken, and are now
considered resolved.

- *FutureRelease:*
    This bug has been fixed (or feature implemented) in
a source tree, but has not yet been included in a formal Android
platform release. (Note that this may also include fixes that exist in a
private source tree that has not yet been contributed to a public
tree.)

- *Released:*
    This bug has been fixed, and is included in a formal
Android platform release. When this state is set, we try to also set a
property indicating which release it was fixed in.

- *Duplicate:*
    This bug is a duplicate of another, existing bug report.

# Other Stuff #

The states and lifecycle above are how we generally try to track software.
However, Android contains a lot of software and gets a correspondingly large
number of bugs. As a result, sometimes bugs don't make it through all the
states in a formal progression. We do try to keep the system up to date, but
we tend to do so in periodic "bug sweeps" where we review the database and
make updates.

Since the AOSP is essentially constantly evolving, we do make tweaks to
the list of bug states and the lifecycle described above.  When we do this,
however, we'll be sure to update this page as well.

Finally, you should be aware that for a variety of reasons, there are
actually multiple issue trackers for Android-related issues. The 
[Google Code Project Hosting Issue Tracker](https://code.google.com/p/android/issues/list)
is the *only* official public issue tracker; however,
Google also maintains a private issue tracker, own, as do most OEMs. We try to
keep the public issue tracker in sync with private issue trackers
wherever possible, but in cases where confidential information and security
issues are involved, this isn't always possible.
