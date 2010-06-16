package org.limewire.ui.swing.util;


/**
 * Utilities supporting Spoon actions.
 */
public class SpoonUtils {

    public static void handleSpoonURL(String url) {
        //TODO: check if spoon is installed, if so launch through spoon, other
        // wise, open in a browser.
        NativeLaunchUtils.openURL(url);
    }
}
