/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.traceview;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.BufferUnderflowException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DmTraceReader extends TraceReader {
    private static final int TRACE_MAGIC = 0x574f4c53;

    private static final int METHOD_TRACE_ENTER = 0x00; // method entry
    private static final int METHOD_TRACE_EXIT = 0x01; // method exit
    private static final int METHOD_TRACE_UNROLL = 0x02; // method exited by exception unrolling

    // When in dual clock mode, we report that a context switch has occurred
    // when skew between the real time and thread cpu clocks is more than this
    // many microseconds.
    private static final long MIN_CONTEXT_SWITCH_TIME_USEC = 100;

    private enum ClockSource {
        THREAD_CPU, WALL, DUAL,
    };

    private int mVersionNumber;
    private boolean mRegression;
    private ProfileProvider mProfileProvider;
    private String mTraceFileName;
    private MethodData mTopLevel;
    private ArrayList<Call> mCallList;
    private HashMap<String, String> mPropertiesMap;
    private HashMap<Integer, MethodData> mMethodMap;
    private HashMap<Integer, ThreadData> mThreadMap;
    private ThreadData[] mSortedThreads;
    private MethodData[] mSortedMethods;
    private long mTotalCpuTime;
    private long mTotalRealTime;
    private MethodData mContextSwitch;
    private int mRecordSize;
    private ClockSource mClockSource;

    // A regex for matching the thread "id name" lines in the .key file
    private static final Pattern mIdNamePattern = Pattern.compile("(\\d+)\t(.*)");  //$NON-NLS-1$

    public DmTraceReader(String traceFileName, boolean regression) throws IOException {
        mTraceFileName = traceFileName;
        mRegression = regression;
        mPropertiesMap = new HashMap<String, String>();
        mMethodMap = new HashMap<Integer, MethodData>();
        mThreadMap = new HashMap<Integer, ThreadData>();
        mCallList = new ArrayList<Call>();

        // Create a single top-level MethodData object to hold the profile data
        // for time spent in the unknown caller.
        mTopLevel = new MethodData(0, "(toplevel)");
        mContextSwitch = new MethodData(-1, "(context switch)");
        mMethodMap.put(0, mTopLevel);
        mMethodMap.put(-1, mContextSwitch);
        generateTrees();
    }

    void generateTrees() throws IOException {
        long offset = parseKeys();
        parseData(offset);
        analyzeData();
    }

    @Override
    public ProfileProvider getProfileProvider() {
        if (mProfileProvider == null)
            mProfileProvider = new ProfileProvider(this);
        return mProfileProvider;
    }

    private MappedByteBuffer mapFile(String filename, long offset) throws IOException {
        MappedByteBuffer buffer = null;
        FileInputStream dataFile = new FileInputStream(filename);
        try {
            File file = new File(filename);
            FileChannel fc = dataFile.getChannel();
            buffer = fc.map(FileChannel.MapMode.READ_ONLY, offset, file.length() - offset);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            return buffer;
        } finally {
            dataFile.close(); // this *also* closes the associated channel, fc
        }
    }

    private void readDataFileHeader(MappedByteBuffer buffer) {
        int magic = buffer.getInt();
        if (magic != TRACE_MAGIC) {
            System.err.printf(
                    "Error: magic number mismatch; got 0x%x, expected 0x%x\n",
                    magic, TRACE_MAGIC);
            throw new RuntimeException();
        }

        // read version
        int version = buffer.getShort();
        if (version != mVersionNumber) {
            System.err.printf(
                    "Error: version number mismatch; got %d in data header but %d in options\n",
                    version, mVersionNumber);
            throw new RuntimeException();
        }
        if (version < 1 || version > 3) {
            System.err.printf(
                    "Error: unsupported trace version number %d.  "
                    + "Please use a newer version of TraceView to read this file.", version);
            throw new RuntimeException();
        }

        // read offset
        int offsetToData = buffer.getShort() - 16;

        // read startWhen
        buffer.getLong();

        // read record size
        if (version == 1) {
            mRecordSize = 9;
        } else if (version == 2) {
            mRecordSize = 10;
        } else {
            mRecordSize = buffer.getShort();
            offsetToData -= 2;
        }

        // Skip over offsetToData bytes
        while (offsetToData-- > 0) {
            buffer.get();
        }
    }

    private void parseData(long offset) throws IOException {
        MappedByteBuffer buffer = mapFile(mTraceFileName, offset);
        readDataFileHeader(buffer);

        ArrayList<TraceAction> trace = null;
        if (mClockSource == ClockSource.THREAD_CPU) {
            trace = new ArrayList<TraceAction>();
        }

        final boolean haveThreadClock = mClockSource != ClockSource.WALL;
        final boolean haveGlobalClock = mClockSource != ClockSource.THREAD_CPU;

        // Parse all call records to obtain elapsed time information.
        ThreadData prevThreadData = null;
        for (;;) {
            int threadId;
            int methodId;
            long threadTime, globalTime;
            try {
                int recordSize = mRecordSize;

                if (mVersionNumber == 1) {
                    threadId = buffer.get();
                    recordSize -= 1;
                } else {
                    threadId = buffer.getShort();
                    recordSize -= 2;
                }

                methodId = buffer.getInt();
                recordSize -= 4;

                switch (mClockSource) {
                    case WALL:
                        threadTime = 0;
                        globalTime = buffer.getInt();
                        recordSize -= 4;
                        break;
                    case DUAL:
                        threadTime = buffer.getInt();
                        globalTime = buffer.getInt();
                        recordSize -= 8;
                        break;
                    default:
                    case THREAD_CPU:
                        threadTime = buffer.getInt();
                        globalTime = 0;
                        recordSize -= 4;
                        break;
                }

                while (recordSize-- > 0) {
                    buffer.get();
                }
            } catch (BufferUnderflowException ex) {
                break;
            }

            int methodAction = methodId & 0x03;
            methodId = methodId & ~0x03;
            MethodData methodData = mMethodMap.get(methodId);
            if (methodData == null) {
                String name = String.format("(0x%1$x)", methodId);  //$NON-NLS-1$
                methodData = new MethodData(methodId, name);
                mMethodMap.put(methodId, methodData);
            }

            ThreadData threadData = mThreadMap.get(threadId);
            if (threadData == null) {
                String name = String.format("[%1$d]", threadId);  //$NON-NLS-1$
                threadData = new ThreadData(threadId, name, mTopLevel);
                mThreadMap.put(threadId, threadData);
            }

            long elapsedGlobalTime = 0;
            if (haveGlobalClock) {
                if (!threadData.mHaveGlobalTime) {
                    threadData.mGlobalStartTime = globalTime;
                    threadData.mHaveGlobalTime = true;
                } else {
                    elapsedGlobalTime = globalTime - threadData.mGlobalEndTime;
                }
                threadData.mGlobalEndTime = globalTime;
            }

            if (haveThreadClock) {
                long elapsedThreadTime = 0;
                if (!threadData.mHaveThreadTime) {
                    threadData.mThreadStartTime = threadTime;
                    threadData.mThreadCurrentTime = threadTime;
                    threadData.mHaveThreadTime = true;
                } else {
                    elapsedThreadTime = threadTime - threadData.mThreadEndTime;
                }
                threadData.mThreadEndTime = threadTime;

                if (!haveGlobalClock) {
                    // Detect context switches whenever execution appears to switch from one
                    // thread to another.  This assumption is only valid on uniprocessor
                    // systems (which is why we now have a dual clock mode).
                    // We represent context switches in the trace by pushing a call record
                    // with MethodData mContextSwitch onto the stack of the previous
                    // thread.  We arbitrarily set the start and end time of the context
                    // switch such that the context switch occurs in the middle of the thread
                    // time and itself accounts for zero thread time.
                    if (prevThreadData != null && prevThreadData != threadData) {
                        // Begin context switch from previous thread.
                        Call switchCall = prevThreadData.enter(mContextSwitch, trace);
                        switchCall.mThreadStartTime = prevThreadData.mThreadEndTime;
                        mCallList.add(switchCall);

                        // Return from context switch to current thread.
                        Call top = threadData.top();
                        if (top.getMethodData() == mContextSwitch) {
                            threadData.exit(mContextSwitch, trace);
                            long beforeSwitch = elapsedThreadTime / 2;
                            top.mThreadStartTime += beforeSwitch;
                            top.mThreadEndTime = top.mThreadStartTime;
                        }
                    }
                    prevThreadData = threadData;
                } else {
                    // If we have a global clock, then we can detect context switches (or blocking
                    // calls or cpu suspensions or clock anomalies) by comparing global time to
                    // thread time for successive calls that occur on the same thread.
                    // As above, we represent the context switch using a special method call.
                    long sleepTime = elapsedGlobalTime - elapsedThreadTime;
                    if (sleepTime > MIN_CONTEXT_SWITCH_TIME_USEC) {
                        Call switchCall = threadData.enter(mContextSwitch, trace);
                        long beforeSwitch = elapsedThreadTime / 2;
                        long afterSwitch = elapsedThreadTime - beforeSwitch;
                        switchCall.mGlobalStartTime = globalTime - elapsedGlobalTime + beforeSwitch;
                        switchCall.mGlobalEndTime = globalTime - afterSwitch;
                        switchCall.mThreadStartTime = threadTime - afterSwitch;
                        switchCall.mThreadEndTime = switchCall.mThreadStartTime;
                        threadData.exit(mContextSwitch, trace);
                        mCallList.add(switchCall);
                    }
                }

                // Add thread CPU time.
                Call top = threadData.top();
                top.addCpuTime(elapsedThreadTime);
            }

            switch (methodAction) {
                case METHOD_TRACE_ENTER: {
                    Call call = threadData.enter(methodData, trace);
                    if (haveGlobalClock) {
                        call.mGlobalStartTime = globalTime;
                    }
                    if (haveThreadClock) {
                        call.mThreadStartTime = threadTime;
                    }
                    mCallList.add(call);
                    break;
                }
                case METHOD_TRACE_EXIT:
                case METHOD_TRACE_UNROLL: {
                    Call call = threadData.exit(methodData, trace);
                    if (call != null) {
                        if (haveGlobalClock) {
                            call.mGlobalEndTime = globalTime;
                        }
                        if (haveThreadClock) {
                            call.mThreadEndTime = threadTime;
                        }
                    }
                    break;
                }
                default:
                    throw new RuntimeException("Unrecognized method action: " + methodAction);
            }
        }

        // Exit any pending open-ended calls.
        for (ThreadData threadData : mThreadMap.values()) {
            threadData.endTrace(trace);
        }

        // Recreate the global timeline from thread times, if needed.
        if (!haveGlobalClock) {
            long globalTime = 0;
            prevThreadData = null;
            for (TraceAction traceAction : trace) {
                Call call = traceAction.mCall;
                ThreadData threadData = call.getThreadData();

                if (traceAction.mAction == TraceAction.ACTION_ENTER) {
                    long threadTime = call.mThreadStartTime;
                    globalTime += call.mThreadStartTime - threadData.mThreadCurrentTime;
                    call.mGlobalStartTime = globalTime;
                    if (!threadData.mHaveGlobalTime) {
                        threadData.mHaveGlobalTime = true;
                        threadData.mGlobalStartTime = globalTime;
                    }
                    threadData.mThreadCurrentTime = threadTime;
                } else if (traceAction.mAction == TraceAction.ACTION_EXIT) {
                    long threadTime = call.mThreadEndTime;
                    globalTime += call.mThreadEndTime - threadData.mThreadCurrentTime;
                    call.mGlobalEndTime = globalTime;
                    threadData.mGlobalEndTime = globalTime;
                    threadData.mThreadCurrentTime = threadTime;
                } // else, ignore ACTION_INCOMPLETE calls, nothing to do
                prevThreadData = threadData;
            }
        }

        // Finish updating all calls and calculate the total time spent.
        for (int i = mCallList.size() - 1; i >= 0; i--) {
            Call call = mCallList.get(i);

            // Calculate exclusive real-time by subtracting inclusive real time
            // accumulated by children from the total span.
            long realTime = call.mGlobalEndTime - call.mGlobalStartTime;
            call.mExclusiveRealTime = Math.max(realTime - call.mInclusiveRealTime, 0);
            call.mInclusiveRealTime = realTime;

            call.finish();
        }
        mTotalCpuTime = 0;
        mTotalRealTime = 0;
        for (ThreadData threadData : mThreadMap.values()) {
            Call rootCall = threadData.getRootCall();
            threadData.updateRootCallTimeBounds();
            rootCall.finish();
            mTotalCpuTime += rootCall.mInclusiveCpuTime;
            mTotalRealTime += rootCall.mInclusiveRealTime;
        }

        if (mRegression) {
            System.out.format("totalCpuTime %dus\n", mTotalCpuTime);
            System.out.format("totalRealTime %dus\n", mTotalRealTime);

            dumpThreadTimes();
            dumpCallTimes();
        }
    }

    static final int PARSE_VERSION = 0;
    static final int PARSE_THREADS = 1;
    static final int PARSE_METHODS = 2;
    static final int PARSE_OPTIONS = 4;

    long parseKeys() throws IOException {
        long offset = 0;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                    new FileInputStream(mTraceFileName), "US-ASCII"));

            int mode = PARSE_VERSION;
            String line = null;
            while (true) {
                line = in.readLine();
                if (line == null) {
                    throw new IOException("Key section does not have an *end marker");
                }

                // Calculate how much we have read from the file so far.  The
                // extra byte is for the line ending not included by readLine().
                offset += line.length() + 1;
                if (line.startsWith("*")) {
                    if (line.equals("*version")) {
                        mode = PARSE_VERSION;
                        continue;
                    }
                    if (line.equals("*threads")) {
                        mode = PARSE_THREADS;
                        continue;
                    }
                    if (line.equals("*methods")) {
                        mode = PARSE_METHODS;
                        continue;
                    }
                    if (line.equals("*end")) {
                        break;
                    }
                }
                switch (mode) {
                case PARSE_VERSION:
                    mVersionNumber = Integer.decode(line);
                    mode = PARSE_OPTIONS;
                    break;
                case PARSE_THREADS:
                    parseThread(line);
                    break;
                case PARSE_METHODS:
                    parseMethod(line);
                    break;
                case PARSE_OPTIONS:
                    parseOption(line);
                    break;
                }
            }
        } catch (FileNotFoundException ex) {
            System.err.println(ex.getMessage());
        } finally {
            if (in != null) {
                in.close();
            }
        }

        if (mClockSource == null) {
            mClockSource = ClockSource.THREAD_CPU;
        }

        return offset;
    }

    void parseOption(String line) {
        String[] tokens = line.split("=");
        if (tokens.length == 2) {
            String key = tokens[0];
            String value = tokens[1];
            mPropertiesMap.put(key, value);

            if (key.equals("clock")) {
                if (value.equals("thread-cpu")) {
                    mClockSource = ClockSource.THREAD_CPU;
                } else if (value.equals("wall")) {
                    mClockSource = ClockSource.WALL;
                } else if (value.equals("dual")) {
                    mClockSource = ClockSource.DUAL;
                }
            }
        }
    }

    void parseThread(String line) {
        String idStr = null;
        String name = null;
        Matcher matcher = mIdNamePattern.matcher(line);
        if (matcher.find()) {
            idStr = matcher.group(1);
            name = matcher.group(2);
        }
        if (idStr == null) return;
        if (name == null) name = "(unknown)";

        int id = Integer.decode(idStr);
        mThreadMap.put(id, new ThreadData(id, name, mTopLevel));
    }

    void parseMethod(String line) {
        String[] tokens = line.split("\t");
        int id = Long.decode(tokens[0]).intValue();
        String className = tokens[1];
        String methodName = null;
        String signature = null;
        String pathname = null;
        int lineNumber = -1;
        if (tokens.length == 6) {
            methodName = tokens[2];
            signature = tokens[3];
            pathname = tokens[4];
            lineNumber = Integer.decode(tokens[5]);
            pathname = constructPathname(className, pathname);
        } else if (tokens.length > 2) {
            if (tokens[3].startsWith("(")) {
                methodName = tokens[2];
                signature = tokens[3];
            } else {
                pathname = tokens[2];
                lineNumber = Integer.decode(tokens[3]);
            }
        }

        mMethodMap.put(id, new MethodData(id, className, methodName, signature,
                pathname, lineNumber));
    }

    private String constructPathname(String className, String pathname) {
        int index = className.lastIndexOf('/');
        if (index > 0 && index < className.length() - 1
                && pathname.endsWith(".java"))
            pathname = className.substring(0, index + 1) + pathname;
        return pathname;
    }

    private void analyzeData() {
        final TimeBase timeBase = getPreferredTimeBase();

        // Sort the threads into decreasing cpu time
        Collection<ThreadData> tv = mThreadMap.values();
        mSortedThreads = tv.toArray(new ThreadData[tv.size()]);
        Arrays.sort(mSortedThreads, new Comparator<ThreadData>() {
            @Override
            public int compare(ThreadData td1, ThreadData td2) {
                if (timeBase.getTime(td2) > timeBase.getTime(td1))
                    return 1;
                if (timeBase.getTime(td2) < timeBase.getTime(td1))
                    return -1;
                return td2.getName().compareTo(td1.getName());
            }
        });

        // Sort the methods into decreasing inclusive time
        Collection<MethodData> mv = mMethodMap.values();
        MethodData[] methods;
        methods = mv.toArray(new MethodData[mv.size()]);
        Arrays.sort(methods, new Comparator<MethodData>() {
            @Override
            public int compare(MethodData md1, MethodData md2) {
                if (timeBase.getElapsedInclusiveTime(md2) > timeBase.getElapsedInclusiveTime(md1))
                    return 1;
                if (timeBase.getElapsedInclusiveTime(md2) < timeBase.getElapsedInclusiveTime(md1))
                    return -1;
                return md1.getName().compareTo(md2.getName());
            }
        });

        // Count the number of methods with non-zero inclusive time
        int nonZero = 0;
        for (MethodData md : methods) {
            if (timeBase.getElapsedInclusiveTime(md) == 0)
                break;
            nonZero += 1;
        }

        // Copy the methods with non-zero time
        mSortedMethods = new MethodData[nonZero];
        int ii = 0;
        for (MethodData md : methods) {
            if (timeBase.getElapsedInclusiveTime(md) == 0)
                break;
            md.setRank(ii);
            mSortedMethods[ii++] = md;
        }

        // Let each method analyze its profile data
        for (MethodData md : mSortedMethods) {
            md.analyzeData(timeBase);
        }

        // Update all the calls to include the method rank in
        // their name.
        for (Call call : mCallList) {
            call.updateName();
        }

        if (mRegression) {
            dumpMethodStats();
        }
    }

    /*
     * This method computes a list of records that describe the the execution
     * timeline for each thread. Each record is a pair: (row, block) where: row:
     * is the ThreadData object block: is the call (containing the start and end
     * times)
     */
    @Override
    public ArrayList<TimeLineView.Record> getThreadTimeRecords() {
        TimeLineView.Record record;
        ArrayList<TimeLineView.Record> timeRecs;
        timeRecs = new ArrayList<TimeLineView.Record>();

        // For each thread, push a "toplevel" call that encompasses the
        // entire execution of the thread.
        for (ThreadData threadData : mSortedThreads) {
            if (!threadData.isEmpty() && threadData.getId() != 0) {
                record = new TimeLineView.Record(threadData, threadData.getRootCall());
                timeRecs.add(record);
            }
        }

        for (Call call : mCallList) {
            record = new TimeLineView.Record(call.getThreadData(), call);
            timeRecs.add(record);
        }

        if (mRegression) {
            dumpTimeRecs(timeRecs);
            System.exit(0);
        }
        return timeRecs;
    }

    private void dumpThreadTimes() {
        System.out.print("\nThread Times\n");
        System.out.print("id  t-start    t-end  g-start    g-end     name\n");
        for (ThreadData threadData : mThreadMap.values()) {
            System.out.format("%2d %8d %8d %8d %8d  %s\n",
                    threadData.getId(),
                    threadData.mThreadStartTime, threadData.mThreadEndTime,
                    threadData.mGlobalStartTime, threadData.mGlobalEndTime,
                    threadData.getName());
        }
    }

    private void dumpCallTimes() {
        System.out.print("\nCall Times\n");
        System.out.print("id  t-start    t-end  g-start    g-end    excl.    incl.  method\n");
        for (Call call : mCallList) {
            System.out.format("%2d %8d %8d %8d %8d %8d %8d  %s\n",
                    call.getThreadId(), call.mThreadStartTime, call.mThreadEndTime,
                    call.mGlobalStartTime, call.mGlobalEndTime,
                    call.mExclusiveCpuTime, call.mInclusiveCpuTime,
                    call.getMethodData().getName());
        }
    }

    private void dumpMethodStats() {
        System.out.print("\nMethod Stats\n");
        System.out.print("Excl Cpu  Incl Cpu  Excl Real Incl Real    Calls  Method\n");
        for (MethodData md : mSortedMethods) {
            System.out.format("%9d %9d %9d %9d %9s  %s\n",
                    md.getElapsedExclusiveCpuTime(), md.getElapsedInclusiveCpuTime(),
                    md.getElapsedExclusiveRealTime(), md.getElapsedInclusiveRealTime(),
                    md.getCalls(), md.getProfileName());
        }
    }

    private void dumpTimeRecs(ArrayList<TimeLineView.Record> timeRecs) {
        System.out.print("\nTime Records\n");
        System.out.print("id  t-start    t-end  g-start    g-end  method\n");
        for (TimeLineView.Record record : timeRecs) {
            Call call = (Call) record.block;
            System.out.format("%2d %8d %8d %8d %8d  %s\n",
                    call.getThreadId(), call.mThreadStartTime, call.mThreadEndTime,
                    call.mGlobalStartTime, call.mGlobalEndTime,
                    call.getMethodData().getName());
        }
    }

    @Override
    public HashMap<Integer, String> getThreadLabels() {
        HashMap<Integer, String> labels = new HashMap<Integer, String>();
        for (ThreadData t : mThreadMap.values()) {
            labels.put(t.getId(), t.getName());
        }
        return labels;
    }

    @Override
    public MethodData[] getMethods() {
        return mSortedMethods;
    }

    @Override
    public ThreadData[] getThreads() {
        return mSortedThreads;
    }

    @Override
    public long getTotalCpuTime() {
        return mTotalCpuTime;
    }

    @Override
    public long getTotalRealTime() {
        return mTotalRealTime;
    }

    @Override
    public boolean haveCpuTime() {
        return mClockSource != ClockSource.WALL;
    }

    @Override
    public boolean haveRealTime() {
        return mClockSource != ClockSource.THREAD_CPU;
    }

    @Override
    public HashMap<String, String> getProperties() {
        return mPropertiesMap;
    }

    @Override
    public TimeBase getPreferredTimeBase() {
        if (mClockSource == ClockSource.WALL) {
            return TimeBase.REAL_TIME;
        }
        return TimeBase.CPU_TIME;
    }

    @Override
    public String getClockSource() {
        switch (mClockSource) {
            case THREAD_CPU:
                return "cpu time";
            case WALL:
                return "real time";
            case DUAL:
                return "real time, dual clock";
        }
        return null;
    }
}
