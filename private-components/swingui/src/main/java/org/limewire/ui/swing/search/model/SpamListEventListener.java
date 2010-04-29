package org.limewire.ui.swing.search.model;

import org.limewire.core.api.spam.SpamManager;

/**
 * A listener to handle updates to the list of visual search results.  This
 * listener installs a PropertyChangeListener to each VisualSearchResult to 
 * handle changes to its "spam-ui" and "spam-core" properties. The core SpamManager
 * is notified about changes to the "spam-ui" property but not the "spam-core"
 * property, since it already knows about those changes.
 */
class SpamListEventListener implements VisualSearchResultStatusListener {

    private final SpamManager spamManager;
    
    /**
     * Constructs a SpamListEventListener with the specified spam manager and
     * similar results detector.
     */
    public SpamListEventListener(SpamManager spamManager) {
        this.spamManager = spamManager;
    }
    
    @Override
    public void resultCreated(VisualSearchResult vsr) {
    }
    
    @Override
    public void resultsCleared() {
    }
    
    
    @Override
    public void resultChanged(VisualSearchResult visualSearchResult, 
            String propertyName, Object oldValue, Object newValue) {        
        if ("spam-ui".equals(propertyName)) {
            boolean oldSpam = (Boolean)oldValue;
            boolean newSpam = (Boolean)newValue;
            if (oldSpam != newSpam) {
                spamChanged(visualSearchResult, newSpam, true);
            }
        } else if ("spam-core".equals(propertyName)) {
            boolean oldSpam = (Boolean)oldValue;
            boolean newSpam = (Boolean)newValue;
            if (oldSpam != newSpam) {
                spamChanged(visualSearchResult, newSpam, false);
            }
        }
    }
    
    private void spamChanged(VisualSearchResult visualSearchResult, boolean newSpam, boolean fromUI) {
        if (newSpam) {
            if (fromUI)
                spamManager.handleUserMarkedSpam(visualSearchResult.getCoreSearchResults());
        } else {
            if (fromUI)
                spamManager.handleUserMarkedGood(visualSearchResult.getCoreSearchResults());
        }
    }
}
