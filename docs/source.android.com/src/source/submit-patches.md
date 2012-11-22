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

# Submitting Patches #

This page describes the full process of submitting a patch to the AOSP, including
reviewing and tracking changes with [Gerrit](https://android-review.googlesource.com/).

## Prerequisites ##

- Before you follow the instructions on this page, you will need to set up your
local working environment and get the Android source files. For instructions,
follow the "Getting Started" section [here](downloading.html).

- For details about Repo and Git, see [Version Control](version-control.html).

- For information about the different roles you can play within the Android
Open Source community, see [Project roles](/source/roles.html).

- If you plan to contribute code to the Android platform, be sure to read
the [AOSP's licensing information](/source/licenses.html).

- Note that changes to some of the upstream projects used by Android should be
made directly to that project, as described in [Upstream Projects](#upstream-projects).

# For contributors #

## Authenticate with the server ##

Before you can upload to Gerrit, you need to establish a password that
will identify you with the server. You only need to do this once.

- Sign in on the [AOSP Gerrit Server](https://android-review.googlesource.com/).

- Go to Settings -> HTTP Password -> Obtain Password

- Follow the instructions on the subsquent pages, and copy-paste your
password in `~/.netrc`. If there are two password lines, copy both.

## Start a repo branch ##

For each change you intend to make, start a new branch within the relevant git repository:

    $ repo start NAME .

You can start several independent branches at the same time in the same repository. The branch NAME is local to your workspace and will not be included on gerrit or the final source tree.

## Make your change ##

Once you have modified the source files (and validated them, please) commit the changes to your local repository:

    $ git add -A
    $ git commit -s

Provide a detailed description of the change in your commit message. This description will be pushed to the public AOSP repository, so please follow our guidelines for writing changelist descriptions: 

- Start with a one-line summary (60 characters max), followed by a blank line. This format is used by git and gerrit for various displays. 
    
        short description on first line
        
        more detailed description of your patch,
        which is likely to take up multiple lines.

- The description should focus on what issue it solves, and how it solves it. The second part is somewhat optional when implementing new features, though desirable.

- Include a brief note of any assumptions or background information that may be important when another contributor works on this feature next year. 

A unique change ID and your name and email as provided during `repo init` will be automatically added to your commit message. 

## Upload to gerrit ##

Once you have committed your change to your personal history, upload it to gerrit with

    $ repo upload

If you have started multiple branches in the same repository, you will be prompted to select which one(s) to upload.

After a successful upload, repo will provide you the URL of a new page on
[Gerrit](https://android-review.googlesource.com/). Visit this link to view
your patch on the review server, add comments, or request specific reviewers
for your patch.

## Uploading a replacement patch ##

Suppose a reviewer has looked at your patch and requested a small modification. You can amend your commit within git, which will result in a new patch on gerrit with the same change ID as the original.

*Note that if you have made other commits since uploading this patch, you will need to manually move your git HEAD.*

    $ git add -A
    $ git commit --amend

When you upload the amended patch, it will replace the original on gerrit and in your local git history.

## Resolving sync conflicts ##

If other patches are submitted to the source tree that conflict with yours, you will need to rebase your patch on top of the new HEAD of the source repository. The easy way to do this is to run

    $ repo sync

This command first fetches the updates from the source server, then attempts to automatically rebase your HEAD onto the new remote HEAD.

If the automatic rebase is unsuccessful, you will have to perform a manual rebase.

    $ git rebase master

Using `git mergetool` may help you deal with the rebase conflict. Once you have successfully merged the conflicting files,

    $ git rebase --continue

After either automatic or manual rebase is complete, run `repo upload` to submit your rebased patch.

## After a submission is approved ##

After a submission makes it through the review and verification process, Gerrit automatically merges the change into the public repository. Other users will be able to run `repo sync` to pull the update into their local client.

# For reviewers and verifiers #

## Reviewing a change ##

If you are assigned to be the Approver for a change, you need to determine the following:

- Does this change fit within this project's stated purpose?

- Is this change valid within the project's existing architecture?

- Does this change introduce design flaws that will cause problems in the future?

- Does this change follow the best practices that have been established for this project?

- Is this change a good way to perform the described function?

- Does this change introduce any security or instability risks?

If you approve of the change, mark it with LGTM ("Looks Good to Me") within Gerrit.

## Verifying a change ##

If you are assigned to be the Verifier for a change, you need to do the following:

- Patch the change into your local client using one of the Download commands.

- Build and test the change.

- Within Gerrit use Publish Comments to mark the commit as "Verified" or "Fails," and add a message explaining what problems were identified.

## Downloading changes from Gerrit ##

A submission that has been verified and merged will be downloaded with the next `repo sync`. If you wish to download a specific change that has not yet been approved, run

    $ repo download TARGET CHANGE

where TARGET is the local directory into which the change should be downloaded and CHANGE is the 
change number as listed in [Gerrit](https://android-review.googlesource.com/). For more information,
see the [Repo reference](/source/using-repo.html).

## How do I become a Verifier or Approver? ##

In short, contribute high-quality code to one or more of the Android projects.
For details about the different roles in the Android Open Source community and
who plays them, see [Project Roles](/source/roles.html).

## Diffs and comments ##

To open the details of the change within Gerrit, click on the "Id number" or "Subject" of a change. To compare the established code with the updated code, click the file name under "Side-by-side diffs."

## Adding comments ##

Anyone in the community can use Gerrit to add inline comments to code submissions. A good comment will be relevant to the line or section of code to which it is attached in Gerrit. It might be a short and constructive suggestion about how a line of code could be improved, or it might be an explanation from the author about why the code makes sense the way it is.

To add an inline comment, double-click the relevant line of the code and write your comment in the text box that opens. When you click Save, only you can see your comment.

To publish your comments so that others using Gerrit will be able to see them, click the Publish Comments button. Your comments will be emailed to all relevant parties for this change, including the change owner, the patch set uploader (if different from the owner), and all current reviewers.

<a name="upstream-projects"></a>

# Upstream Projects #

Android makes use of a number of other open-source projects, such as the Linux kernel and WebKit, as described in
[Branches and Releases](/source/code-lines.html). For most projects under `external/`, changes should be made upstream and then the Android maintainers informed of the new upstream release containing these changes. It may also be useful to upload patches that move us to track a new upstream release, though these can be difficult changes to make if the project is widely used within Android like most of the larger ones mentioned below, where we tend to upgrade with every release.

One interesting special case is bionic. Much of the code there is from BSD, so unless the change is to code that's new to bionic, we'd much rather see an upstream fix and then pull a whole new file from the appropriate BSD. (Sadly we have quite a mix of different BSDs at the moment, but we hope to address that in future, and get into a position where we track upstream much more closely.)

## ICU4C ##

All changes to the ICU4C project at `external/icu4c` should be made upstream at
[icu-project.org/](http://site.icu-project.org/).
See [Submitting ICU Bugs and Feature Requests](http://site.icu-project.org/bugs) for more.

## OpenSSL ##

All changes to the OpenSSL project at `external/openssl` should be made upstream at
[openssl.org](http://www.openssl.org).

## V8 ##

All changes to the V8 project at `external/v8` should be submitted upstream at
[code.google.com/p/v8](https://code.google.com/p/v8). See [Contributing to V8](https://code.google.com/p/v8/wiki/Contributing)
for details.

## WebKit ##

All changes to the WebKit project at `external/webkit` should be made
upstream at [webkit.org](http://www.webkit.org). The process begins by filing a WebKit bug. 
This bug should use `Android` for the `Platform` and `OS` 
fields only if the bug is specific to Android. Bugs are far more likely to receive the reviewers'
attention once a proposed fix is added and tests are included. See
[Contributing Code to WebKit](http://webkit.org/coding/contributing.html) for details.

## zlib ##

All changes to the zlib project at `external/zlib` should be made upstream at
[zlib.net](http://zlib.net).
