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

# Key Character Map Files #

Key character map files (`.kcm` files) are responsible for mapping combinations
of Android key codes with modifiers to Unicode characters.

Device-specific key layout files are *required* for all internal (built-in)
input devices that have keys, if only to tell the system that the device
is special purpose only (not a full keyboard).

Device-specific key layout files are *optional* for external keyboards, and
often aren't needed at all.  The system provides a generic key character map
that is suitable for many external keyboards.

If no device-specific key layout file is available, then the system will
choose a default instead.

## Location ##

Key character map files are located by USB vendor, product (and optionally version)
id or by input device name.

The following paths are consulted in order.

*   `/system/usr/keychars/Vendor_XXXX_Product_XXXX_Version_XXXX.kcm`
*   `/system/usr/keychars/Vendor_XXXX_Product_XXXX.kcm`
*   `/system/usr/keychars/DEVICE_NAME.kcm`
*   `/data/system/devices/keychars/Vendor_XXXX_Product_XXXX_Version_XXXX.kcm`
*   `/data/system/devices/keychars/Vendor_XXXX_Product_XXXX.kcm`
*   `/data/system/devices/keychars/DEVICE_NAME.kcm`
*   `/system/usr/keychars/Generic.kcm`
*   `/data/system/devices/keychars/Generic.kcm`
*   `/system/usr/keychars/Virtual.kcm`
*   `/data/system/devices/keychars/Virtual.kcm`

When constructing a file path that contains the device name, all characters
in the device name other than '0'-'9', 'a'-'z', 'A'-'Z', '-' or '_' are replaced by '_'.

## Generic Key Character Map File ##

The system provides a special built-in key character map file called `Generic.kcm`.
This key character map is intended to support a variety of standard external
keyboards.

*Do not modify the generic key character map!*

## Virtual Key Character Map File ##

The system provides a special built-in key character map file called `Virtual.kcm`
that is used by the virtual keyboard devices.

The virtual keyboard device is a synthetic input device whose id is -1
(see `KeyCharacterMap.VIRTUAL_KEYBOARD`).  It is present on all Android devices
beginning with Android Honeycomb 3.0.  The purpose of the virtual keyboard device
is to provide a known built-in input device that can be used for injecting
keystokes into applications by the IME or by test instrumentation, even
for devices that do not have built-in keyboards.

The virtual keyboard is assumed to have a full QWERTY layout that is the
same on all devices.  This makes it possible for applications to inject
keystrokes using the virtual keyboard device and always get the same results.

*Do not modify the virtual key character map!*

## Syntax ##

A key character map file is a plain text file consisting of a keyboard type
declaration and a set of key declarations.

### Keyboard Type Declaration ###

A keyboard type declaration describes the overall behavior of the keyboard.
A character map file must contain a keyboard type declaration.  For clarity,
it is often placed at the top of the file.

    type FULL

The following keyboard types are recognized:

*   `NUMERIC`: A numeric (12-key) keyboard.

    A numeric keyboard supports text entry using a multi-tap approach.
    It may be necessary to tap a key multiple times to generate the desired letter or symbol.

    This type of keyboard is generally designed for thumb typing.

    Corresponds to `KeyCharacterMap.NUMERIC`.

*   `PREDICTIVE`: A keyboard with all the letters, but with more than one letter per key.

    This type of keyboard is generally designed for thumb typing.

    Corresponds to `KeyCharacterMap.PREDICTIVE`.

*   `ALPHA`: A keyboard with all the letters, and maybe some numbers.

    An alphabetic keyboard supports text entry directly but may have a condensed
    layout with a small form factor.  In contrast to a `FULL` keyboard, some
    symbols may only be accessible using special on-screen character pickers.
    In addition, to improve typing speed and accuracy, the framework provides
    special affordances for alphabetic keyboards such as auto-capitalization
    and toggled / locked SHIFT and ALT keys.

    This type of keyboard is generally designed for thumb typing.

*   `FULL`: A full PC-style keyboard.

    A full keyboard behaves like a PC keyboard.  All symbols are accessed directly
    by pressing keys on the keyboard without on-screen support or affordances such
    as auto-capitalization.

    This type of keyboard is generally designed for full two hand typing.

