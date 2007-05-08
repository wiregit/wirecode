package com.limegroup.gnutella.gui;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import com.limegroup.gnutella.RouterService;

public class GUITestUtils {

    public static void initializeUI() {
        new RouterService(new VisualConnectionCallback());
    }
    
    public static void waitForSwing() throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
            }            
        });
    }
    
}
