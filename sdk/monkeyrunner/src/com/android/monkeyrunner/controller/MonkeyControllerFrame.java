/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.monkeyrunner.controller;

import com.android.chimpchat.core.PhysicalButton;
import com.android.chimpchat.core.TouchPressType;
import com.android.chimpchat.core.IChimpImage;
import com.android.chimpchat.core.IChimpDevice;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Main window for MonkeyController.
 */
public class MonkeyControllerFrame extends JFrame {
    private static final Logger LOG = Logger.getLogger(MonkeyControllerFrame.class.getName());

    private final JButton refreshButton = new JButton("Refresh");
    private final JButton variablesButton = new JButton("Variable");
    private final JLabel imageLabel = new JLabel();
    private final VariableFrame variableFrame;

    private final IChimpDevice device;
    private BufferedImage currentImage;

    private final TouchPressType DOWN_AND_UP = TouchPressType.DOWN_AND_UP;

    private final Timer timer = new Timer(1000, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            updateScreen();
        }
    });

    private class PressAction extends AbstractAction {
        private final PhysicalButton button;

        public PressAction(PhysicalButton button) {
            this.button = button;
        }
        /* When this fails, it no longer throws a runtime exception,
         * but merely will log the failure.
         */
        public void actionPerformed(ActionEvent event) {
            device.press(button.getKeyName(), DOWN_AND_UP);
            updateScreen();
        }
    }

    private JButton createToolbarButton(PhysicalButton hardButton) {
        JButton button = new JButton(new PressAction(hardButton));
        button.setText(hardButton.getKeyName());
        return button;
    }

    public MonkeyControllerFrame(IChimpDevice chimpDevice) {
        super("MonkeyController");
        this.device = chimpDevice;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        JToolBar toolbar = new JToolBar();

        toolbar.add(createToolbarButton(PhysicalButton.HOME));
        toolbar.add(createToolbarButton(PhysicalButton.BACK));
        toolbar.add(createToolbarButton(PhysicalButton.SEARCH));
        toolbar.add(createToolbarButton(PhysicalButton.MENU));

        add(toolbar);
        add(refreshButton);
        add(variablesButton);
        add(imageLabel);

        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateScreen();
            }
        });

        variableFrame = new VariableFrame();
        variablesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                variableFrame.setVisible(true);
            }
        });

        /* Similar to above, when the following two methods fail, they
         * no longer throw a runtime exception, but merely will log the failure.
         */
        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                device.touch(event.getX(), event.getY(), DOWN_AND_UP);
                updateScreen();
            }

        });

        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addKeyEventDispatcher(new KeyEventDispatcher() {
            public boolean dispatchKeyEvent(KeyEvent event) {
                if (KeyEvent.KEY_TYPED == event.getID()) {
                    device.type(Character.toString(event.getKeyChar()));
                }
                return false;
            }
        });

        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                init();
                variableFrame.init(device);
            }
        });

        pack();
    }

    private void updateScreen() {
        IChimpImage snapshot = device.takeSnapshot();
        currentImage = snapshot.createBufferedImage();
        imageLabel.setIcon(new ImageIcon(currentImage));

        pack();
    }

    private void init() {
        updateScreen();
        timer.start();
    }

}
