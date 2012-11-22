
/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.chimpchat;

import com.android.chimpchat.core.IChimpView;
import com.android.chimpchat.core.PhysicalButton;
import com.android.chimpchat.core.ChimpException;
import com.android.chimpchat.core.ChimpRect;
import com.android.chimpchat.core.ChimpView;

import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a nicer interface to interacting with the low-level network access protocol for talking
 * to the monkey.
 *
 * This class is thread-safe and can handle being called from multiple threads.
 */
public class ChimpManager {
    private static Logger LOG = Logger.getLogger(ChimpManager.class.getName());

    private Socket monkeySocket;
    private BufferedWriter monkeyWriter;
    private BufferedReader monkeyReader;

    /**
     * Create a new ChimpMananger to talk to the specified device.
     *
     * @param monkeySocket the already connected socket on which to send protocol messages.
     * @throws IOException if there is an issue setting up the sockets
     */
    public ChimpManager(Socket monkeySocket) throws IOException {
        this.monkeySocket = monkeySocket;
        monkeyWriter =
                new BufferedWriter(new OutputStreamWriter(monkeySocket.getOutputStream()));
        monkeyReader = new BufferedReader(new InputStreamReader(monkeySocket.getInputStream()));
    }

    /* Ensure that everything gets shutdown properly */
    protected void finalize() throws Throwable {
        try {
            quit();
        } finally {
            close();
            super.finalize();
        }
    }

    /**
     * Send a touch down event at the specified location.
     *
     * @param x the x coordinate of where to click
     * @param y the y coordinate of where to click
     * @return success or not
     * @throws IOException on error communicating with the device
     */
    public boolean touchDown(int x, int y) throws IOException {
        return sendMonkeyEvent("touch down " + x + " " + y);
    }

    /**
     * Send a touch down event at the specified location.
     *
     * @param x the x coordinate of where to click
     * @param y the y coordinate of where to click
     * @return success or not
     * @throws IOException on error communicating with the device
     */
    public boolean touchUp(int x, int y) throws IOException {
        return sendMonkeyEvent("touch up " + x + " " + y);
    }

    /**
     * Send a touch move event at the specified location.
     *
     * @param x the x coordinate of where to click
     * @param y the y coordinate of where to click
     * @return success or not
     * @throws IOException on error communicating with the device
     */
    public boolean touchMove(int x, int y) throws IOException {
        return sendMonkeyEvent("touch move " + x + " " + y);
    }

    /**
     * Send a touch (down and then up) event at the specified location.
     *
     * @param x the x coordinate of where to click
     * @param y the y coordinate of where to click
     * @return success or not
     * @throws IOException on error communicating with the device
     */
    public boolean touch(int x, int y) throws IOException {
        return sendMonkeyEvent("tap " + x + " " + y);
    }

    /**
     * Press a physical button on the device.
     *
     * @param name the name of the button (As specified in the protocol)
     * @return success or not
     * @throws IOException on error communicating with the device
     */
    public boolean press(String name) throws IOException {
        return sendMonkeyEvent("press " + name);
    }

    /**
     * Send a Key Down event for the specified button.
     *
     * @param name the name of the button (As specified in the protocol)
     * @return success or not
     * @throws IOException on error communicating with the device
     */
    public boolean keyDown(String name) throws IOException {
        return sendMonkeyEvent("key down " + name);
    }

    /**
     * Send a Key Up event for the specified button.
     *
     * @param name the name of the button (As specified in the protocol)
     * @return success or not
     * @throws IOException on error communicating with the device
     */
    public boolean keyUp(String name) throws IOException {
        return sendMonkeyEvent("key up " + name);
    }

    /**
     * Press a physical button on the device.
     *
     * @param button the button to press
     * @return success or not
     * @throws IOException on error communicating with the device
     */
    public boolean press(PhysicalButton button) throws IOException {
        return press(button.getKeyName());
    }

