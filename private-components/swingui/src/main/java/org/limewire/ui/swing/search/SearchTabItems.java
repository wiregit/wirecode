package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.components.FancyTabList;
import org.limewire.ui.swing.components.NoOpAction;
import org.limewire.ui.swing.components.TabActionMap;

/**
 * This class contains the numbers of different types of files
 * that matched given search criteria.
 * 
 * @see org.limewire.ui.swing.search.SearchResultsPanel.
 */
class SearchTabItems {

    private final List<TabActionMap> searchActionMaps;

    private final SearchTabListener listener;
    
    private final FancyTabList searchTab;

    SearchTabItems(SearchCategory category, SearchTabListener listener) {
        this.listener = listener;
        
        this.searchActionMaps = new ArrayList<TabActionMap>();
        searchActionMaps.add(newTabActionMap(new SearchTabAction("All", SearchCategory.ALL)));
        searchActionMaps.add(newTabActionMap(new SearchTabAction("Music", SearchCategory.AUDIO)));
        searchActionMaps.add(newTabActionMap(new SearchTabAction("Videos", SearchCategory.VIDEO)));
        searchActionMaps.add(newTabActionMap(new SearchTabAction("Images", SearchCategory.IMAGES)));
        searchActionMaps.add(newTabActionMap(new SearchTabAction("Documents", SearchCategory.DOCUMENTS)));

        for (TabActionMap map : searchActionMaps) {
            SearchTabAction action = (SearchTabAction)map.getMainAction();
            if (category == action.getCategory()) {
                action.putValue(Action.SELECTED_KEY, true);
                listener.categorySelected(category);
            } else if (category != SearchCategory.ALL) {
                action.setEnabled(false);
            }
        }

        FancyTabList ttp = new FancyTabList(searchActionMaps);
        ttp.setFlowedLayout();
        ttp.setHighlightPainter(new RectanglePainter<JXButton>(
            2, 2, 0, 2, 5, 5, true, Color.WHITE, 0f, Color.WHITE));

        this.searchTab = ttp;
    }
    
    public FancyTabList getSearchTab() {
        return searchTab;
    }
    
    public Collection<Map.Entry<SearchCategory, Action>> getResultCountActions() {
        Map<SearchCategory, Action> counts = new EnumMap<SearchCategory, Action>(SearchCategory.class);
        for(TabActionMap map : searchActionMaps) {
            SearchCategory category = ((SearchTabAction)map.getMainAction()).getCategory();
            counts.put(category, map.getMoreTextAction());
        }
        return counts.entrySet();
    }
    
    private TabActionMap newTabActionMap(SearchTabAction action) {
        Action moreText = new NoOpAction();
        moreText.putValue(Action.NAME, "#");
        return new TabActionMap(action, null, moreText, null);
    }

    private class SearchTabAction extends AbstractAction {
        private final SearchCategory category;

        public SearchTabAction(String name, SearchCategory category) {
            super(name);
            this.category = category;
        }

        SearchCategory getCategory() {
            return category;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            listener.categorySelected(category);
        }
    }

    static interface SearchTabListener {
        void categorySelected(SearchCategory searchCategory);
    }
}
