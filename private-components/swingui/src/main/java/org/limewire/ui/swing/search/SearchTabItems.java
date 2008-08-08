package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.components.FancyTabList;
import org.limewire.ui.swing.components.NoOpAction;
import org.limewire.ui.swing.components.TabActionMap;

/**
 * This class is a panel that displays the numbers of different types of files
 * that matched given search criteria.
 * 
 * @see org.limewire.ui.swing.search.SearchResultsPanel.
 */
class SearchTabItems extends JXPanel {

    private final List<TabActionMap> searchTabs;

    private final SearchTabListener listener;

    SearchTabItems(SearchCategory category, SearchTabListener listener) {
        this.listener = listener;
        this.searchTabs = new ArrayList<TabActionMap>();
        searchTabs.add(newTabActionMap(new SearchTabAction("All types", SearchCategory.ALL)));
        searchTabs.add(newTabActionMap(new SearchTabAction("Audio", SearchCategory.AUDIO)));
        searchTabs.add(newTabActionMap(new SearchTabAction("Videos", SearchCategory.VIDEO)));
        searchTabs.add(newTabActionMap(new SearchTabAction("Images", SearchCategory.IMAGES)));
        searchTabs.add(newTabActionMap(new SearchTabAction("Documents", SearchCategory.DOCUMENTS)));

        for (TabActionMap map : searchTabs) {
            SearchTabAction action = (SearchTabAction)map.getMainAction();
            if (category == action.getCategory()) {
                action.putValue(Action.SELECTED_KEY, true);
                listener.categorySelected(category);
            } else if (category != SearchCategory.ALL) {
                action.setEnabled(false);
            }
        }

        setLayout(new GridBagLayout());
        setBackground(Color.LIGHT_GRAY);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.RELATIVE;

        FancyTabList ttp = new FancyTabList(searchTabs);
        ttp.setFlowedLayout(new Insets(0, 2, 0, 5));
        ttp.setHighlightPainter(new RectanglePainter<JXButton>(2, 2, 0, 2, 5, 5, true, Color.WHITE,
                0f, Color.WHITE));
        add(ttp, gbc);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        add(Box.createGlue(), gbc);
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
