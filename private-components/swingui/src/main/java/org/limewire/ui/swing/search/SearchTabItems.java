package org.limewire.ui.swing.search;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.components.FancyTab;
import org.limewire.ui.swing.components.FancyTabList;
import org.limewire.ui.swing.components.NoOpAction;
import org.limewire.ui.swing.components.TabActionMap;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.util.MediaType;

/**
 * This class contains the numbers of different types of files
 * that matched given search criteria.
 * 
 * @see org.limewire.ui.swing.search.SearchResultsPanel.
 */
class SearchTabItems extends FancyTabList
implements ListEventListener<VisualSearchResult> {

    private static final Map<String, String> schemaToTitleMap =
        new HashMap<String, String>();

    static {
        schemaToTitleMap.put("audio", "Music");
        schemaToTitleMap.put("image", "Images");
        schemaToTitleMap.put("document", "Documents");
        schemaToTitleMap.put("video", "Videos");
        schemaToTitleMap.put("application", "Other");
        schemaToTitleMap.put("custom", "Other");
        schemaToTitleMap.put("other", "Other");
    }

    private final List<TabActionMap> searchActionMaps;

    private final SearchTabListener listener;
    
    private EventList eventList;

    SearchTabItems(SearchCategory category, SearchTabListener listener) {
        this.listener = listener;
        
        this.searchActionMaps = new ArrayList<TabActionMap>();
        searchActionMaps.add(
            newTabActionMap(new SearchTabAction("All", SearchCategory.ALL)));
        searchActionMaps.add(
            newTabActionMap(new SearchTabAction("Music", SearchCategory.AUDIO)));
        searchActionMaps.add(
            newTabActionMap(new SearchTabAction("Videos", SearchCategory.VIDEO)));
        searchActionMaps.add(
            newTabActionMap(new SearchTabAction("Images", SearchCategory.IMAGES)));
        searchActionMaps.add(
            newTabActionMap(new SearchTabAction("Documents", SearchCategory.DOCUMENTS)));
        searchActionMaps.add(
            newTabActionMap(new SearchTabAction("Other", SearchCategory.OTHER)));

        for (TabActionMap map : searchActionMaps) {
            SearchTabAction action = (SearchTabAction) map.getMainAction();
            if (category == action.getCategory()) {
                action.putValue(Action.SELECTED_KEY, true);
                listener.categorySelected(category);
            } else if (category != SearchCategory.ALL) {
                action.setEnabled(false);
            }
        }

        setFlowedLayout();
        setHighlightPainter(new RectanglePainter<JXButton>(
            2, 2, 0, 2, 5, 5, true, Color.WHITE, 0f, Color.WHITE));
        setTabActionMaps(searchActionMaps);

        // Make all the tabs except "All" invisible
        // until we get a matching search result.
        setTabsVisible(false);
        getTab("All").setVisible(true);

        Font font = getFont().deriveFont(14.0f);
        font.deriveFont(Font.BOLD); // TODO: RMV This doesn't work!
        setTextFont(font);
    }
    
    public Collection<Map.Entry<SearchCategory, Action>> getResultCountActions() {
        Map<SearchCategory, Action> counts =
            new EnumMap<SearchCategory, Action>(SearchCategory.class);
        for (TabActionMap map : searchActionMaps) {
            SearchCategory category =
                ((SearchTabAction) map.getMainAction()).getCategory();
            counts.put(category, map.getMoreTextAction());
        }
        return counts.entrySet();
    }

    public void listChanged(ListEvent event) {
        // Get the most recent search result.
        EventList list = event.getSourceList();
        VisualSearchResult vsr = (VisualSearchResult) list.get(list.size() - 1);

        // Determine its media type.
        String extension = vsr.getFileExtension();
        MediaType mediaType = MediaType.getMediaTypeForExtension(extension);

        // Find the "tab" for the media type.
        String schema = mediaType == null ? "other" : mediaType.toString();
        String title = schemaToTitleMap.get(schema);
        FancyTab tab = getTab(title);

        // Make that tab visible if it isn't already.
        tab.setVisible(true);
    }
    
    private TabActionMap newTabActionMap(SearchTabAction action) {
        Action moreText = new NoOpAction();
        moreText.putValue(Action.NAME, "#");
        return new TabActionMap(action, null, moreText, null);
    }

    public void setEventList(EventList<VisualSearchResult> eventList) {
        if (this.eventList != null) {
            this.eventList.removeListEventListener(this);
        }

        this.eventList = eventList;
        eventList.addListEventListener(this);
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