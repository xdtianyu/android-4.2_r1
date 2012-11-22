/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.motorolamobility.preflighting.internal.daemon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.motorolamobility.preflighting.core.exception.PreflightingToolException;
import com.motorolamobility.preflighting.core.validation.ValidationManager.InputParameter;
import com.motorolamobility.preflighting.core.verbose.DebugVerboseOutputter;
import com.motorolamobility.preflighting.core.verbose.DebugVerboseOutputter.VerboseLevel;
import com.motorolamobility.preflighting.i18n.PreflightingNLS;
import com.motorolamobility.preflighting.internal.PreflightingApplication;
import com.motorolamobility.preflighting.internal.commandoutput.NullOutputStream;
import com.motorolamobility.preflighting.internal.commandoutput.OutputterFactory;
import com.motorolamobility.preflighting.internal.commandoutput.OutputterFactory.OutputType;
import com.motorolamobility.preflighting.internal.help.printer.HelpPrinter;

public class Daemon
{
    final public static String STATUS_CONTROL_MESSAGE = "are_you_alive";

    final public static String LISTENING_STATUS_MESSAGE = "i_am_up_and_running";

    final public static Integer DEFAULT_PORT = 50000;

    final public static int BOUND_TIMEOUT = 2000;

    final private static int TEST_TIMEOUT = 1000;

    final private static String LOCALHOST = "127.0.0.1";

    private final int serverPort;

    private final String sdkPath;

    private Thread daemonThread = null;

    private PrintStream debugStream = null;

    public static PrintStream nullStream = new PrintStream(new NullOutputStream());

    public Daemon(int serverPort, String sdkPath)
    {
        // Default debug/error stream
        DebugVerboseOutputter.setStream(nullStream);
        OutputterFactory.getInstance().setDefaultOutputter(OutputType.XML);
        this.serverPort = serverPort;
        this.sdkPath = sdkPath;
    }

    class DaemonRunnable implements Runnable
    {

        Socket socket;

        private final Daemon daemon;

        private final String command;

        public DaemonRunnable(Socket socket, String command, Daemon daemon)
        {
            this.socket = socket;
            this.command = command;
            this.daemon = daemon;
        }

        /*
         * delete -sdk parameter from client
         * throws an exception if -daemon is found
         * add -sdk parameter that comes from daemon
         */
        private String processParameters(String parameters, PrintStream stream)
                throws PreflightingToolException
        {
            String newParams = null;
            String regex = "((?!(\\s+-)).)*"; //$NON-NLS-1$
            Pattern pat = Pattern.compile(regex);
            Matcher matcher = pat.matcher(parameters);

            // for each parameter part found, process it
            while (matcher.find())
            {
                String parameterValues = parameters.substring(matcher.start(), matcher.end());

                // remove -sdk coming from client
                if (parameterValues.trim().startsWith("-sdk")) //$NON-NLS-1$
                {
                    newParams = parameters.substring(0, matcher.start());
                    newParams =
                            newParams + parameters.substring(matcher.end(), parameters.length());
                }
                if (parameterValues.trim().startsWith("-daemon")) //$NON-NLS-1$
                {
                    throw new PreflightingToolException("Cannot start a daemon inside another."); //$NON-NLS-1$
                }
            }
            if (newParams == null)
            {
                newParams = parameters;
            }

            //adding the sdk path passed to the daemon
            if ((newParams != null) && (daemon.getSdkPath() != null))
            {
                newParams +=
                        " -" + (InputParameter.SDK_PATH.getAlias()) + " " + daemon.getSdkPath();
            }

            return newParams;
        }

        public void run()
        {
            PrintStream stream = null;
            try
            {

                String parameters = command;
                stream = new PrintStream(socket.getOutputStream(), true);

                DebugVerboseOutputter.printVerboseMessage("New input: " + parameters, //$NON-NLS-1$
                        VerboseLevel.v0);

                if (parameters.equals(STATUS_CONTROL_MESSAGE))
                {
                    stream.println(LISTENING_STATUS_MESSAGE);
                }
                else if (parameters.equals("-list-checkers"))
                {
                    HelpPrinter.printXMLCheckerList(stream);
                }
                else if (parameters.equals("-list-devices"))
                {
                    HelpPrinter.printXMLDevicesList(stream);
                }
                else if (parameters.equals("-get-checkers-devices-specsMap-xml"))
                {
                    HelpPrinter.printXMLDevicesCheckersSpecsMap(stream);
                }
                else
                {
                    parameters = processParameters(parameters, stream);
                    PreflightingApplication.validate(parameters, stream, daemon.getDebugStream());
                }
                stream.flush();
            }
            catch (Exception e)
            {
                DebugVerboseOutputter.printVerboseMessage(PreflightingNLS.Daemon_ValidationError
                        + " " + socket.getRemoteSocketAddress() + ". " + e.getMessage(),
                        VerboseLevel.v0);
            }
            finally
            {
                if (stream != null)
                {
                    stream.close();
                }
                if (socket != null)
                {
                    try
                    {
                        socket.close();
                    }
                    catch (IOException e)
                    {
                        // do nothing since the socket was closed here
                    }
                }
            }
        }
    }

