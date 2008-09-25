package org.limewire.ui.swing.search;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

public class SpamListEventListener implements ListEventListener<VisualSearchResult> {

    private final SpamManager spamManager;

    public SpamListEventListener(SpamManager spamManager) {
        this.spamManager = spamManager;
    }

    @Override
    public void listChanged(ListEvent<VisualSearchResult> listChanges) {
        EventList<VisualSearchResult> eventList = listChanges.getSourceList();

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
                                } else {
                                    for (SearchResult searchResult : visualSearchResult
                                            .getCoreSearchResults()) {
                                        spamManager.handleUserMarkedGood(searchResult);
                                    }
                                }
                            }
                        }
                    }

                });
            }
        }
    }
}
