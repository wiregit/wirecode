package org.limewire.ui.swing.mainframe;

import org.limewire.core.api.Application;
import org.limewire.core.api.search.store.StoreEnabled;
import org.limewire.inject.EagerSingleton;
import org.limewire.ui.swing.browser.BrowserUtils;
import org.limewire.ui.swing.browser.UriAction;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavMediator;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

@EagerSingleton
public class StoreMediator implements NavMediator<StorePanel> {
    public static final String NAME = "LimeWire Store";
    
    private Provider<StorePanel> store;
    private StorePanel storePanel;
    private Navigator navigator;
    private Application application;
    private final Provider<Boolean> storeEnabled;

    @Inject
    public StoreMediator(Provider<StorePanel> storePanel, Navigator navigator,
            Application application, @StoreEnabled Provider<Boolean> storeEnabled) {
        this.store = storePanel;
        this.navigator = navigator;
        this.application = application;
        this.storeEnabled = storeEnabled;

        BrowserUtils.addTargetedUrlAction("_lwStore", new UriAction() {
            @Override
            public boolean uriClicked(final TargetedUri targetedUrl) {
                SwingUtils.invokeNowOrLater(new Runnable() {
                    @Override
                    public void run() {
                        load(targetedUrl.getUri());
                    }
                });
                return true;
            }
        }); 
    }
    
    @Override
    public StorePanel getComponent() {
        if(storePanel == null)
            storePanel = store.get();
        return storePanel;
    }
    
    public void load(String url) {
        if(storeEnabled.get()) {
            navigator.getNavItem(NavCategory.LIMEWIRE, StoreMediator.NAME).select();
            getComponent().load(url); 
        } else {
            NativeLaunchUtils.openURL(application.addClientInfoToUrl(url));
        }
    }
}
