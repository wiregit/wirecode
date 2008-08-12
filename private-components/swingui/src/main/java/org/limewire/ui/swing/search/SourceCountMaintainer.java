package org.limewire.ui.swing.search;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

public class SourceCountMaintainer implements ListEventListener<VisualSearchResult> {
    
    private final EventList<VisualSearchResult> source;
    private final Action countAction;
    private final List<Integer> countList;
    private int totalCount = 0;
    
    public SourceCountMaintainer(EventList<VisualSearchResult> resultsEventList, Action action) {
        this.source = resultsEventList;
        this.countAction = action;
        this.countList = new ArrayList<Integer>();
        
        // Do the initial sync
        for(VisualSearchResult result : resultsEventList) {
            int size = result.getSources().size();
            countList.add(size);
            totalCount += size;
        }
        countAction.putValue(Action.NAME, totalCount == 0 ? null : String.valueOf(totalCount));
        // Then add future listeners.
        source.addListEventListener(this);
    }
    
    public void dispose() {
        source.removeListEventListener(this);
    }

    @Override
    public void listChanged(ListEvent<VisualSearchResult> listChanges) {
        if(!listChanges.isReordering()) {                
            while(listChanges.next()) {
                int changeIndex = listChanges.getIndex();
                int changeType = listChanges.getType();
                int newSources;
                switch(changeType) {
                case ListEvent.INSERT:
                    newSources = source.get(changeIndex).getSources().size();
                    totalCount += newSources;
                    countList.add(changeIndex, newSources);
                    break;
                case ListEvent.DELETE:
                    totalCount -= countList.remove(changeIndex);
                    break;
                case ListEvent.UPDATE:
                    newSources = source.get(changeIndex).getSources().size();
                    totalCount += newSources;
                    totalCount -= countList.set(changeIndex, newSources);
                    break;
                default: throw new IllegalStateException("invalid type: " + changeType);
                }
            }
            countAction.putValue(Action.NAME, totalCount == 0 ? null : String.valueOf(totalCount));
        } else {
            countList.clear();
            for(VisualSearchResult result : source) {
                countList.add(result.getSources().size());
            }
        }
    }

}
