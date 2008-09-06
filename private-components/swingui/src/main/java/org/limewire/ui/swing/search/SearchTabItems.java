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

    private boolean isAll;

    static {
        schemaToTitleMap.put("audio", "Music");
        schemaToTitleMap.put("image", "Images");
        schemaToTitleMap.put("document", "Documents");
        schemaToTitleMap.put("video", "Videos");
        schemaToTitleMap.put("application", "Programs");
        schemaToTitleMap.put("custom", "Other");
        schemaToTitleMap.put("other", "Other");
    }

    private final List<TabActionMap> searchActionMaps;

    private final SearchTabListener listener;
    
    private EventList<VisualSearchResult> eventList;

    SearchTabItems(SearchCategory category, SearchTabListener listener) {
        isAll = category == SearchCategory.ALL;

        this.listener = listener;
        
        this.searchActionMaps = new ArrayList<TabActionMap>();
        if (isAll) {
            searchActionMaps.add(
                newTabActionMap(new SearchTabAction("All", SearchCategory.ALL)));
            searchActionMaps.add(
                newTabActionMap(new SearchTabAction("Music", SearchCategory.AUDIO)));
            searchActionMaps.add(
                newTabActionMap(new SearchTabAction("Videos", SearchCategory.VIDEO)));
            searchActionMaps.add(
                newTabActionMap(new SearchTabAction("Images", SearchCategory.IMAGE)));
            searchActionMaps.add(
                newTabActionMap(new SearchTabAction("Documents", SearchCategory.DOCUMENT)));
            searchActionMaps.add(
                newTabActionMap(new SearchTabAction("Programs", SearchCategory.PROGRAM)));
            searchActionMaps.add(
                newTabActionMap(new SearchTabAction("Other", SearchCategory.OTHER)));
        } else if (category == SearchCategory.AUDIO) {
            searchActionMaps.add(newTabActionMap(
                new SearchTabAction("Music results ", SearchCategory.AUDIO)));
        } else if (category == SearchCategory.VIDEO) {
            searchActionMaps.add(newTabActionMap(
                new SearchTabAction("Video results ", SearchCategory.VIDEO)));
        } else if (category == SearchCategory.IMAGE) {
            searchActionMaps.add(newTabActionMap(
                new SearchTabAction("Image results ", SearchCategory.IMAGE)));
        } else if (category == SearchCategory.DOCUMENT) {
            searchActionMaps.add(newTabActionMap(
                new SearchTabAction("Document results ", SearchCategory.DOCUMENT)));
        } else if (category == SearchCategory.PROGRAM) {
            searchActionMaps.add(newTabActionMap(
                new SearchTabAction("Program results ", SearchCategory.PROGRAM)));
        } else if (category == SearchCategory.OTHER) {
            searchActionMaps.add(newTabActionMap(
                new SearchTabAction("Other results ", SearchCategory.OTHER)));
        }

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
        if (isAll) getTab("All").setVisible(true);

        Font font = getFont().deriveFont(12.0f);
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
        if (!isAll) {
            if (title.endsWith("s")) {
                title = title.substring(0, title.length() - 1);
            }
            title += " results ";
        }
        FancyTab tab = getTab(title);

        if (tab == null) {
            // This should never happen!
            System.err.println(
                "SearchTabItems.listChanged: no tab found with title \""
                + title + '"');
        } else {
            // Make that tab visible if it isn't already.
            tab.setVisible(true);
        }
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