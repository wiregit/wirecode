package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;
import java.net.URISyntaxException;

import javax.swing.Action;

import org.limewire.core.api.search.store.StoreAuthState;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.search.store.StoreBrowserPanel;
import org.limewire.ui.swing.search.store.StoreController;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * The Store menu in the main menubar.
 */
class StoreMenu extends MnemonicMenu {
    
    private static final Log LOG = LogFactory.getLog(StoreMenu.class);

    private final Provider<StoreController> storeController;
    private final Action loginAction;
    private final Action logoutAction;

    /**
     * Constructs a StoreMenu with the specified controller provider.
     */
    @Inject
    public StoreMenu(Provider<StoreController> storeController) {
        super(I18n.tr("&Store"));
        this.storeController = storeController;
        loginAction = new LoginAction();
        logoutAction = new LogoutAction();
    }
    
    /**
     * Registers a listener on the specified store manager to handle login
     * changes.
     */
    @Inject
    void register(ListenerSupport<StoreAuthState> listenerSupport) {
        EventListener<StoreAuthState> storeAuthListener = new EventListener<StoreAuthState>() {
            @Override
            public void handleEvent(StoreAuthState event) {
                loginAction.setEnabled(!event.isLoggedIn());
                logoutAction.setEnabled(event.isLoggedIn());
            }
        };
        listenerSupport.addListener(storeAuthListener);
    }

    @Override
    public void createMenuItems() {
        add(loginAction);
        add(logoutAction);
    }

    /**
     * Menu action to log in to Lime Store.
     */
    private class LoginAction extends AbstractAction {
        
        public LoginAction() {
            super(I18n.tr("Log in"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            StoreController controller = storeController.get();
            StoreBrowserPanel browserPanel = new StoreBrowserPanel(controller);
            try {
                browserPanel.showLogin();
            } catch (URISyntaxException e1) {
                LOG.error(e1.getMessage(), e1);
            }
        }
    }

    /**
     * Menu action to log out of Lime Store.
     */
    private class LogoutAction extends AbstractAction {
        
        public LogoutAction() {
            super(I18n.tr("Log out"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            StoreController controller = storeController.get();
            controller.logout();
        }
    }
}
