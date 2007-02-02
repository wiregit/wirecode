package com.limegroup.gnutella.gui;

import com.limegroup.gnutella.RouterService;

public class GUITestUtils {

    public static void initializeUI() {
        new RouterService(new VisualConnectionCallback());
    }
    
}