    /**
     * This function allows the communication bridge between the host and the device
     * to be invisible to the script for internal needs.
     * It splits a command into monkey events and waits for responses for each over an adb tcp socket.
     * Returns on an error, else continues and sets up last response.
     *
     * @param command the monkey command to send to the device
     * @return the (unparsed) response returned from the monkey.
     * @throws IOException on error communicating with the device
     */
    private String sendMonkeyEventAndGetResponse(String command) throws IOException {
        command = command.trim();
        LOG.info("Monkey Command: " + command + ".");

        // send a single command and get the response
        monkeyWriter.write(command + "\n");
        monkeyWriter.flush();
        return monkeyReader.readLine();
    }

    /**
     * Parse a monkey response string to see if the command succeeded or not.
     *
     * @param monkeyResponse the response
     * @return true if response code indicated success.
     */
    private boolean parseResponseForSuccess(String monkeyResponse) {
        if (monkeyResponse == null) {
            return false;
        }
        // return on ok
        if(monkeyResponse.startsWith("OK")) {
            return true;
        }

        return false;
    }

    /**
     * Parse a monkey response string to get the extra data returned.
     *
     * @param monkeyResponse the response
     * @return any extra data that was returned, or empty string if there was nothing.
     */
    private String parseResponseForExtra(String monkeyResponse) {
        int offset = monkeyResponse.indexOf(':');
        if (offset < 0) {
            return "";
        }
        return monkeyResponse.substring(offset + 1);
    }

    /**
     * This function allows the communication bridge between the host and the device
     * to be invisible to the script for internal needs.
     * It splits a command into monkey events and waits for responses for each over an
     * adb tcp socket.
     *
     * @param command the monkey command to send to the device
     * @return true on success.
     * @throws IOException on error communicating with the device
     */
    private boolean sendMonkeyEvent(String command) throws IOException {
        synchronized (this) {
            String monkeyResponse = sendMonkeyEventAndGetResponse(command);
            return parseResponseForSuccess(monkeyResponse);
        }
    }

    /**
     * Close all open resources related to this device.
     */
    public void close() {
        try {
            monkeySocket.close();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Unable to close monkeySocket", e);
        }
        try {
            monkeyReader.close();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Unable to close monkeyReader", e);
        }
        try {
            monkeyWriter.close();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Unable to close monkeyWriter", e);
        }
    }

    /**
     * Function to get a static variable from the device.
     *
     * @param name name of static variable to get
     * @return the value of the variable, or null if there was an error
     * @throws IOException on error communicating with the device
     */
    public String getVariable(String name) throws IOException {
        synchronized (this) {
            String response = sendMonkeyEventAndGetResponse("getvar " + name);
            if (!parseResponseForSuccess(response)) {
                return null;
            }
            return parseResponseForExtra(response);
        }
    }

    /**
     * Function to get the list of variables from the device.
     * @return the list of variables as a collection of strings
     * @throws IOException on error communicating with the device
     */
    public Collection<String> listVariable() throws IOException {
        synchronized (this) {
            String response = sendMonkeyEventAndGetResponse("listvar");
            if (!parseResponseForSuccess(response)) {
                Collections.emptyList();
            }
            String extras = parseResponseForExtra(response);
            return Lists.newArrayList(extras.split(" "));
        }
    }

    /**
     * Tells the monkey that we are done for this session.
     * @throws IOException on error communicating with the device
     */
    public void done() throws IOException {
        // this command just drops the connection, so handle it here
        synchronized (this) {
            sendMonkeyEventAndGetResponse("done");
        }
    }

    /**
     * Tells the monkey that we are done forever.
     * @throws IOException on error communicating with the device
     */
    public void quit() throws IOException {
        // this command drops the connection, so handle it here
        synchronized (this) {
            try {
                sendMonkeyEventAndGetResponse("quit");
            } catch (SocketException e) {
                // flush was called after the call had been written, so it tried flushing to a
                // broken pipe.
            }
        }
    }

