package org.limewire.ui.swing.mainframe;

import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.library.nav.NavMediator;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class StoreMediator implements NavMediator<StorePanel> {
    public static final String NAME = "LimeWire Store";
    
    private final Provider<StorePanel> storePanel;
    private StorePanel store;
    
    @Inject
    public StoreMediator(Provider<StorePanel> storePanel) {
        this.storePanel = storePanel;
    }
    
    @Override
    public StorePanel getComponent() {
        if(store == null)
            store = storePanel.get();
        return store;
    }
}
