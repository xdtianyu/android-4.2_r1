#!/usr/bin/env python
#
# Copyright (C) 2012 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the 'License');
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an 'AS IS' BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
import csv
import os
import re
import subprocess
import sys
from xml.dom import Node
from xml.dom import minidom


def getChildrenWithTag(parent, tagName):
    children = []
    for child in  parent.childNodes:
        if (child.nodeType == Node.ELEMENT_NODE) and (child.tagName == tagName):
            #print "parent " + parent.getAttribute("name") + " " + tagName +\
            #    " " + child.getAttribute("name")
            children.append(child)
    return children

class TestCase(object):
    def __init__(self, name, average, stddev, passFail):
        self.name = name
        self.average = average
        self.stddev = stddev
        self.passFail = passFail

    def getName(self):
        return self.name

    def getStddev(self):
        return self.stddev

    def getAverage(self):
        return self.average

    def getPassFail(self):
        return self.passFail

def parseSuite(suite, parentName):
    if parentName != "":
        parentName += '.'
    cases = {}
    childSuites = getChildrenWithTag(suite, "TestSuite")
    for child in childSuites:
        cases.update(parseSuite(child, parentName + child.getAttribute("name")))
    childTestCases = getChildrenWithTag(suite, "TestCase")
    for child in childTestCases:
        className = parentName + child.getAttribute("name")
        for test in getChildrenWithTag(child, "Test"):
            methodName = test.getAttribute("name")
            # do not include this
            if methodName == "testAndroidTestCaseSetupProperly":
                continue
            caseName = className + "#" + methodName
            passFail = test.getAttribute("result")
            average = ""
            stddev = ""
            failedScene = getChildrenWithTag(test, "FailedScene")
            if len(failedScene) > 0:
                message = failedScene[0].getAttribute("message")
                #print message
                messages = message.split('|')
                if len(messages) > 2:
                    average = messages[1].split()[1]
                    stddev = messages[2].split()[1]
            testCase = TestCase(caseName, average, stddev, passFail)
            cases[caseName] = testCase
    return cases


class Result(object):
    def __init__(self, reportXml):
        self.results = {}
        self.infoKeys = []
        self.infoValues = []
        doc = minidom.parse(reportXml)
        testResult = doc.getElementsByTagName("TestResult")[0]
        buildInfo = testResult.getElementsByTagName("BuildInfo")[0]
        buildId = buildInfo.getAttribute("buildID")
        deviceId = buildInfo.getAttribute("deviceID")
        deviceName = buildInfo.getAttribute("build_device")
        boardName = buildInfo.getAttribute("build_board")
        partitions = buildInfo.getAttribute("partitions")
        m = re.search(r'.*;/data\s+(\w+)\s+(\w+)\s+(\w+)\s+(\w+);', partitions)
        dataPartitionSize = m.group(1)
        self.addKV("device", deviceName)
        self.addKV("board", boardName)
        self.addKV("serial", deviceId)
        self.addKV("build", buildId)
        self.addKV("data size", dataPartitionSize)
        packages = getChildrenWithTag(testResult, "TestPackage")
        for package in packages:
            casesFromChild = parseSuite(package, "")
            self.results.update(casesFromChild)
        #print self.results.keys()

    def addKV(self, key, value):
        self.infoKeys.append(key)
        self.infoValues.append(value)

    def getResults(self):
        return self.results

    def getKeys(self):
        return self.infoKeys

    def getValues(self):
        return self.infoValues

def executeWithResult(command):
    p = subprocess.Popen(command.split(), stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    out, err = p.communicate()
    return out

def main(argv):
    if len(argv) < 3:
        print "get_csv_report.py pts_report_dir output_file"
        sys.exit(1)
    reportPath = os.path.abspath(argv[1])
    outputCsv = os.path.abspath(argv[2])

    deviceResults = []
    xmls = executeWithResult("find " + reportPath + " -name testResult.xml -print")
    print "xml files found :"
    print xmls
    for xml in xmls.splitlines():
        result = Result(xml)
        deviceResults.append(result)
    reportInfo = []
    keys = deviceResults[0].getKeys()
    noDevices = len(deviceResults)
    for i in xrange(len(keys)):
        reportInfo.append([])
        reportInfo[i].append(keys[i])
        # for worst/average
        reportInfo[i].append("")
        for j in xrange(noDevices):
            reportInfo[i].append(deviceResults[j].getValues()[i])
    #print reportInfo

    tests = []
    for deviceResult in deviceResults:
        for key in deviceResult.getResults().keys():
            if not key in tests:
                tests.append(key)
    tests.sort()
    #print tests

    reportTests = []
    for i in xrange(len(tests)):
        reportTests.append([])
        reportTests.append([])
        reportTests[2 * i].append(tests[i])
        reportTests[2 * i + 1].append(tests[i])
        reportTests[2 * i].append("average")
        reportTests[2 * i + 1].append("stddev")
        for j in xrange(noDevices):
            if deviceResults[j].getResults().has_key(tests[i]):
                result = deviceResults[j].getResults()[tests[i]]
                if result.getPassFail() == "pass":
                    reportTests[2 * i].append(result.getAverage())
                    reportTests[2 * i + 1].append(result.getStddev())
                else:
                    reportTests[2 * i].append("fail")
                    reportTests[2 * i + 1].append("fail")
            else:
                reportTests[2 * i].append("")
                reportTests[2 * i + 1].append("")

    #print reportTests

    with open(outputCsv, 'wb') as f:
        writer = csv.writer(f)
        writer.writerows(reportInfo)
        writer.writerows(reportTests)


if __name__ == '__main__':
    main(sys.argv)
