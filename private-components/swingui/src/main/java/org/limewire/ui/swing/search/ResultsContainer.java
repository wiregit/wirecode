package org.limewire.ui.swing.search;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.search.ResultType;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.AllResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.AudioResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.BaseResultPanel;
import org.limewire.ui.swing.search.resultpanel.DocumentsResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.ImagesResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.VideoResultsPanelFactory;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.matchers.Matcher;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * This class is a panel that displays search results.
 * @see org.limewire.ui.swing.search.SearchResultsPanel.
 */
public class ResultsContainer extends JXPanel implements ModeListener {

    private BaseResultPanel currentPanel;
    private Map<String, BaseResultPanel> panelMap =
        new HashMap<String, BaseResultPanel>();
    private ModeListener.Mode mode = ModeListener.Mode.LIST;

    /**
     * See LimeWireUISearchModule for binding information.
     */
    @AssistedInject ResultsContainer(
        @Assisted EventList<VisualSearchResult> visualSearchResults, 
        @Assisted Search search,
        AllResultsPanelFactory allFactory,
        AudioResultsPanelFactory audioFactory,
        VideoResultsPanelFactory videoFactory,
        ImagesResultsPanelFactory imagesFactory,
        DocumentsResultsPanelFactory documentsFactory) {
        
        setLayout(new BorderLayout());
        
        panelMap.put(SearchCategory.ALL.name(),
            allFactory.create(visualSearchResults, search));
        panelMap.put(SearchCategory.AUDIO.name(),
            audioFactory.create(filter(ResultType.AUDIO, visualSearchResults), search));
        panelMap.put(SearchCategory.VIDEO.name(),
            videoFactory.create(filter(ResultType.VIDEO, visualSearchResults), search));
        panelMap.put(SearchCategory.IMAGES.name(),
            imagesFactory.create(filter(ResultType.IMAGE, visualSearchResults), search));
        panelMap.put(SearchCategory.DOCUMENTS.name(),
            documentsFactory.create(filter(ResultType.DOCUMENT, visualSearchResults), search));
    }

    public void synchronizeResultCount(SearchCategory key, final Action action) {
        // Adds itself as a listener to the list & keeps the action in sync.
        new SourceCountMaintainer(panelMap.get(key.name()).getResultsEventList(), action);
    }
    
    /**
     * Changes whether the list view or table view is displayed.
     * @param mode LIST or TABLE
     */
    public void setMode(ModeListener.Mode mode) {
        this.mode = mode;
        currentPanel.setMode(mode);
    }
    
    void showCategory(SearchCategory category) {
        // TODO: This should be using a CardLayout and showing
        //       the right panel instead of removing and re-adding.
        //       Otherwise, state (such as where you have scrolled
        //       to) is likely lost when flipping between categories.
        
        //if (currentPanel != null) remove(currentPanel);
        removeAll();
        currentPanel = panelMap.get(category.name());
        currentPanel.setMode(mode);
        add(currentPanel, BorderLayout.CENTER);
    }
    
    private EventList<VisualSearchResult> filter(final ResultType category, EventList<VisualSearchResult> list) {
        return new FilterList<VisualSearchResult>(list, new Matcher<VisualSearchResult>() {
            @Override
            public boolean matches(VisualSearchResult item) {
                return item.getCategory() == category;
            }        
        });
    }
}
