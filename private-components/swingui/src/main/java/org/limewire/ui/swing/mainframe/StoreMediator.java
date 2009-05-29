package org.limewire.ui.swing.mainframe;

import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.browser.BrowserUtils;
import org.limewire.ui.swing.browser.UriAction;
import org.limewire.ui.swing.library.nav.NavMediator;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class StoreMediator implements NavMediator<StorePanel> {
    public static final String NAME = "LimeWire Store";
    
    private final Provider<StorePanel> storePanel;
    private StorePanel store;
    
    @Inject
    public StoreMediator(Provider<StorePanel> storePanel, final Navigator navigator) {
        this.storePanel = storePanel;
        
        BrowserUtils.addTargetedUrlAction("_lwStore", new UriAction() {
            @Override
            public boolean uriClicked(final TargetedUri targetedUrl) {
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        navigator.getNavItem(NavCategory.LIMEWIRE, StoreMediator.NAME).select();
                        getComponent().load(targetedUrl.getUri());
                    }
                });
                return true;
            }
        }); 
    }
    
    @Override
    public StorePanel getComponent() {
        if(store == null)
            store = storePanel.get();
        return store;
    }
}
