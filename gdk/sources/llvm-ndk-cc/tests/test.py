#!/usr/bin/env python3
# -*- coding:utf-8 -*-
"""llvm-ndk-cc Toolchains Test.
"""

import filecmp
import glob
import os
import subprocess
import sys

__author__ = 'Nowar Gu'

class Options(object):
  Verbose = 0
  Cleanup = 1

def compareFile(Filename):
  """Compare Filename and Filename.expect for equality."""
  Actual = Filename
  Expect = Filename + '.expect'

  if not os.path.isfile(Actual):
    if Options.Verbose:
      print('Could not find {0}'.format(Actual))
    return False
  if not os.path.isfile(Expect):
    if Options.Verbose:
      print('Could not find {0}'.format(Expect))
    return False

  return filecmp.cmp(Actual, Expect, False)


def runFileTest(Filename):
  """Run test on each file in directory."""
  Passed = True

  if Options.Verbose != 0:
    print('\nTesting {0}'.format(Filename))

  Cmd = ('../../../../../out/host/linux-x86/bin/llvm-ndk-cc'
         ' -I../../../../platforms/android-100/arch-llvm/usr/include'
         ' -I{0}'.format(os.path.dirname(os.path.realpath(Filename))))

  BaseArgs = Cmd.split()
  FilenameStubs = Filename.split('.')
  FileBasename = FilenameStubs[0]
  Args = BaseArgs
  Args.append(Filename)
  Args.append('-o')
  Args.append(FileBasename + '.bc')

  StdoutFile = open('{0}.stdout.txt'.format(FileBasename), 'w+')
  StderrFile = open('{0}.stderr.txt'.format(FileBasename), 'w+')

  if Options.Verbose > 1:
    print('Executing:',end=' ')
    for Arg in Args:
      print(Arg,end=' ')
    print()
  Ret = subprocess.call(Args, stdout=StdoutFile, stderr=StderrFile)
  StdoutFile.flush()
  StderrFile.flush()
  if Options.Verbose > 1:
    StdoutFile.seek(0)
    StderrFile.seek(0)
    for Line in StdoutFile:
      print('STDOUT> {0}'.format(Line), end='')
    for Line in StderrFile:
      print('STDERR> {0}'.format(Line), end='')

  StdoutFile.close()
  StderrFile.close()
  return Ret == 0


def runTest(Dirname):
  """Run an llvm-ndk-cc test from Dirname."""
  Passed = True
  os.chdir(Dirname)

  SrcFiles = glob.glob('*.c')
  SrcFiles.extend(glob.glob('*.cpp'))
  SrcFiles.sort()
  FilesPassed = []
  ResultPassed = True
  for File in SrcFiles:
    FileRunPassed = runFileTest(File)
    FileBasename = File.split('.')[0]
    if not compareFile('{0}.stdout.txt'.format(FileBasename)):
      ResultPassed = False
      if Options.Verbose:
        print('stdout is different.')
    if not compareFile('{0}.stderr.txt'.format(FileBasename)):
      ResultPassed = False
      if Options.Verbose:
        print('stderr is different.')
    FilesPassed.append(FileRunPassed)

  Result = True
  for FilePassed in FilesPassed:
    Result = Result and FilePassed

  if Dirname[0:2] == 'F_':
    if Result == True:
      Result = False
      if Options.Verbose:
        print('Command passed on invalid input.')
    else:
      Result = True
  elif Dirname[0:2] == 'P_':
    if Result == False:
      if Options.Verbose:
        print('Command failed on valid input.')
  else:
    if Options.Verbose:
      print('Test directory name should start with an F or a P.')

  Cmd = ('../../../../../out/host/linux-x86/bin/llvm-ndk-link')
  Args = Cmd.split()
  Args.extend(glob.glob('*.bc'))
  Args.append('-o')
  Args.append('AllFilesLinked.bc')
  if Result == True:
    Ret = subprocess.call(Args)
    if Ret != 0:
      Result = False;

    if Options.Verbose > 1:
      print()
      print('Executing:',end=' ')
      for Arg in Args:
        print(Arg,end=' ')
      print()


  if Options.Cleanup:
    for File in glob.glob('*.stdout.txt'):
      os.remove(File)
    for File in glob.glob('*.stderr.txt'):
      os.remove(File)
    for File in glob.glob('*.bc'):
      os.remove(File)

  os.chdir('..')
  if Dirname[0:2] == 'F_':
      return not Result and ResultPassed
  else:
    return Result and ResultPassed


def showUsage():
  print('Usage: {0} [Option]... [directory]...'.format(sys.argv[0]))
  print('llvm-ndk-cc Toolchains Test')
  print('  -h, --help       Help message')
  print('  -n, --no-cleanup Do not cleanup after testing')
  print('  -v, --verbose    Verbose output')
  return


def main():
  Passed = 0
  Failed = 0
  Files = []
  FailedTests = []

  for Arg in sys.argv[1:]:
    if Arg in ['-h', '--help']:
      showUsage()
      return 0
    elif Arg in ['-n', '--no-cleanup']:
      Options.Cleanup = 0
    elif Arg in ['-v', '--verbose']:
      Options.Verbose += 1
    else:
      if os.path.isdir(Arg):
        Files.append(Arg)
      else:
        print('Invalid test or options: {0}'.format(Arg), file=sys.stderr)
        return 1

  if not Files:
    TmpFiles = os.listdir('.')
    for File in TmpFiles:
      if os.path.isdir(File) and (File[0:2] == 'F_' or File[0:2] == 'P_'):
        Files.append(File)

  for Dir in Files:
    if os.path.isdir(Dir):
      if runTest(Dir):
        Passed += 1
      else:
        Failed += 1
        FailedTests.append(Dir)

  print()
  print('Tests Passed: {0}'.format(Passed))
  print('Tests Failed: {0}'.format(Failed))
  if Failed:
    print('Failures:', end=' ')
    for Test in FailedTests:
      print(Test, end=' ')
    print()

  return Failed != 0


if __name__ == '__main__':
  sys.exit(main())
