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

# Version Control with Repo and Git #

To work with the Android code, you will need to use both Git and Repo.  In most situations, you can use Git instead of Repo, or mix Repo and Git commands to form complex commands. Using Repo for basic across-network operations will make your work much simpler, however.

**Git** is an open-source version-control system designed to handle very large projects that are distributed over multiple repositories. In the context of Android, we use Git for local operations such as local branching, commits, diffs, and edits.  One of the challenges in setting up the Android project was figuring out how to best support the outside community--from the hobbiest community to large OEMs building mass-market consumer devices. We wanted components to be replaceable, and we wanted interesting components to be able to grow a life of their own outside of Android. We first chose a distributed revision control system, then further narrowed it down to Git.

**Repo** is a repository management tool that we built on top of Git. Repo
unifies the many Git repositories when necessary, does the uploads to our
[revision control system](https://android-review.googlesource.com/), and
automates parts of the Android development workflow. Repo is not meant to
replace Git, only to make it easier to work with Git in the context of
Android. The repo command is an executable Python script that you can put
anywhere in your path. In working with the Android source files, you will
use Repo for across-network operations. For example, with a single Repo
command you can download files from multiple repositories into your local
working directory.

**Gerrit** is a web-based code review system for projects that use git. Gerrit encourages more centralized use of Git by allowing all authorized users to submit changes, which are automatically merged if they pass code review. In addition, Gerrit makes reviewing easier by displaying changes side by side in-browser and enabling inline comments. 

## Basic Workflow ##

<div style="float:right">
  <img src="/images/submit-patches-0.png" alt="basic workflow diagram">
</div>

The basic pattern of interacting with the repositories is as follows:

1. Use `repo start` to start a new topic branch.

1. Edit the files.

1. Use `git add` to stage changes.

1. Use `git commit` to commit changes.

1. Use `repo upload` to upload changes to the review server.

# Task reference #

The task list below shows a summary of how to do common Repo and Git tasks.
For complete quick-start information and examples, see [Getting started](downloading.html).

## Synchronizing your client ##

To synchronize the files for all available projects: 

    $ repo sync

To synchronize the files for selected projects:

    $ repo sync PROJECT0 PROJECT1 PROJECT2 ...

## Creating topic branches ##

Start a topic branch in your local work environment whenever you begin a change, for example when you begin work on a bug or new feature. A topic branch is not a copy of the original files; it is a pointer to a particular commit. This makes creating local branches and switching among them a light-weight operation. By using branches, you can isolate one aspect of your work from the others. For an interesting article about using topic branches, see [Separating topic branches](http://www.kernel.org/pub/software/scm/git/docs/howto/separating-topic-branches.txt).
<img src="/images/external-link.png" alt="">

To start a topic branch using Repo: 

    $ repo start BRANCH_NAME

To verify that your new branch was created:

    $ repo status

## Using topic branches ##

To assign the branch to a particular project:

    $ repo start BRANCH_NAME PROJECT

To switch to another branch that you have created in your local work environment:

    $ git checkout BRANCH_NAME

To see a list of existing branches:

    $ git branch

or 

    $ repo branches

The name of the current branch will be preceded by an asterisk.

*Note: A bug might be causing `repo sync` to reset the local topic branch. If `git branch` shows \* (no branch) after you run `repo sync`, then run `git checkout` again.*

## Staging files ##

By default, Git notices but does not track the changes you make in a project. In order to tell git to preserve your changes, you must mark them for inclusion in a commit. This is also called "staging". 

You can stage your changes by running

    git add

which accepts as arguments any files or directories within the project directory. Despite the name, `git add` does not simply add files to the git repository; it can also be used to stage file modifications and deletions.

## Viewing client status ##

To list the state of your files:

    $ repo status

To see uncommitted edits:

    $ repo diff

The `repo diff` command shows every local edit that you have made that would *not* go into the commit, if you were to commit right now. To see every edit that would go into the commit if you were to commit right now, you need a Git command, `git diff`. Before running it, be sure you are in the project directory:

    $ cd ~/WORKING_DIRECTORY/PROJECT  
    $ git diff --cached

## Committing changes ##

A commit is the basic unit of revision control in git, consisting of a snapshot of directory structure and file contents for the entire project. Creating a commit in git is as simple as typing

    git commit

You will be prompted for a commit message in your favorite editor; please provide a helpful message for any changes you submit to the AOSP. If you do not add a log message, the commit will be aborted. 

## Uploading changes to Gerrit ##

Before uploading, update to the latest revisions:

    repo sync

Next run

    repo upload

This will list the changes you have committed and prompt you to select which branches to upload to the review server. If there is only one branch, you will see a simple `y/n` prompt.

## Recovering sync conflicts ##

If a `repo sync` shows sync conflicts:

- View the files that are unmerged (status code = U).
- Edit the conflict regions as necessary.
- Change into the relevant project directory, run `git add` and `git commit` for the files in question, and then "rebase" the changes. For example:

        $ git add .
        $ git commit 
        $ git rebase --continue

- When the rebase is complete start the entire sync again:

        $ repo sync PROJECT0 PROJECT1 ... PROJECTN

## Cleaning up your client files ##

To update your local working directory after changes are merged in Gerrit:

    $ repo sync 

To safely remove stale topic branches: 

    $ repo prune

## Deleting a client ##

Because all state information is stored in your client, you only need to delete the directory from your filesystem:

    $ rm -rf WORKING_DIRECTORY

Deleting a client will *permanently delete* any changes you have not yet uploaded for review.

# Git and Repo cheatsheet #

<img src="/images/git-repo-1.png" alt="list of basic git and repo commands">


