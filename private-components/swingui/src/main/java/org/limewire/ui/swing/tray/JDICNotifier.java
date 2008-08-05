package org.limewire.ui.swing.tray;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdesktop.application.Application;
import org.jdesktop.application.Resource;
import org.jdesktop.jdic.tray.SystemTray;
import org.jdesktop.jdic.tray.TrayIcon;
import org.limewire.core.settings.UISettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;


/**
 * Puts an icon and menu in the system tray.
 * Works on Windows, Linux, and any other platforms JDIC supports.
 */
class JDICNotifier implements TrayNotifier {
	
    private static final Log LOG = LogFactory.getLog(DefaultNotificationRenderer.class);
    
    static {
        try {
            System.loadLibrary("jdic");
        } catch (UnsatisfiedLinkError ule) {}
    }
    
	private final SystemTray tray;
	private final TrayIcon trayIcon;
	private final NotificationWindow notificationWindow;
	private final JPopupMenu popupMenu;
	
	@Resource
	private Icon trayIconResource;
	
	public JDICNotifier() {
	    GuiUtils.assignResources(this);
		tray = SystemTray.getDefaultSystemTray();
		popupMenu = buildPopupMenu();
		trayIcon = buildTrayIcon(I18n.tr("LimeWire"));
		notificationWindow = buildNotificationWindow();
	}

	private TrayIcon buildTrayIcon(String desc) {
        //String tip = "LimeWire: Running the Gnutella Network";
        TrayIcon icon = new TrayIcon(trayIconResource, desc, popupMenu);
        
    	// left click restores.  This happens on the awt thread.
        icon.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        	    ActionMap map = Application.getInstance().getContext().getActionManager().getActionMap();
        	    map.get("restoreView").actionPerformed(e);
        	}
        });
        
        icon.setIconAutoSize(true);
        return icon;
	}
	
	private JPopupMenu buildPopupMenu() {
		JPopupMenu menu = new TrayPopupMenu();
		
		// restore
		JMenuItem item = new JMenuItem(I18n.tr("Restore"));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    ActionMap map = Application.getInstance().getContext().getActionManager().getActionMap();
                map.get("restoreView").actionPerformed(e);
			}
		});
		menu.add(item);
		
		menu.addSeparator();
		
		// about box
		item = new JMenuItem(I18n.tr("About"));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    ActionMap map = Application.getInstance().getContext().getActionManager().getActionMap();
                map.get("showAboutWindow").actionPerformed(e);
			}
		});
		menu.add(item);
		
		menu.addSeparator();
		
		//exit after transfers
		item = new JMenuItem(I18n.tr("Exit after Transfers"));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    ActionMap map = Application.getInstance().getContext().getActionManager().getActionMap();
                map.get("shutdownAfterTransfers").actionPerformed(e);
			}
		});
		menu.add(item);
		
		// exit
		item = new JMenuItem(I18n.tr("Exit"));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Application.getInstance().exit(e);
			}
		});
		menu.add(item);
		
		return menu;
	}
	
	@Override
	public boolean isExitEvent(EventObject event) {
	    if(event != null && event.getSource() instanceof JMenuItem) {
	        JMenuItem item = (JMenuItem)event.getSource();
	        return item.getParent() == popupMenu;
	    } else {
	        return false;
	    }
	}
	
	private NotificationWindow buildNotificationWindow() {
		NotificationWindow window = new NotificationWindow(GuiUtils.getMainFrame());
		window.setLocationOffset(new Dimension(2, 7));
		window.setTitle("LimeWire");
		window.setIcon(trayIconResource);
		return window;
	}
	
	public boolean showTrayIcon() {
	    if(trayIconResource == null) {
	        return false;
	    }
	    
	    try {
	        tray.addTrayIcon(trayIcon);
	    } catch(IllegalArgumentException iae) {
	        // Sometimes JDIC can't load the trayIcon :(
	        return false;
	    }

        // XXX use the actual icon size once the necessary call is available in JDIC 
        //notificationWindow.setParentSize(_icon.getSize());
        notificationWindow.setParentSize(new Dimension(22, 22));

        return true;
	}
	
	public boolean supportsSystemTray() {
	    return trayIconResource != null;
	}

	public void hideTrayIcon() {
		tray.removeTrayIcon(trayIcon);
		notificationWindow.setParentLocation(null);
		notificationWindow.setParentSize(null);
	}

	public void showMessage(Notification notification) {
	    try {
	        notificationWindow.addNotification(notification);
	        try {
	            notificationWindow.setParentLocation(trayIcon.getLocationOnScreen());
	        } catch (NullPointerException ignore) {
	            // thrown if the native peer is not found (GUI-273)?
	        }
	        notificationWindow.showWindow();
        } catch (Exception e) {
            // see GUI-239
            LOG.error("Disabling notifications due to error", e);
            UISettings.SHOW_NOTIFICATIONS.setValue(false);
            notificationWindow.hideWindowImmediately();
        }
	}

	public void hideMessage(Notification notification) {
		notificationWindow.removeNotification(notification);
	}

    public void updateUI() {
        SwingUtilities.updateComponentTreeUI(notificationWindow);
    }
	
}