    private void fork(Socket socket, String command)
    {
        DaemonRunnable runnable = new DaemonRunnable(socket, command, this);
        Thread t = new Thread(runnable, "AppValidator - " + socket.getLocalPort()); //$NON-NLS-1$

        t.start();
    }

    public PrintStream getDebugStream()
    {
        return debugStream;
    }

    private void run() throws IOException
    {
        ServerSocket socket = null;
        try
        {
            socket = new ServerSocket(serverPort);

            while (true)
            {
                try
                {
                    Socket conn = socket.accept();

                    String command = readCommand(conn);

                    if (!command.equals("-quit"))

                    {
                        fork(conn, command);
                    }
                    else
                    {
                        break;
                    }

                }
                catch (OutOfMemoryError err)
                {
                    // log
                    DebugVerboseOutputter.printVerboseMessage(
                            "The application ran out of memory: " + err.getMessage(), //$NON-NLS-1$
                            VerboseLevel.v0);
                    break;
                }
                // any other exception occured. continue
                catch (Exception e)
                {
                    DebugVerboseOutputter.printVerboseMessage(
                            "The validation instance failed to execute: " + e.getMessage(), //$NON-NLS-1$
                            VerboseLevel.v0);
                }
            }
        }
        finally
        {
            if (socket != null)
            {
                try
                {
                    socket.close();
                }
                catch (IOException e)
                {
                    // do nothing since the socket was closed here
                }
            }
        }

        System.out.println(PreflightingNLS.Daemon_Stopped);

    }

    private String readCommand(Socket socket) throws IOException
    {

        String command = "";

        InputStream in = socket.getInputStream();
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new InputStreamReader(in));
            command = reader.readLine();
        }
        finally
        {
            if (reader != null)
            {
                reader.close();
            }
        }
        return command;

    }

    /**
     * Starts this daemon in a new thread
     */
    public void startDaemon()
    {
        try
        {
            System.out.println(PreflightingNLS.Daemon_StartingStatusMessage);

            //that's the daemon thread
            daemonThread = new Thread()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Daemon.this.run();
                    }
                    catch (Exception e)
                    {
                        DebugVerboseOutputter.printVerboseMessage(
                                "Daemon aborted: " + e.getMessage(), //$NON-NLS-1$
                                VerboseLevel.v0);
                    }
                }
            };
            daemonThread.start();
        }

        catch (Exception e)
        {
            DebugVerboseOutputter.printVerboseMessage(PreflightingNLS.Daemon_StartingErrorMessage
                    + " " + e.getMessage(), VerboseLevel.v0);
        }
    }

    /**
     * Test daemon with a status control message.
     * @param daemonThread
     * @param serverPort
     * @throws IOException 
     * @throws UnknownHostException 
     * @throws InterruptedException 
     * @throws Exception
     */
    public boolean testDaemon() throws UnknownHostException, IOException, InterruptedException
    {
        boolean returnValue = false;

        if (daemonThread.isAlive())
        {
            DebugVerboseOutputter.printVerboseMessage(PreflightingNLS.bind(
                    PreflightingNLS.Daemon_TestDaemonStatusMessage, serverPort), VerboseLevel.v0);
            int i = 1;
            while (true)
            {
                Socket clientSocket = null;
                BufferedReader reader = null;
                try
                {
                    clientSocket = new Socket(LOCALHOST, serverPort);
                    OutputStream out = clientSocket.getOutputStream();
                    String aux = Daemon.STATUS_CONTROL_MESSAGE + "\n";
                    out.write(aux.getBytes());
                    out.flush();

                    InputStream in = clientSocket.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(in));

                    String line = reader.readLine();
                    if (line.equals(Daemon.LISTENING_STATUS_MESSAGE))
                    {
                        DebugVerboseOutputter.printVerboseMessage(PreflightingNLS.bind(
                                PreflightingNLS.Daemon_TestDaemonSucceedTry, i), VerboseLevel.v0);
                        DebugVerboseOutputter.printVerboseMessage(PreflightingNLS.bind(
                                PreflightingNLS.Daemon_LinsteningMessage, serverPort),
                                VerboseLevel.v0);
                        returnValue = true;
                        break;
                    }

                    DebugVerboseOutputter.printVerboseMessage(
                            PreflightingNLS.bind(PreflightingNLS.Daemon_TestDaemonFailedTry, i++),
                            VerboseLevel.v0);
                    //wait before next try
                    Thread.sleep(TEST_TIMEOUT);
                }
                finally
                {
                    if (reader != null)
                    {
                        reader.close();
                    }
                    if (clientSocket != null)
                    {
                        clientSocket.close();
                    }
                }
            }

        }
        return returnValue;
    }

    public void join() throws InterruptedException
    {
        daemonThread.join();
    }

    public String getSdkPath()
    {
        return sdkPath;
    }

    /**
     * Turn on/off debug mode. If debug is true, debug messages will be printed to the system error stream.
     * If false, no message is printed. 
     * 
     * @param debug true for debug mode on
     */
    public void setDebugOn(boolean debug)
    {
        debugStream = debug ? System.err : nullStream;
        DebugVerboseOutputter.setStream(debugStream);
    }

}
