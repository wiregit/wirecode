/**
 * 
 */
package org.limewire.ui.swing.search.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

public class GroupingListEventListener implements ListEventListener<VisualSearchResult> {

    private List<Group> groups = new CopyOnWriteArrayList<Group>();

    public class Group {
        private final List<VisualSearchResult> items;

        private final SearchResultComparator searchResultComparator;

        public Group() {
            this.items = new CopyOnWriteArrayList<VisualSearchResult>();
            this.searchResultComparator = new NamesMatchComparator();
        }

        public Group(VisualSearchResult searchResult) {
            this();
            add(searchResult);
        }

        public void add(VisualSearchResult searchResult) {
            items.add(searchResult);
        }

        public boolean remove(VisualSearchResult searchResult) {
            boolean removed = items.remove(searchResult);
            return removed;
        }

        public boolean isInGroup(VisualSearchResult searchResult) {
            return items.contains(searchResult);
        }

        public boolean belongsInGroup(VisualSearchResult searchResult) {
            for (VisualSearchResult inGroup : items) {
                if (searchResultComparator.compare(inGroup, searchResult) == 0) {
                    return true;
                }
            }
            return false;
        }

        public VisualSearchResult getParent() {
            VisualSearchResult parent = null;
            for (VisualSearchResult result : items) {
                if (parent == null || result.getSize() > parent.getSize()) {
                    parent = result;
                }
            }
            return parent;
        }

        public void updateParent() {
            VisualSearchResult parent = getParent();
            for (VisualSearchResult result : items) {
                VisualSearchResult oldParent = result.getSimilarityParent();
                if (oldParent != parent) {
                    if (oldParent != null) {
                        ((SearchResultAdapter) oldParent).removeSimilarSearchResult(result);
                    }
                    
                    if (parent == result) {
                        ((SearchResultAdapter) result).setSimilarityParent(null);
                        result.setVisible(true);
                    } else {
                        ((SearchResultAdapter) parent).addSimilarSearchResult(result);
                        ((SearchResultAdapter) result).setSimilarityParent(parent);
                        result.setVisible(false);
                    }
                }
            }
        }

        public int size() {
            return items.size();
        }

        public String toString() {
            return items.toString();
        }

    }

    @Override
    public void listChanged(ListEvent<VisualSearchResult> listChanges) {
        EventList<VisualSearchResult> eventList = listChanges.getSourceList();
        while (listChanges.next()) {
            VisualSearchResult searchResult = eventList.get(listChanges.getIndex());
            handleEvent(searchResult);

        }
    }

    private void handleEvent(VisualSearchResult searchResult) {
        Group group = findAndAddGroup(searchResult);
        group.updateParent();
    }

    private Group findAndAddGroup(VisualSearchResult searchResult) {
        Group group = null;
        for (Group g : groups) {
            if (g.belongsInGroup(searchResult)) {
                group = g;
            }
        }
        if (group == null) {
            group = new Group(searchResult);
            groups.add(group);
        } else {
            group.add(searchResult);
        }
        return group;
    }

    public void update() {
        List<Group> done = new ArrayList<Group>();

        for (Group group : groups) {
            done.add(group);
            for (Group innerGroup : groups) {
                if (!done.contains(innerGroup)) {
                    for (VisualSearchResult searchResult : innerGroup.items) {
                        if (group.belongsInGroup(searchResult)
                                && !group.isInGroup(searchResult)) {
                            innerGroup.remove(searchResult);
                            group.add(searchResult);
                        }
                    }
                }
            }
            group.updateParent();
        }
    }
}