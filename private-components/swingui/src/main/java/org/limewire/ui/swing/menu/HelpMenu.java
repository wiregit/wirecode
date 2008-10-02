package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JMenu;

import org.limewire.core.api.Application;
import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class HelpMenu extends JMenu {

    @Inject
    public HelpMenu(Application application, final IconManager iconManager,
            final TrayNotifier trayNotifier) {
        super(I18n.tr("Help"));
        
        if(application.isTestingVersion()) {
            add(new AbstractAction("Error Test") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    throw new RuntimeException("Test Error");
                }
            });
    
            add(new AbstractAction("Tray Test") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (new Random().nextBoolean()) {
                        Icon icon = iconManager.getIconForFile(new File("limewire.exe"));
                        Notification notification = new Notification(
                                "This is a very looooooooooooooooooooooooooooooooong message.",
                                icon, this);
                        trayNotifier.showMessage(notification);
                    } else if (new Random().nextBoolean()) {
                        Icon icon = iconManager.getIconForFile(new File("limewire.html"));
                        Notification notification = new Notification(
                                "This is a another very loooong  loooong loooong loooong loooong loooong loooong loooong loooong message.",
                                icon, this);
                        trayNotifier.showMessage(notification);               
                    } else {
                        Icon icon = iconManager.getIconForFile(new File("limewire.html"));
                        Notification notification = new Notification(
                                "Short message.",
                                icon, this);
                        trayNotifier.showMessage(notification);               
                    }
                }
            });
        }
    }
}
