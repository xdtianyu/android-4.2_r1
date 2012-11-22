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
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class JUnitClient
{

    static int serverPort = 2020;

    public static void runDaemon(int port)
    {

        final int serverPort = port;
        Thread t = new Thread("App Validator Daemon")
        {

            @Override
            public void run()
            {
                super.run();
                /*
                                try
                                {
                                    Daemon.run(serverPort);
                                }
                                catch (IOException e)
                                {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }*/
            }
        };

        t.setDaemon(true);
        t.start();
    }

    public void testClientInternal(String name) throws UnknownHostException, IOException
    {
        Socket s = new Socket("10.10.26.208", serverPort);

        OutputStream out = s.getOutputStream();
        String aux =
                "c:\\temp\\apks\\" + name
                        + " -sdk C:\\home\\studio\\android-sdk-windows -w4 -v2 -output text\n";
        out.write(aux.getBytes());
        out.flush();

        InputStream in = s.getInputStream();
        BufferedReader reader = null;
        StringBuilder sb;
        try
        {
            reader = new BufferedReader(new InputStreamReader(in));

            sb = new StringBuilder();
            String line = reader.readLine();

            while (line != null)
            {
                sb.append(line + "\n");
                line = reader.readLine();
            }
        }
        finally
        {
            if (reader != null)
            {
                reader.close();
            }
            try
            {
                s.close();
            }
            catch (Exception e)
            {
                //Do Nothing.
            }
        }

        System.out.println(sb.toString());
    }

    @Test
    public void testClient() throws IOException, InterruptedException
    {
        List<Thread> threads = Collections.synchronizedList(new ArrayList<Thread>());

        for (int j = 0; j < 1; j++)
        {
            for (int i = 1; i <= 1; i++)
            {
                final int ii = i;
                Thread t = new Thread()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            testClientInternal("a" + ii + ".apk");
                        }
                        catch (Exception e)
                        {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                };
                t.start();
                threads.add(t);
            }
        }

        for (Thread thread : threads)
        {
            thread.join();
        }
    }

    @Test
    public void testDaemon() throws IOException
    {
        //Daemon.run(serverPort);
    }
}
