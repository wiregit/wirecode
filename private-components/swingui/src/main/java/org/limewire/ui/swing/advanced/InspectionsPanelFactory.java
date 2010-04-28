package org.limewire.ui.swing.advanced;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

/**
 * Factory that uses reflection to get the testing only InspectionsPanel class from the injector.
 */
public class InspectionsPanelFactory {
    
    private final Injector injector;
    
    @Inject
    public InspectionsPanelFactory(Injector injector) {
        this.injector = injector;
    }
    
    @SuppressWarnings("unchecked")
    public Provider<TabPanel> getProvider() {
        try {
              return injector.<TabPanel>getProvider((Class<TabPanel>)Class.forName("org.limewire.ui.swing.advanced.InspectionsPanel"));
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("InspectionPanel class was not found, this class is only included for CVS builds");
        }
    }
}
