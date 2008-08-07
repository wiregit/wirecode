package org.limewire.ui.swing.mainframe;

import org.limewire.ui.swing.nav.NavTree;
import org.limewire.ui.swing.nav.NavigableTarget;
import org.limewire.ui.swing.nav.NavigableTree;
import org.limewire.ui.swing.search.SearchNavigator;

import com.google.inject.AbstractModule;


public class LimeWireUiMainframeModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(NavigableTarget.class).to(MainPanel.class);
        bind(NavigableTree.class).to(NavTree.class);
        bind(SearchNavigator.class).to(TopPanel.class);
    }
    

}
