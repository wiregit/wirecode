package org.limewire.ui.swing.mainframe;

import javax.swing.JLayeredPane;

import org.limewire.ui.swing.search.SearchNavigator;
import org.limewire.ui.swing.warnings.DocumentWarningController;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

/**
 * Module to configure Guice bindings for the main UI frame classes.
 */
public class LimeWireUiMainframeModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(SearchNavigator.class).to(TopPanel.class);
        bind(JLayeredPane.class).annotatedWith(GlobalLayeredPane.class).toInstance(new JLayeredPane());
        bind(DocumentWarningController.class).asEagerSingleton();
        bind(BottomHeaderFactory.class).toProvider(
                FactoryProvider.newFactory(BottomHeaderFactory.class, BottomHeaderPanel.class));
    }
}