*   `SPECIAL_FUNCTION`: A keyboard that is only used to perform system control functions
    rather than for typing.

    A special function keyboard consists only of non-printing keys such as
    HOME and POWER that are not actually used for typing.

The `Generic.kcm` and `Virtual.kcm` key character maps are both `FULL` keyboards.

### Key Declarations ###

Key declarations each consist of the keyword `key` followed by an Android key code
name, an open curly brace, a set of properties and behaviors and a close curly brace.

    key A {
        label:                              'A'
        base:                               'a'
        shift, capslock:                    'A'
        ctrl, alt, meta:                    none
    }

#### Properties ####

Each key property establishes a mapping from a key to a behavior.  To make the
key character map files more compact, several properties can be mapped to the
same behavior by separating them with a comma.

In the above example, the `label` property is assigned the `'A'` behavior.
Likewise, the `ctrl`, `alt` and `meta` properties are all simultaneously assigned
the `none` behavior.

The following properties are recognized:

*   `label`: Specifies the label that is physically printed on the key, when it
    consists of a single character.  This is the value that is returned by
    the `KeyCharacterMap.getDisplayLabel` method.

*   `number`: Specifies the behavior (character that should be typed) when a numeric
    text view has focus, such as when the user is typing a phone number.

    Compact keyboards often combine multiple symbols into a single key, such that
    the same key might be used to type `'1'` and `'a'` or `'#'` and `'q'`, perhaps.
    For these keys, the `number` property should be set to indicate which symbol
    should be typed in a numeric context, if any.

    Some typical "numeric" symbols are digits `'0'` through `'9'`, `'#'`, `'+'`,
    `'('`, `')'`, `','`, and `'.'`.

*   `base`: Specifies the behavior (character that should be typed) when no modifiers
    are pressed.

*   &lt;modifier&gt; or &lt;modifier1&gt;`+`&lt;modifier2&gt;`+`...: Specifies the
    behavior (character that should be typed) when the key is pressed and all of the
    specified modifiers are active.

    For example, the modifier property `shift` specifies a behavior that applies when
    the either the LEFT SHIFT or RIGHT SHIFT modifier is pressed.

    Similarly, the modifier property `rshift+ralt` specifies a behavior that applies
    when the both RIGHT SHIFT and RIGHT ALT modifiers are pressed together.

The following modifiers are recognized in modifier properties:

*   `shift`: Applies when either the LEFT SHIFT or RIGHT SHIFT modifier is pressed.
*   `lshift`: Applies when the LEFT SHIFT modifier is pressed.
*   `rshift`: Applies when the RIGHT SHIFT modifier is pressed.
*   `alt`: Applies when either the LEFT ALT or RIGHT ALT modifier is pressed.
*   `lalt`: Applies when the LEFT ALT modifier is pressed.
*   `ralt`: Applies when the RIGHT ALT modifier is pressed.
*   `ctrl`: Applies when either the LEFT CONTROL or RIGHT CONTROL modifier is pressed.
*   `lctrl`: Applies when the LEFT CONTROL modifier is pressed.
*   `rctrl`: Applies when the RIGHT CONTROL modifier is pressed.
*   `meta`: Applies when either the LEFT META or RIGHT META modifier is pressed.
*   `lmeta`: Applies when the LEFT META modifier is pressed.
*   `rmeta`: Applies when the RIGHT META modifier is pressed.
*   `sym`: Applies when the SYMBOL modifier is pressed.
*   `fn`: Applies when the FUNCTION modifier is pressed.
*   `capslock`: Applies when the CAPS LOCK modifier is locked.
*   `numlock`: Applies when the NUM LOCK modifier is locked.
*   `scrolllock`: Applies when the SCROLL LOCK modifier is locked.

The order in which the properties are listed is significant.  When mapping a key to
a behavior, the system scans all relevant properties in order and returns the last
applicable behavior that it found.

Consequently, properties that are specified later override properties that are
specified earlier for a given key.

#### Behaviors ####

Each property maps to a behavior.  The most common behavior is typing a character
but there are others.

The following behaviors are recognized:

*   `none`: Don't type a character.

    This behavior is the default when no character is specified.  Specifying `none`
    is optional but it improves clarity.

