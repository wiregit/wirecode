package org.limewire.ui.swing.tray;



class Growl {
    
    private static boolean loaded = false;
    
    static {
        boolean growlLoaded = false;
        try {
            System.loadLibrary("Growl");
            growlLoaded = true;
        } catch (UnsatisfiedLinkError err) {
            growlLoaded = false;
        }
        loaded = growlLoaded;
    }
    
    public Growl() {
        if(loaded) {
            RegisterGrowl();
        }
    }
    
    public void showMessage (String message) {
        if(loaded) {
            SendNotification(message);
        }
    }
    
    private static final native void RegisterGrowl();
    private static final native void SendNotification(String message);
    
}
