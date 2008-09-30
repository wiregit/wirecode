package org.limewire.ui.swing.search;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.ui.swing.search.model.SimilarResultsDetector;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

public class SpamListEventListener implements ListEventListener<VisualSearchResult> {

    private final SpamManager spamManager;

    private final SimilarResultsDetector similarResultsDetector;

    public SpamListEventListener(SpamManager spamManager,
            SimilarResultsDetector similarResultsDetector) {
        this.spamManager = spamManager;
        this.similarResultsDetector = similarResultsDetector;
    }

    @Override
    public void listChanged(ListEvent<VisualSearchResult> listChanges) {
        final EventList<VisualSearchResult> eventList = listChanges.getSourceList();

        while (listChanges.next()) {
            boolean added = listChanges.getType() == ListEvent.INSERT;
            if (added) {
                final VisualSearchResult visualSearchResult = eventList.get(listChanges.getIndex());
                visualSearchResult.addPropertyChangeListener(new PropertyChangeListener() {

                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        if ("spam".equals(evt.getPropertyName())) {
                            boolean oldSpam = (Boolean) evt.getOldValue();
                            boolean newSpam = (Boolean) evt.getNewValue();
                            if (oldSpam != newSpam) {
                                if (newSpam) {
                                    for (SearchResult searchResult : visualSearchResult
                                            .getCoreSearchResults()) {
                                        spamManager.handleUserMarkedSpam(searchResult);
                                    }
                                    VisualSearchResult parent = visualSearchResult
                                            .getSimilarityParent();
                                    if (parent == null) {
                                        //null parent means you are the parent
                                        pickNewParent(visualSearchResult);
                                    } else {
                                        removeItemFromPArent(visualSearchResult, parent);
                                    }
                                } else {
                                    for (SearchResult searchResult : visualSearchResult
                                            .getCoreSearchResults()) {
                                        spamManager.handleUserMarkedGood(searchResult);
                                    }
                                    similarResultsDetector.detectSimilarResult(eventList, visualSearchResult);
                                }
                            }
                        }
                    }

                    private void removeItemFromPArent(final VisualSearchResult visualSearchResult,
                            VisualSearchResult parent) {
                        parent.removeSimilarSearchResult(visualSearchResult);
                        visualSearchResult.setSimilarityParent(null);
                        visualSearchResult.setChildrenVisible(false);
                        visualSearchResult.setVisible(true);
                    }

                    private void pickNewParent(final VisualSearchResult visualSearchResult) {
                        VisualSearchResult newParent = null;
                        if (visualSearchResult.getSimilarResults().size() > 0) {
                            newParent = visualSearchResult.getSimilarResults().get(
                                    0);
                            newParent.setSimilarityParent(null);
                            visualSearchResult.removeSimilarSearchResult(newParent);

                        }

                        for (VisualSearchResult simResult : visualSearchResult
                                .getSimilarResults()) {
                            visualSearchResult.removeSimilarSearchResult(simResult);
                            if (newParent != null) {
                                newParent.addSimilarSearchResult(simResult);
                            }
                            simResult.setSimilarityParent(newParent);
                        }

                        if (newParent != null) {
                            newParent.setChildrenVisible(visualSearchResult
                                    .isChildrenVisible());
                            newParent.setVisible(true);
                        }
                        visualSearchResult.setChildrenVisible(false);
                        visualSearchResult.setVisible(true);
                    }

                });
            }
        }
    }
}