*   `'X'`: Type the specified character literal.

    This behavior causes the specified character to be entered into the focused
    text view.  The character literal may be any ASCII character, or one of the
    following escape sequences:

    *   `'\\'`: Type a backslash character.
    *   `'\n'`: Type a new line character (use this for ENTER / RETURN).
    *   `'\t'`: Type a TAB character.
    *   `'\''`: Type an apostrophe character.
    *   `'\"'`: Type a quote character.
    *   `'\uXXXX'`: Type the Unicode character whose code point is given in hex by XXXX.

*   `fallback` &lt;Android key code name&gt;: Perform a default action if the key is not
    handled by the application.

    This behavior causes the system to simulate a different key press when an application
    does not handle the specified key natively.  It is used to support default behavior
    for new keys that not all applications know how to handle, such as ESCAPE or
    numeric keypad keys (when numlock is not pressed).

    When a fallback behavior is performed, the application will receive two key presses:
    one for the original key and another for the fallback key that was selected.
    If the application handles the original key during key up, then the fallback key
    event will be canceled (`KeyEvent.isCanceled` will return `true`).

The system reserves two Unicode characters to perform special functions:

*   `'\uef00'`: When this behavior is performed, the text view consumes and removes the
    four characters preceding the cursor, interprets them as hex digits, and inserts the
    corresponding Unicode code point.

*   `'\uef01'`: When this behavior is performed, the text view displays a
    character picker dialog that contains miscellaneous symbols.

The system recognizes the following Unicode characters as combining diacritical dead
key characters:

*   `'\u0300'`: Grave accent.
*   `'\u0301'`: Acute accent.
*   `'\u0302'`: Circumflex accent.
*   `'\u0303'`: Tilde accent.
*   `'\u0308'`: Umlaut accent.

When a dead key is typed followed by another character, the dead key and the following
characters are composed.  For example, when the user types a grave accent dead
key followed by the letter 'a', the result is '&agrave;'.

Refer to `KeyCharacterMap.getDeadChar` for more information about dead key handling.

### Comments ###

Comment lines begin with '#' and continue to the end of the line.  Like this:

    # A comment!

Blank lines are ignored.

### How Key Combinations are Mapped to Behaviors ###

When the user presses a key, the system looks up the behavior associated with
the combination of that key press and the currently pressed modifiers.

#### SHIFT + A ####

Suppose the user pressed A and SHIFT together.  The system first locates
the set of properties and behaviors associated with `KEYCODE_A`.

    key A {
        label:                              'A'
        base:                               'a'
        shift, capslock:                    'A'
        ctrl, alt, meta:                    none
    }

The system scans the properties from first to last and left to right, ignoring
the `label` and `number` properties, which are special.

The first property encountered is `base`.  The `base` property always applies to
a key, no matter what modifiers are pressed.  It essentially specifies the default
behavior for the key unless it is overridden by following properties.
Since the `base` property applies to this key press, the system makes note
of the fact that its behavior is `'a'` (type the character `a`).

The system then continues to scan subsequent properties in case any of them
are more specific than `base` and override it.  It encounters `shift` which
also applies to the key press SHIFT + A.  So the system decides to ignore
the `base` property's behavior and chooses the behavior associated with
the `shift` property, which is `'A'` (type the character `A`).

It then continues to scan the table, however no other properties apply to this
key press (CAPS LOCK is not locked, neither CONTROL key is pressed, neither
ALT key is pressed and neither META key is pressed).

So the resulting behavior for the key combination SHIFT + A is `'A'`.

#### CONTROL + A ####

Now consider what would happen if the user pressed A and CONTROL together.

As before, the system would scan the table of properties.  It would notice
that the `base` property applied but would also continue scanning until
it eventually reached the `control` property.  As it happens, the `control`
property appears after `base` so its behavior overrides the `base` behavior.

So the resulting behavior for the key combination CONTROL + A is `none`.

#### ESCAPE ####

Now suppose the user pressed ESCAPE.

    key ESCAPE {
        base:                               fallback BACK
        alt, meta:                          fallback HOME
        ctrl:                               fallback MENU
    }

This time the system obtains the behavior `fallback BACK`, a fallback behavior.
Because no character literal appears, no character will be typed.

