package org.limewire.ui.swing.search.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.limewire.core.api.spam.SpamManager;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

/**
 * A listener to handle updates to the list of visual search results.  This
 * listener installs a PropertyChangeListener to each VisualSearchResult to 
 * handle changes to its "spam-ui" and "spam-core" properties. The
 * SimilarResultsDetector is notified when changes occur. The core SpamManager
 * is notified about changes to the "spam-ui" property but not the "spam-core"
 * property, since it already knows about those changes.
 */
class SpamListEventListener implements ListEventListener<VisualSearchResult>, PropertyChangeListener {

    private final SpamManager spamManager;

    private final SimilarResultsDetector similarResultsDetector;

    /**
     * Constructs a SpamListEventListener with the specified spam manager and
     * similar results detector.
     */
    public SpamListEventListener(SpamManager spamManager,
            SimilarResultsDetector similarResultsDetector) {
        this.spamManager = spamManager;
        this.similarResultsDetector = similarResultsDetector;
    }

    @Override
    public void listChanged(ListEvent<VisualSearchResult> listChanges) {
        final EventList<VisualSearchResult> eventList = listChanges.getSourceList();

        while (listChanges.next()) {
            if (listChanges.getType() == ListEvent.INSERT) {
                eventList.get(listChanges.getIndex()).addPropertyChangeListener(this);
            }
        };
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        VisualSearchResult visualSearchResult = (VisualSearchResult)evt.getSource();
        
        if ("spam-ui".equals(evt.getPropertyName())) {
            boolean oldSpam = (Boolean) evt.getOldValue();
            boolean newSpam = (Boolean) evt.getNewValue();
            if (oldSpam != newSpam)
                spamChanged(visualSearchResult, newSpam, true);
        } else if ("spam-core".equals(evt.getPropertyName())) {
            boolean oldSpam = (Boolean) evt.getOldValue();
            boolean newSpam = (Boolean) evt.getNewValue();
            if (oldSpam != newSpam)
                spamChanged(visualSearchResult, newSpam, false);                            
        }
    }
    
    private void spamChanged(VisualSearchResult visualSearchResult, boolean newSpam, boolean fromUI) {
        if (newSpam) {
            if (fromUI) {
                spamManager.handleUserMarkedSpam(visualSearchResult.getCoreSearchResults());
            }
            VisualSearchResult parent = visualSearchResult.getSimilarityParent();
            VisualSearchResult newParent = null;
            if (parent == null) {
                //null parent means you are the parent
                newParent = pickNewParent(visualSearchResult);
            } else {
                removeItemFromParent(visualSearchResult, parent);
            }

            similarResultsDetector.removeSpamItem(visualSearchResult, newParent);
        } else {
            if (fromUI)
                spamManager.handleUserMarkedGood(visualSearchResult.getCoreSearchResults());
            similarResultsDetector.detectSimilarResult(visualSearchResult);
        }
    }

    private void removeItemFromParent(final VisualSearchResult visualSearchResult, VisualSearchResult parent) {
        parent.removeSimilarSearchResult(visualSearchResult);
        visualSearchResult.setSimilarityParent(null);
        visualSearchResult.setChildrenVisible(false);
        visualSearchResult.setVisible(true);
    }

    private VisualSearchResult pickNewParent(final VisualSearchResult visualSearchResult) {
        VisualSearchResult newParent = null;
        if (visualSearchResult.getSimilarResults().size() > 0) {
            newParent = visualSearchResult.getSimilarResults().get(0);
            newParent.setSimilarityParent(null);
            visualSearchResult.removeSimilarSearchResult(newParent);

        }

        for (VisualSearchResult simResult : visualSearchResult.getSimilarResults()) {
            visualSearchResult.removeSimilarSearchResult(simResult);
            if (newParent != null) {
                newParent.addSimilarSearchResult(simResult);
            }
            simResult.setSimilarityParent(newParent);
        }

        if (newParent != null) {
            newParent.setChildrenVisible(visualSearchResult.isChildrenVisible());
            newParent.setVisible(true);
        }
        visualSearchResult.setChildrenVisible(false);
        //toggle visibility to ensure a repaint
        visualSearchResult.setVisible(false);
        visualSearchResult.setVisible(true);
        
        return newParent;
    }
}
