package org.limewire.ui.swing;

import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;

public class Main {
    
    public static void main(String[] args) throws Throwable {
        Frame splash = null;
        Image splashImage = null;
        
        // show initial splash screen only if there are no arguments
        if (args == null || args.length == 0) {
            splashImage = getSplashImage();
            if(splashImage != null) {
                splash = AWTSplashWindow.splash(splashImage);
            }
        }
        
        try {
            new Initializer().initialize(args, splash, splashImage);
        } catch(Throwable t) {
            if(splash != null) {
                try {
                    splash.dispose();
                } catch(Throwable ignored) {}
            }
            
            t.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Gets the image to be used as the splash.
     */
    private static Image getSplashImage() {
        URL imageURL = ClassLoader.getSystemResource("org/limewire/ui/swing/mainframe/resources/splash.png");
        if (imageURL != null) {
            return Toolkit.getDefaultToolkit().createImage(imageURL);
        } else {
            return null;
        }
    }

}
