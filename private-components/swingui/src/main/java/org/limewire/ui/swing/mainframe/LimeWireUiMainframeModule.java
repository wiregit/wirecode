package org.limewire.ui.swing.mainframe;

import org.limewire.ui.swing.nav.NavigableTarget;
import org.limewire.ui.swing.nav.NavigableTree;
import org.limewire.ui.swing.search.SearchNavigator;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;


public class LimeWireUiMainframeModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(NavigableTarget.class).annotatedWith(Names.named("MainTarget")).to(MainPanel.class);
        bind(NavigableTree.class).annotatedWith(Names.named("MainTree")).to(LeftPanel.class);
        bind(SearchNavigator.class).to(TopPanel.class);
    }
    

}
