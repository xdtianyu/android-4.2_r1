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

# Command File Format

A command file allows one to specify sets of TF commands (that is, configurations with their
associated arguments) to be specified all at once.  Further, the format used in the command file
supports simple macro expansions, which makes it very useful without being unwieldy.  You can see a
relatively involved example at the bottom of the page, as well as more gradual documentation
immediately below.


## Lines

The format is line-based.

* Each output line will be considered as the arguments for a single Configuration for Trade
    Federation to run.
* Each input line will turn into one or more output lines.

In essence, the command file format combinatorially creates a sequence of Configuration specifications that it passes to Trade Federation to run.  Blank lines are ignored.  Comments are delimited by the "#" character and may only be preceded by whitespace.


## Macros

The specific syntax for defining a macro is discussed below in the Short Macro and Long Macro sections.  The following rules apply to all macros

* The name of a macro must begin with an alpha character.  Each subsequent character may be any
    alphanumeric, an underscore, or a hyphen.
* A macro with name "macro_name" is invoked by adding macro_name() as an argument on some subsequent
    line.
* The macro format does not support passing arguments to macros.  It allows only concatenation.
* A macro's expansion may contain invocations of other macros.  Technically, a macro's expansion may
    contain invocations of itself, but in that case, the macro will never fully expand.
* The parser currently has a hard limit of 10 iterations of expansion.  This will be made
    configurable at some point.
* During a single iteration of expansion:
    * All short macro invocations on a line will be expanded a single level — macro invocations
        embedded within the first-level expansions will not be expanded yet
    * Only one long macro invocation on a line will be expanded.  This will be the left-most long
        macro invocation.


## Short Macros

A short macro can be defined with the syntax:

    MACRO macro_name = this is the macro expansion

The macro expansion terminates at the end of the line.  For multi-line expansion, see Long Macros
below.

### Short Macro Expansion

* `a macro_name() invocation`<br />
  will be replaced by<br />
  `a this is the macro expansion invocation`
* `three macro_name() A macro_name() B macro_name()`<br />
  will be replaced by (all during the first iteration)<br />
  `three this is the macro expansion A this is the macro expansion B this is the macro expansion`


## Long Macros

A long macro can be defined with the syntax:

    LONG MACRO macro_name
      expansion line 1
      expansion line 2
      expansion line 3
    END MACRO

The macro is then invoked with th enormal `macro_name()` syntax.  Leading whitespace/indentation
will be ignored.

### Long Macro Expansion

The output of a single input line will include one line for each combination of macro expansions on
that line.  That is, the number of output lines is multiplicatively related to the number of macro
expansions on that line:

* Only a single long macro invocation per line will be expanded during a single iteration.  This
    means that a line may only contain 10 long macro invocations to stay under the iteration count
    limit.
* A single invocation of a long macro on a single line will cause that line to expand to the number
    of lines of the long macro's expansion.  On each expanded line, the invocation will be replaced
    by the corresponding line of the macro's expansion.

* Example 1:

        a macro_name() invocation

    will be replaced by (in a single iteration)

        a expansion line 1 invocation
        a expansion line 2 invocation
        a expansion line 3 invocation

* Example 2:

        alpha macro_name() beta macro_name()

    will be replaced by (during the first iteration)

        alpha expansion line 1 beta macro_name()
        alpha expansion line 2 beta macro_name()
        alpha expansion line 3 beta macro_name()

    which will be replaced by (during the second iteration)

        alpha expansion line 1 beta expansion line 1
        alpha expansion line 1 beta expansion line 2
        alpha expansion line 1 beta expansion line 3
        alpha expansion line 2 beta expansion line 1
        alpha expansion line 2 beta expansion line 2
        alpha expansion line 2 beta expansion line 3
        alpha expansion line 3 beta expansion line 1
        alpha expansion line 3 beta expansion line 2
        alpha expansion line 3 beta expansion line 3

