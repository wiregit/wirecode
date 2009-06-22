package org.limewire.ui.swing.mainframe;

import javax.swing.JLayeredPane;

import org.limewire.ui.swing.search.SearchNavigator;
import org.limewire.ui.swing.warnings.DocumentWarningPanel;

import com.google.inject.AbstractModule;


public class LimeWireUiMainframeModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(SearchNavigator.class).to(TopPanel.class);
        bind(JLayeredPane.class).annotatedWith(LimeWireLayeredPane.class).toInstance(new JLayeredPane());
        bind(DocumentWarningPanel.class);
    }
}
