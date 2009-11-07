package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.limewire.core.api.search.store.StoreListener;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.search.store.StoreBrowserPanel;
import org.limewire.ui.swing.search.store.StoreController;
import org.limewire.ui.swing.search.store.StoreControllerFactory;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * The Store menu in the main menubar.
 */
class StoreMenu extends MnemonicMenu {

    private final StoreControllerFactory storeControllerFactory;
    private final Action loginAction;
    private final Action logoutAction;
    
    /**
     * Constructs a StoreMenu with the specified controller factory.
     */
    @Inject
    public StoreMenu(StoreControllerFactory storeControllerFactory) {
        super(I18n.tr("&Store"));
        
        this.storeControllerFactory = storeControllerFactory;
        
        loginAction = new LoginAction();
        logoutAction = new LogoutAction();
    }
    
    /**
     * Registers a listener on the specified store manager to handle login
     * changes.
     */
    @Inject
    void register(StoreManager storeManager) {
        // Add store listener to enable/disable actions.
        storeManager.addStoreListener(new StoreListener() {
            @Override
            public void loginChanged(boolean loggedIn) {
                loginAction.setEnabled(!loggedIn);
                logoutAction.setEnabled(loggedIn);
            }
        });
        
        // Initialize action state.
        loginAction.setEnabled(!storeManager.isLoggedIn());
        logoutAction.setEnabled(storeManager.isLoggedIn());
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
            StoreController controller = storeControllerFactory.create();
            StoreBrowserPanel browserPanel = new StoreBrowserPanel(controller);
            browserPanel.showLogin();
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
            StoreController controller = storeControllerFactory.create();
            controller.logout();
        }
    }
}