When processing the key, the system will first deliver `KEYCODE_ESCAPE` to the
application.  If the application does not handle it, then the system will try
again but this time it will deliver `KEYCODE_BACK` to the application as
requested by the fallback behavior.

So applications that recognize and support `KEYCODE_ESCAPE` have the
opportunity to handle it as is, but other applications that do not can instead
perform the fallback action of treating the key as if it were `KEYCODE_BACK`.

#### NUMPAD_0 with or without NUM LOCK ####

The numeric keypad keys have very different interpretations depending on whether
the NUM LOCK key is locked.

The following key declaration ensures that `KEYCODE_NUMPAD_0` types `0`
when NUM LOCK is pressed.  When NUM LOCK is not pressed, the key is delivered
to the application as usual, and if it is not handled, then the fallback
key `KEYCODE_INSERT` is delivered instead.

    key NUMPAD_0 {
        label, number:                      '0'
        base:                               fallback INSERT
        numlock:                            '0'
        ctrl, alt, meta:                    none
    }

As we can see, fallback key declarations greatly improve compatibility
with older applications that do not recognize or directly support all of the keys
that are present on a full PC style keyboard.

### Examples ###

#### Full Keyboard ####

    # This is an example of part of a key character map file for a full keyboard
    # include a few fallback behaviors for special keys that few applications
    # handle themselves.

    type FULL

    key C {
        label:                              'C'
        base:                               'c'
        shift, capslock:                    'C'
        alt:                                '\u00e7'
        shift+alt:                          '\u00c7'
        ctrl, meta:                         none
    }
    
    key SPACE {
        label:                              ' '
        base:                               ' '
        ctrl:                               none
        alt, meta:                          fallback SEARCH
    }
    
    key NUMPAD_9 {
        label, number:                      '9'
        base:                               fallback PAGE_UP
        numlock:                            '9'
        ctrl, alt, meta:                    none
    }

#### Alphanumeric Keyboard ####

    # This is an example of part of a key character map file for an alphanumeric
    # thumb keyboard.  Some keys are combined, such as `A` and `2`.  Here we
    # specify `number` labels to tell the system what to do when the user is
    # typing a number into a dial pad.
    #
    # Also note the special character '\uef01' mapped to ALT+SPACE.
    # Pressing this combination of keys invokes an on-screen character picker.

    type ALPHA
    
    key A {
        label:                              'A'
        number:                             '2'
        base:                               'a'
        shift, capslock:                    'A'
        alt:                                '#'
        shift+alt, capslock+alt:            none
    }

    key SPACE {
        label:                              ' '
        number:                             ' '
        base:                               ' '
        shift:                              ' '
        alt:                                '\uef01'
        shift+alt:                          '\uef01'
    }

#### Game Pad ####

    # This is an example of part of a key character map file for a game pad.
    # It defines fallback actions that enable the user to navigate the user interface
    # by pressing buttons.

    type SPECIAL_FUNCTION

    key BUTTON_A {
        base:                               fallback BACK
    }

    key BUTTON_X {
        base:                               fallback DPAD_CENTER
    }

    key BUTTON_START {
        base:                               fallback HOME
    }

    key BUTTON_SELECT {
        base:                               fallback MENU
    }

## Compatibility Note ##

Prior to Android Honeycomb 3.0, the Android key character map was specified
using a very different syntax and was compiled into a binary file format
(`.kcm.bin`) at build time.

Although the new format uses the same extension `.kcm`, the syntax is quite
different (and much more powerful).

As of Android Honeycomb 3.0, all Android key character map files must use
the new syntax and plain text file format that is described in this document.
The old syntax is not supported and the old `.kcm.bin` files are not recognized
by the system.

## Language Note ##

Android does not currently support multilingual keyboards.  Moreover, the
built-in generic key character map assumes a US English keyboard layout.

OEMs are encouraged to provide custom key character maps for their keyboards
if they are designed for other languages.

Future versions of Android may provide better support for multilingual keyboards
or user-selectable keyboard layouts.

## Validation ##

Make sure to validate your key character map files using the
[Validate Keymaps](/tech/input/validate-keymaps.html) tool.
