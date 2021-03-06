package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;
import java.util.Random;

import org.limewire.activation.api.ActivationManager;
import org.limewire.core.api.Application;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.DelayedMenuItemCreator;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.action.UrlAction;
import org.limewire.ui.swing.mainframe.AboutAction;
import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

class HelpMenu extends MnemonicMenu implements DelayedMenuItemCreator  {

    private final Application application;
    
    private final Provider<TrayNotifier> trayNotifierProvider;
    private final Provider<AboutAction> aboutAction;
    private final ActivationManager activationManager;
    
    @Inject
    public HelpMenu(Application application, 
            Provider<TrayNotifier> trayNotifierProvider,
            Provider<AboutAction> aboutAction,
            ActivationManager activationManager) {
        
        super(I18n.tr("&Help"));

        this.application = application;
        
        this.trayNotifierProvider = trayNotifierProvider;
        this.aboutAction = aboutAction;
        this.activationManager = activationManager;
    }

    @Override
    public void createMenuItems() {       
                
        if(!activationManager.isProActive()) {
            add(new UrlAction(I18n.tr("&Tech Support"),"http://www.limewire.com/client_redirect/?page=gopro", application));
        } else {
            add(new UrlAction(I18n.tr("&Tech Support"), "http://www.limewire.com/client_redirect/?page=prosupport", application));
        }
        
        add(new UrlAction(I18n.tr("&Using LimeWire"), "http://www.limewire.com/client_redirect/?page=support", application));
        
        add(new UrlAction(I18n.tr("&FAQ"), "http://www.limewire.com/client_redirect/?page=faq", application));

        add(new UrlAction(I18n.tr("&Forum"), "http://www.limewire.com/forum.htm", application));

        if(application.isBetaVersion()) {
            add(new UrlAction(I18n.tr("&Give Feedback"), "http://www.limewire.com/client_redirect/?page=betaTesting", application));
        }
        
        if (!OSUtils.isMacOSX()) {
            addSeparator();
            add(aboutAction.get());
        }

        if (application.isTestingVersion()) {
            addSeparator();
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
                        Notification notification = new Notification("This is a not tooo long message title",
                                "This is a super looooooooooooooooooooooooooooooooong message.",
                                this);
                        trayNotifierProvider.get().showMessage(notification);
                    } else if (new Random().nextBoolean()) {
                        Notification notification = new Notification("Long super loooooooooooooong loooon loooong message title",
                                "This is a another very loooong  loooong loooong loooong loooong loooong loooong loooong loooong message.",
                                this);
                        trayNotifierProvider.get().showMessage(notification);
                    } else {
                        Notification notification = new Notification("Short Title", "Short message.", this);
                        trayNotifierProvider.get().showMessage(notification);
                    }
                }
            });
        }
    }
}
