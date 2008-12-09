package org.limewire.ui.swing.tray;

import org.limewire.service.ErrorService;

class Growl {
    
    static {
        try {
            System.loadLibrary("Growl");
        } catch (UnsatisfiedLinkError err) {
            ErrorService.error(err);
        }
    }
    
    public Growl() {
        RegisterGrowl();
    }
    
    public void showMessage (String message) {
        SendNotification(message);
    }
    
    private static final native void RegisterGrowl();
    private static final native void SendNotification(String message);
    
}
