#
# Copyright (C) 2010 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#
# Test the bcc compiler

import unittest
import subprocess
import os
import sys

gArmInitialized = False
gUseArm = True
gUseX86 = True
gRunOTCCOutput = True


def parseArgv():
    global gUseArm
    global gUseX86
    global gRunOTCCOutput
    for arg in sys.argv[1:]:
        if arg == "--noarm":
            print "--noarm: not testing ARM"
            gUseArm = False
        elif arg == "--nox86":
            print "--nox86: not testing x86"
            gUseX86 = False
        elif arg == "--norunotcc":
            print "--norunotcc detected, not running OTCC output"
            gRunOTCCOutput = False
        else:
            print "Unknown parameter: ", arg
            raise "Unknown parameter"
    sys.argv = sys.argv[0:1]

def compile(args):
    proc = subprocess.Popen(["../libbcc_driver"] + args, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
    result = proc.communicate()
    return result

def runCmd(args):
    proc = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    result = proc.communicate()
    return result[0].strip()

def uname():
    return runCmd(["uname"])

def unameM():
    return runCmd(["uname", "-m"])

def which(item):
    return runCmd(["which", item])

def fileType(item):
    return runCmd(["file", item])

def outputCanRun():
    ft = fileType(which("bcc"))
    return ft.find("ELF 32-bit LSB executable, Intel 80386") >= 0

def checkEnvironment():
    global gRunOTCCOutput
    gRunOTCCOutput = uname() == "Linux" and unameM() != "x86_64" and outputCanRun()

def adb(args):
    return runCmd(["adb"] + args)

def setupArm():
    global gArmInitialized
    if gArmInitialized:
        return
    print "Setting up arm"
    adb(["remount"])
    adb(["shell", "rm", "/system/bin/bcc"])
    adb(["shell", "mkdir", "/system/bin/bccdata"])
    adb(["shell", "mkdir", "/system/bin/bccdata/data"])
    # Clear out old data TODO: handle recursion
    adb(["shell", "rm", "/system/bin/bccdata/data/*"])
    # Copy over data
    for root, dirs, files in os.walk("data"):
        for d in dirs:
            adb(["shell", "mkdir", os.path.join(root, d)])
        for f in files:
            adb(["push", os.path.join(root, f), os.path.join("/system/bin/bccdata", root, f)])
    # Copy over compiler
    adb(["sync"])
    gArmInitialized = True

def compileArm(args):
    setupArm()
    proc = subprocess.Popen(["adb", "shell", "/system/bin/bcc"] + args, stdout=subprocess.PIPE)
    result = proc.communicate()
    return result[0].replace("\r","")

def compare(a, b):
    if a != b:
        firstDiff = firstDifference(a, b)
        print "Strings differ at character %d. Common: %s. Difference '%s' != '%s'" % (
            firstDiff, a[0:firstDiff], safeAccess(a, firstDiff), safeAccess(b, firstDiff))

def safeAccess(s, i):
    if 0 <= i < len(s):
        return s[i]
    else:
        return '?'

def firstDifference(a, b):
    commonLen = min(len(a), len(b))
    for i in xrange(0, commonLen):
        if a[i] != b[i]:
            return i
    return commonLen

# a1 and a2 are the expected stdout and stderr.
# b1 and b2 are the actual stdout and stderr.
# Compare the two, sets. Allow any individual line
# to appear in either stdout or stderr. This is because
# the way we obtain output on the ARM combines both
# streams into one sequence.

def compareOuput(a1,a2,b1,b2):
    while True:
        totalLen = len(a1) + len(a2) + len(b1) + len(b2)
        a1, b1 = matchCommon(a1, b1)
        a1, b2 = matchCommon(a1, b2)
        a2, b1 = matchCommon(a2, b1)
        a2, b2 = matchCommon(a2, b2)
        newTotalLen = len(a1) + len(a2) + len(b1) + len(b2)
        if newTotalLen == 0:
            return True
        if newTotalLen == totalLen:
            print "Failed at %d %d %d %d" % (len(a1), len(a2), len(b1), len(b2))
            print "a1", a1
            print "a2", a2
            print "b1", b1
            print "b2", b2
            return False

def matchCommon(a, b):
    """Remove common items from the beginning of a and b,
       return just the tails that are different."""
    while len(a) > 0 and len(b) > 0 and a[0] == b[0]:
        a = a[1:]
        b = b[1:]
    return a, b

def rewritePaths(args):
    return [rewritePath(x) for x in args]

def rewritePath(p):
    """Take a path that's correct on the x86 and convert to a path
       that's correct on ARM."""
    if p.startswith("data/"):
        p = "/system/bin/bccdata/" + p
    return p

class TestACC(unittest.TestCase):

    def checkResult(self, out, err, stdErrResult, stdOutResult=""):
        a1 = out.splitlines()
        a2 = err.splitlines()
        b2 = stdErrResult.splitlines()
        b1 = stdOutResult.splitlines()
        self.assertEqual(True, compareOuput(a1,a2,b1,b2))

    def compileCheck(self, args, stdErrResult, stdOutResult="",
                     targets=['arm', 'x86']):
        global gUseArm
        global gUseX86
        targetSet = frozenset(targets)
        if gUseX86 and 'x86' in targetSet:
            print args
            out, err = compile(args)
            self.checkResult(out, err, stdErrResult, stdOutResult)
        if gUseArm and 'arm' in targetSet:
            out = compileArm(rewritePaths(args))
            self.checkResult(out, "", stdErrResult, stdOutResult)

    def compileCheckArm(self, args, result):
        self.assertEqual(compileArm(args), result)

    def testCompileReturnVal(self):
        self.compileCheck(["data/returnval-ansi.bc"], "")

    def testCompileOTCCANSII(self):
        self.compileCheck(["data/otcc-ansi.bc"], "", "", ['x86'])

    def testRunReturnVal(self):
        self.compileCheck(["-c -R", "data/returnval-ansi.bc"],
        "Executing compiled code:\nresult: 42\n")

    def testStringLiteralConcatenation(self):
        self.compileCheck(["-c -R", "data/testStringConcat.bc"],
        "Executing compiled code:\nresult: 13\n", "Hello, world\n")

    def testRunOTCCANSI(self):
        global gRunOTCCOutput
        if gRunOTCCOutput:
            self.compileCheck(["-c -R", "data/otcc-ansi.bc", "data/returnval.c"],
                "Executing compiled code:\notcc-ansi.c: About to execute compiled code:\natcc-ansi.c: result: 42\nresult: 42\n", "",
                 ['x86'])

    def testRunOTCCANSI2(self):
        global gRunOTCCOutput
        if gRunOTCCOutput:
            self.compileCheck(["-c -R", "data/otcc-ansi.bc", "data/otcc.c", "data/returnval.c"],
                "Executing compiled code:\notcc-ansi.c: About to execute compiled code:\notcc.c: about to execute compiled code.\natcc-ansi.c: result: 42\nresult: 42\n", "",['x86'])

    def testRunConstants(self):
        self.compileCheck(["-c -R", "data/constants.bc"],
            "Executing compiled code:\nresult: 0\n",
            "0 = 0\n010 = 8\n0x10 = 16\n'\\a' = 7\n'\\b' = 8\n'\\f' = 12\n'\\n' = 10\n'\\r' = 13\n'\\t' = 9\n'\\v' = 11\n'\\\\' = 92\n'\\'' = 39\n" +
            "'\\\"' = 34\n'\\?' = 63\n'\\0' = 0\n'\\1' = 1\n'\\12' = 10\n'\\123' = 83\n'\\x0' = 0\n'\\x1' = 1\n'\\x12' = 18\n'\\x123' = 35\n'\\x1f' = 31\n'\\x1F' = 31\n")

    def testRunFloat(self):
        self.compileCheck(["-c -R", "data/float.bc"],
            "Executing compiled code:\nresult: 0\n",
            """Constants: 0 0 0 0.01 0.01 0.1 10 10 0.1
int: 1 float: 2.2 double: 3.3
 ftoi(1.4f)=1
 dtoi(2.4)=2
 itof(3)=3
 itod(4)=4
globals: 1 2 3 4
args: 1 2 3 4
locals: 1 2 3 4
cast rval: 2 4
cast lval: 1.1 2 3.3 4
""")

    def testRunFlops(self):
        self.compileCheck(["-c -R", "data/flops.bc"],
            """Executing compiled code:
result: 0""",
"""-1.1 = -1.1
!1.2 = 0
!0 = 1
double op double:
1 + 2 = 3
1 - 2 = -1
1 * 2 = 2
1 / 2 = 0.5
float op float:
1 + 2 = 3
1 - 2 = -1
1 * 2 = 2
1 / 2 = 0.5
double op float:
1 + 2 = 3
1 - 2 = -1
1 * 2 = 2
1 / 2 = 0.5
double op int:
1 + 2 = 3
1 - 2 = -1
1 * 2 = 2
1 / 2 = 0.5
int op double:
1 + 2 = 3
1 - 2 = -1
1 * 2 = 2
1 / 2 = 0.5
double op double:
1 op 2: < 1   <= 1   == 0   >= 0   > 0   != 1
1 op 1: < 0   <= 1   == 1   >= 1   > 0   != 0
2 op 1: < 0   <= 0   == 0   >= 1   > 1   != 1
double op float:
1 op 2: < 1   <= 1   == 0   >= 0   > 0   != 1
1 op 1: < 0   <= 1   == 1   >= 1   > 0   != 0
2 op 1: < 0   <= 0   == 0   >= 1   > 1   != 1
float op float:
1 op 2: < 1   <= 1   == 0   >= 0   > 0   != 1
1 op 1: < 0   <= 1   == 1   >= 1   > 0   != 0
2 op 1: < 0   <= 0   == 0   >= 1   > 1   != 1
int op double:
1 op 2: < 1   <= 1   == 0   >= 0   > 0   != 1
1 op 1: < 0   <= 1   == 1   >= 1   > 0   != 0
2 op 1: < 0   <= 0   == 0   >= 1   > 1   != 1
double op int:
1 op 2: < 1   <= 1   == 0   >= 0   > 0   != 1
1 op 1: < 0   <= 1   == 1   >= 1   > 0   != 0
2 op 1: < 0   <= 0   == 0   >= 1   > 1   != 1
branching: 1 0 1
testpassi: 1 2 3 4 5 6 7 8 9 10 11 12
testpassf: 1 2 3 4 5 6 7 8 9 10 11 12
testpassd: 1 2 3 4 5 6 7 8 9 10 11 12
testpassi: 1 2 3 4 5 6 7 8 9 10 11 12
testpassf: 1 2 3 4 5 6 7 8 9 10 11 12
testpassd: 1 2 3 4 5 6 7 8 9 10 11 12
testpassi: 1 2 3 4 5 6 7 8 9 10 11 12
testpassf: 1 2 3 4 5 6 7 8 9 10 11 12
testpassd: 1 2 3 4 5 6 7 8 9 10 11 12
testpassidf: 1 2 3
""")
    def testCasts(self):
        self.compileCheck(["-c -R", "data/casts.bc"],
            """Executing compiled code:
result: 0""", """Reading from a pointer: 3 3
Writing to a pointer: 4
Testing casts: 3 3 4.5 4
Testing reading (int*): 4
Testing writing (int*): 8 9
Testing reading (char*): 0x78 0x56 0x34 0x12
Testing writing (char*): 0x87654321
f(10)
Function pointer result: 70
Testing read/write (float*): 8.8 9.9
Testing read/write (double*): 8.8 9.9
""")

    def testChar(self):
        self.compileCheck(["-c -R", "data/char.bc"], """Executing compiled code:
result: 0""", """a = 99, b = 41
ga = 100, gb = 44""")

    def testPointerArithmetic(self):
        self.compileCheck(["-c -R", "data/pointers.bc"], """Executing compiled code:
result: 0""", """Pointer difference: 1 4
Pointer addition: 2
Pointer comparison to zero: 0 0 1
Pointer comparison: 1 0 0 0 1
""")
    def testRollo3(self):
        self.compileCheck(["-c -R", "data/rollo3.bc"], """Executing compiled code:
result: 10""", """""")

    def testFloatDouble(self):
        self.compileCheck(["-c -R", "data/floatdouble.bc"], """Executing compiled code:
result: 0""", """0.002 0.1 10""")

    def testIncDec(self):
        self.compileCheck(["-c -R", "data/inc.bc"], """Executing compiled code:
0
1
2
1
1
2
1
0
result: 0
""","""""")

    def testIops(self):
        self.compileCheck(["-c -R", "data/iops.bc"], """Executing compiled code:
result: 0""", """Literals: 1 -1
++
0
1
2
3
4
5
6
7
8
9
--
10
9
8
7
6
5
4
3
2
1
0
""")

    def testFilm(self):
        self.compileCheck(["-c -R", "data/film.bc"], """Executing compiled code:
result: 0""", """testing...
Total bad: 0
""")

    def testpointers2(self):
        self.compileCheck(["-c -R", "data/pointers2.bc"], """Executing compiled code:
result: 0""", """a = 0, *pa = 0
a = 2, *pa = 2
a = 0, *pa = 0 **ppa = 0
a = 2, *pa = 2 **ppa = 2
a = 0, *pa = 0 **ppa = 0
a = 2, *pa = 2 **ppa = 2
""")

    def testassignmentop(self):
        self.compileCheck(["-c -R", "data/assignmentop.bc"], """Executing compiled code:
result: 0""", """2 *= 5  10
20 /= 5  4
17 %= 5  2
17 += 5  22
17 -= 5  12
17<<= 1  34
17>>= 1  8
17&= 1  1
17^= 1  16
16|= 1  17
*f() = *f() + 10;
f()
f()
a = 10
*f() += 10;
f()
a = 10
""")

    def testcomma(self):
        self.compileCheck(["-c -R", "data/comma.bc"], """Executing compiled code:
result: 0""", """statement: 10
if: a = 0
while: b = 11
for: b = 22
return: 30
arg: 12
""")

    def testBrackets(self):
        self.compileCheck(["-c -R", "data/brackets.bc"], """Executing compiled code:
Errors: 0
2D Errors: 0
result: 0
""","""""")

    def testShort(self):
        self.compileCheck(["-c -R", "data/short.bc"], """Executing compiled code:
result: -2
""","""""")

    def testAssignment(self):
        self.compileCheck(["-c -R", "data/assignment.bc"], """Executing compiled code:
result: 7
""","""""")

    def testArray(self):
        self.compileCheck(["-c -R", "data/array.bc"], """Executing compiled code:
localInt: 3
localDouble: 3 3
globalChar: 3
globalDouble: 3
testArgs: 0 2 4
testDecay: Hi!
test2D:
abcdefghijklmnopabcd
defghijklmnopabcdefg
ghijklmnopabcdefghij
jklmnopabcdefghijklm
mnopabcdefghijklmnop
pabcdefghijklmnopabc
cdefghijklmnopabcdef
fghijklmnopabcdefghi
ijklmnopabcdefghijkl
lmnopabcdefghijklmno
result: 0
""","""""")

    def testDefines(self):
        self.compileCheck(["-c -R", "data/defines.bc"], """Executing compiled code:
result: 3
""","""""")

    def testFuncArgs(self):
        self.compileCheck(["-c -R", "data/funcargs.bc"], """Executing compiled code:
result: 4
""","""""")

    def testB2071670(self):
        self.compileCheck(["-c -R", "data/b2071670.bc"], """Executing compiled code:
result: 1092616192
""","""""")

    def testStructs(self):
        self.compileCheck(["-c -R", "data/structs.bc"], """Executing compiled code:
testCopying: 37 == 37
testUnion: 1 == 0x3f800000
testArgs: (6, 8, 10, 12)
result: 6
""","""""")

    def testAddressOf(self):
        self.compileCheck(["-c -R", "data/addressOf.bc"], """Executing compiled code:
testStruct: 10 10 10
testArray: 1 1 1
result: 0
""","""""")

def main():
    checkEnvironment()
    parseArgv()
    unittest.main()

if __name__ == '__main__':
    main()

