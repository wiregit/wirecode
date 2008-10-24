package org.limewire.ui.swing;

import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.lang.reflect.Method;
import java.net.URL;

public class Main {
    
    public static void main(String[] args) {
        Frame splash = null;
        Image splashImage = getSplashImage();
        
        // show initial splash screen only if there are no arguments
        if (args == null || args.length == 0) {
            if(splashImage != null) {
                splash = AWTSplashWindow.splash(splashImage);
            }
        }
        
        // load the GUI through reflection so that we don't reference classes here,
        // which would slow the speed of class-loading, causing the splash to be
        // displayed later.
        try {
            Class<?> loadClass = Class.forName("org.limewire.ui.swing.GuiLoader");
            Object loadInstance = loadClass.newInstance();
            Method loadMethod = loadClass.getMethod("load", new Class[] { String[].class, Frame.class, Image.class } );
            loadMethod.invoke(loadInstance, args, splash, splashImage);
        } catch(Throwable t) {
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
