package org.limewire.ui.swing.tray;

import java.util.EventObject;

import org.limewire.core.settings.UISettings;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/** This class acts as a proxy for a platform-specific user notification class. */
@Singleton
class TrayNotifierProxy implements TrayNotifier {

    /** The NotifyUser object that this class is serving as a proxy for. */
    private TrayNotifier notifier;

    /** Flag for whether or not the application is currently in the tray. */
    private boolean inTray;

    @Inject
    TrayNotifierProxy(TrayManager trayManager) {
        if (OSUtils.supportsTray() && trayManager.isTrayLibraryLoaded()) {
        	notifier = new JDICNotifier();
        	// If add notifications failed, we're screwed.
            if(!showTrayIcon()) {
                notifier = new BasicNotifier();
            }
        } else if (OSUtils.isMacOSX()) {
            notifier = new GrowlNotifier();
        } else {
            notifier = new BasicNotifier();
        }        
    }
    
    public boolean supportsSystemTray() {
        return notifier.supportsSystemTray();
    }

    public boolean showTrayIcon() {
        if (inTray)
            return true;
        boolean notify = notifier.showTrayIcon();
        inTray = true;
        return notify;
    }

    public void hideTrayIcon() {
        if (!inTray)
            return;
        notifier.hideTrayIcon();
        inTray = false;
    }
    
    public void hideMessage(Notification notification) {
        notifier.hideMessage(notification);
    }

    public void showMessage(Notification notification) {
        if (!UISettings.SHOW_NOTIFICATIONS.getValue()) {
            return;
        }
        
        notifier.showMessage(notification);
    }
    
    @Override
    public boolean isExitEvent(EventObject event) {
        return notifier.isExitEvent(event);
    }

}