    /**
     * Send a tap event at the specified location.
     *
     * @param x the x coordinate of where to click
     * @param y the y coordinate of where to click
     * @return success or not
     * @throws IOException on error communicating with the device
     */
    public boolean tap(int x, int y) throws IOException {
        return sendMonkeyEvent("tap " + x + " " + y);
    }

    /**
     * Type the following string to the monkey.
     *
     * @param text the string to type
     * @return success
     * @throws IOException on error communicating with the device
     */
    public boolean type(String text) throws IOException {
        // The network protocol can't handle embedded line breaks, so we have to handle it
        // here instead
        StringTokenizer tok = new StringTokenizer(text, "\n", true);
        while (tok.hasMoreTokens()) {
            String line = tok.nextToken();
            if ("\n".equals(line)) {
                boolean success = press(PhysicalButton.ENTER);
                if (!success) {
                    return false;
                }
            } else {
                boolean success = sendMonkeyEvent("type " + line);
                if (!success) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Type the character to the monkey.
     *
     * @param keyChar the character to type.
     * @return success
     * @throws IOException on error communicating with the device
     */
    public boolean type(char keyChar) throws IOException {
        return type(Character.toString(keyChar));
    }

    /**
     * Wake the device up from sleep.
     * @throws IOException on error communicating with the device
     */
    public void wake() throws IOException {
        sendMonkeyEvent("wake");
    }


    /**
     * Retrieves the list of view ids from the current application.
     * @return the list of view ids as a collection of strings
     * @throws IOException on error communicating with the device
     */
    public Collection<String> listViewIds() throws IOException {
        synchronized (this) {
            String response = sendMonkeyEventAndGetResponse("listviews");
            if (!parseResponseForSuccess(response)) {
                Collections.emptyList();
            }
            String extras = parseResponseForExtra(response);
            return Lists.newArrayList(extras.split(" "));
        }
    }

    /**
     * Queries the on-screen view with the given id and returns the response.
     * It's up to the calling method to parse the returned String.
     * @param idType The type of ID to query the view by
     * @param id The view id of the view
     * @param query the query
     * @return the response from the query
     * @throws IOException on error communicating with the device
     */
    public String queryView(String idType, List<String> ids, String query) throws IOException {
        StringBuilder monkeyCommand = new StringBuilder("queryview " + idType + " ");
        for(String id : ids) {
            monkeyCommand.append(id).append(" ");
        }
        monkeyCommand.append(query);
        synchronized (this) {
            String response = sendMonkeyEventAndGetResponse(monkeyCommand.toString());
            if (!parseResponseForSuccess(response)) {
                throw new ChimpException(parseResponseForExtra(response));
            }
            return parseResponseForExtra(response);
        }
    }

    /**
     * Returns the current root view of the device.
     * @return the root view of the device
     */
    public IChimpView getRootView() throws IOException {
        synchronized(this) {
            String response = sendMonkeyEventAndGetResponse("getrootview");
            String extra = parseResponseForExtra(response);
            List<String> ids = Arrays.asList(extra.split(" "));
            if (!parseResponseForSuccess(response) || ids.size() != 2) {
                throw new ChimpException(extra);
            }
            ChimpView root = new ChimpView(ChimpView.ACCESSIBILITY_IDS, ids);
            root.setManager(this);
            return root;
        }
    }

    /**
     * Queries the device for a list of views with the given
     * @return A string containing the accessibility ids of the views with the given text
     */
    public String getViewsWithText(String text) throws IOException {
        synchronized(this) {
            // Monkey has trouble parsing a single word in quotes
            if (text.split(" ").length > 1) {
                text = "\"" + text + "\"";
            }
            String response = sendMonkeyEventAndGetResponse("getviewswithtext " + text);
            if (!parseResponseForSuccess(response)) {
                throw new ChimpException(parseResponseForExtra(response));
            }
            return parseResponseForExtra(response);
        }
    }
}
