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

# Using Eclipse #

This document will help you set up the Eclipse IDE for Android platform development.

*Note: if you are looking for information on how to use
Eclipse to develop applications that run on Android, this is not the right
page for you. You probably would find [the Eclipse page on 
developer.android.com](https://developer.android.com/sdk/eclipse-adt.html) more useful.*

## Basic setup ##

First, it's important to make sure the regular Android development system is set up.

    cd /path/to/android/root 
    make     

**Important**: You will still be using `make` to build the files you will actually run (in the emulator or on a device). You will be using Eclipse to edit files and verify that they compile, but when you want to run something you will need to make sure files are saved in Eclipse and run `make` in a shell. The Eclipse build is just for error checking.

Eclipse needs a list of directories to search for Java files. This is called the "Java Build Path" and can be set with the `.classpath` file. We have a sample version to start you off.

    cd /path/to/android/root 
    cp development/ide/eclipse/.classpath .
    chmod u+w .classpath  

Now edit that copy of `.classpath`, if necessary.

### Increase Eclipse's Memory Settings ###

The Android project is large enough that Eclipse's Java VM sometimes runs out of memory while compiling it. Avoid this problem by editing the `eclipse.ini` file. On Apple OSX the eclipse.ini file is located at

    /Applications/eclipse/Eclipse.app/Contents/MacOS/eclipse.ini

Memory-related defaults (as of Eclipse 3.4):

    -Xms40m 
    -Xmx256m 
    -XX:MaxPermSize=256m 

Recommended settings for Android development:

    -Xms128m 
    -Xmx512m 
    -XX:MaxPermSize=256m 

These settings set Eclipse's minimum Java heap size to 128MB, set the maximum Java heap size to 512MB, and keep the maximum permanent generation size at the default of 256MB.

Now start Eclipse:

    eclipse  

Now create a project for Android development:

1. If Eclipse asks you for a workspace location, choose the default.

2. If you have a "Welcome" screen, close it to reveal the Java perspective.

3. File > New > Java Project

4. Pick a project name, "android" or anything you like.

5. Select "Create project from existing source", enter the path to your Android root directory, and click Finish.

6. Wait while it sets up the project. (You'll see a subtle progress meter in the lower right corner.)

Once the project workspace is created, Eclipse should start building. In theory, it should build with no errors and you should be set to go. If necessary, uncheck and re-check Project Build Automatically to force a rebuild.

*Note:* Eclipse sometimes likes to add an `import android.R` statement at the top of your files that use resources, especially when you ask eclipse to sort or otherwise manage imports. This will cause your make to break. Look out for these erroneous import statements and delete them.

### When You Sync ###

Every time you repo sync, or otherwise change files outside of Eclipse (especially the .classpath), you need to refresh Eclipse's view of things:

1. Window > Show View > Navigator

1. In the Navigator, right-click on the project name

1. Click Refresh in the context menu

### Adding Apps to the Build Path ###

The default `.classpath` includes the source to the core system and a sample set of apps, but might not include the particular app you may want to work on. To add an app, you must add the app's source directory. To do this inside Eclipse:

1. Project > Properties

1. Select "Java Build Path" from the left-hand menu.

1. Choose the "Source" tab.

1. Click "Add Folder..."

1. Add your app's `src` directory.

1. Click OK.

When you're done, the "source folder" path in the list should look like 

    android/packages/apps/YOURAPP/src 

Depending on which app(s) you include, you may also need to include `othersrc/main/java` directories under `android/dalvik/libcore`. Do this if you find you cannot build with the default set.

## Eclipse formatting ##

You can import files in `development/ide/eclipse` to make Eclipse
follow the Android style rules.  

1. Select Window > Preferences > Java > Code Style.

1. Use Formatter > Import to import `android-formatting.xml`.

1. Organize Imports > Import to import `android.importorder`.

## Debugging the emulator with Eclipse ##

You can also use eclipse to debug the emulator and step through code. First, start the emulator running:

    cd /path/to/android/root 
    . build/envsetup.sh 
    lunch 1    
    make       
    emulator  

If the emulator is running, you should see a picture of a phone.

In another shell, start DDMS (the Dalvik debug manager):

    cd /path/to/android/root 
    ddms      

You should see a splufty debugging console.

Now, in eclipse, you can attach to the emulator:

1. Run > Open Debug Dialog...

1. Right-click "Remote Java Application", select "New".

1. Pick a name, i.e. "android-debug" or anything you like.

1. Set the "Project" to your project name.

1. Keep the Host set to "localhost", but change Port to 8700.

1. Click the "Debug" button and you should be all set.

Note that port 8700 is attached to whatever process is currently selected in the DDMS console, so you need to sure that DDMS has selected the process you want to debug.

You may need to open the Debug perspective (next to the "Java" perspective icon in the upper-right, click the small "Open Perspective" icon and select "Debug"). Once you do, you should see a list of threads; if you select one and break it (by clicking the "pause" icon), it should show the stack trace, source file, and line where execution is at. Breakpoints and whatnot should all work.

## Bonus material ##

Replace Ctrl with the Apple key on Mac.

shortcut     | function
-------------|-----------------
Ctrl-Shift-o | Organize imports 
Ctrl-Shift-t | load class by name 
Ctrl-Shift-r | load non-class resource by name 
Ctrl-1       | quick fix 
Ctrl-e       | Recently viewed files 
Ctrl-space   | auto complete 
Shift-Alt-r  | refactor:rename 
Shift-Alt-v  | refactor:move 

## Eclipse is not working correctly, what should I do? ##

Make sure:

- You followed the instructions on this page precisely.

- Your Problems view doesn't show any errors.

- Your application respects the package/directory structure.

If you're still having problems, please contact one of the Android mailing lists or IRC channels.

