package org.limewire.ui.swing.search;

import java.awt.CardLayout;
import java.awt.Component;
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
import org.limewire.ui.swing.search.resultpanel.OtherResultsPanelFactory;

/**
 * This class is a panel that displays search results.
 * @see org.limewire.ui.swing.search.SearchResultsPanel.
 */
public class ResultsContainer extends JXPanel {

    private BaseResultPanel currentPanel;
    private Map<String, BaseResultPanel> panelMap =
        new HashMap<String, BaseResultPanel>();
    private ModeListener.Mode mode = ModeListener.Mode.LIST;
    
    private final CardLayout cardLayout = new CardLayout();

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
        DocumentsResultsPanelFactory documentsFactory,
        OtherResultsPanelFactory otherFactory) {
        
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
        panelMap.put(SearchCategory.OTHER.name(),
            otherFactory.create(filter(ResultType.OTHER, visualSearchResults), search));
        
        setLayout(cardLayout);
        
        for (Map.Entry<String, BaseResultPanel> entry : panelMap.entrySet()) {
            add(entry.getValue(), entry.getKey());
        }
    }

    public void synchronizeResultCount(SearchCategory key, final Action action) {
        // Adds itself as a listener to the list & keeps the action in sync.
        new SourceCountMaintainer(
            panelMap.get(key.name()).getResultsEventList(), action);
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
        String name = category.name();
        currentPanel = panelMap.get(name); 
        cardLayout.show(this, name);
        currentPanel.setMode(mode);
    }
    
    private EventList<VisualSearchResult> filter(
        final ResultType category, EventList<VisualSearchResult> list) {
        return new FilterList<VisualSearchResult>(
            list, new Matcher<VisualSearchResult>() {
            @Override
            public boolean matches(VisualSearchResult item) {
                return item.getCategory() == category;
            }        
        });
    }

    public Component getScrollPaneHeader() {
        return currentPanel.getScrollPaneHeader();
    }
}