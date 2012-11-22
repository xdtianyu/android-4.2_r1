#!/usr/bin/python2.4
#
# Copyright 2007 The Android Open Source Project

"""Dump Quake ms2 files.

Useful for debugging Quake.
"""

# ms2 file format
# int32 numcommands
# int32 numorder
# int32 commands[numcommands]
# int32 vertexorder[numorder]
#
# Where a command is
#
#    >= 0 --> strip(n)
#    < 0  --> fan(-n)
# followed by struct { float s; float t; } st[n];

import array
import sys

def readInt32(f):
	a = array.array('i')
	a.read(f, 1)
	return a[0]

def readFloat32(f):
	a = array.array('f')
	a.read(f, 1)
	return a[0]

def dumpms2(path):
	f = open(path, "rw")
	numCommands = readInt32(f)
	numOrder = readInt32(f)
	commandIndex = 0
	
	# Seek ahead and read the vertex order information
	f.seek(4 + 4 + 4 * numCommands)
	vertexOrder = array.array('i')
	vertexOrder.read(f, numOrder)
	
	# Read commands
	f.seek(4 + 4)
	vertexOrderIndex = 0
	
	while commandIndex < numCommands:
		cmd = readInt32(f)
		commandIndex = commandIndex + 1
		if cmd == 0:
			break
		elif(cmd > 0):
			# strip
			print "strip ", cmd
			for i in range(cmd):
				s = readFloat32(f)
				t = readFloat32(f)
				print "[", i, "] ", vertexOrder[vertexOrderIndex], \
					" (", s, ",", t, ")"
				commandIndex += 2
				vertexOrderIndex += 1
		else:
			# fan
			print "fan ", -cmd
			for i in range(-cmd):
				s = readFloat32(f)
				t = readFloat32(f)
				print "[", i, "] ", vertexOrder[vertexOrderIndex], \
					" (", s, ",", t, ")"
				commandIndex += 2
				vertexOrderIndex += 1
	
	f.close()

if __name__ == '__main__': 
	dumpms2(sys.argv[1])
