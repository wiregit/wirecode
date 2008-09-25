package org.limewire.ui.swing.mainframe;

import org.limewire.ui.swing.friends.FriendsCountUpdater;
import org.limewire.ui.swing.search.SearchNavigator;

import com.google.inject.AbstractModule;


public class LimeWireUiMainframeModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(SearchNavigator.class).to(TopPanel.class);
        bind(FriendsCountUpdater.class).to(StatusPanel.class);
        bind(UnseenMessageListener.class).to(StatusPanel.class);
    }
}
