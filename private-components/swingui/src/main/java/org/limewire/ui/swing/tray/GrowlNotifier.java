package org.limewire.ui.swing.tray;

import java.util.EventObject;

class GrowlNotifier implements TrayNotifier {
    
    private final Growl mGrowl;
    
    public GrowlNotifier() {
        mGrowl = new Growl();
    }
    
    public boolean supportsSystemTray() {
        return false;
    }

    public boolean showTrayIcon() {
        return false;
    }

    public void hideTrayIcon() {
        
    }

    public void showMessage(Notification notification) {
        mGrowl.showMessage(notification.getMessage());
    }
    
    public void hideMessage(Notification notification) {
        
    }

    public void updateUI() {
        
    }
    
    @Override
    public boolean isExitEvent(EventObject event) {
        return false;
    }
    
}
