package org.limewire.ui.swing;

import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;

public class Main {
    
    public static void main(String[] args) throws Throwable {
        Frame splash = null;
        
        // show initial splash screen only if there are no arguments
        if (args == null || args.length == 0)
            splash = showInitialSplash();
        
        new Initializer().initialize(args, splash);
    }
    
    /**
     * Shows the initial splash window.
     */
    private static Frame showInitialSplash() {
        Frame splashFrame = null;
        Image image = null;
        URL imageURL = ClassLoader.getSystemResource("org/limewire/ui/swing/mainframe/resources/splash.png");
        if (imageURL != null) {
            image = Toolkit.getDefaultToolkit().createImage(imageURL);
            if (image != null) {
                splashFrame = AWTSplashWindow.splash(image);
            }
        }

            
        return splashFrame;
    }

}
