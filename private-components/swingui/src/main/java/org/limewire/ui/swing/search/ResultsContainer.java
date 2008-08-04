package org.limewire.ui.swing.search;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.swing.EventSelectionModel;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import java.awt.CardLayout;
import java.util.HashMap;
import java.util.Map;

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
import org.limewire.ui.swing.search.resultpanel.SearchScrollPane;
import org.limewire.ui.swing.search.resultpanel.VideoResultsPanelFactory;

/**
 * This class is a panel that displays search results.
 * @see org.limewire.ui.swing.search.SearchResultsPanel.
 */
public class ResultsContainer extends JXPanel implements ModeListener {

    private BaseResultPanel currentPanel;
    private final CardLayout cardLayout = new CardLayout();
    private final FilterMatcherEditor matcherEditor = new FilterMatcherEditor();
    private Map<String, BaseResultPanel> panelMap =
        new HashMap<String, BaseResultPanel>();

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
        
        setLayout(cardLayout);
        
        FilterList<VisualSearchResult> filterList =
            new FilterList<VisualSearchResult>(
                visualSearchResults, matcherEditor);
        
        EventSelectionModel<VisualSearchResult> eventSelectionModel =
            new EventSelectionModel<VisualSearchResult>(filterList);
        eventSelectionModel.setSelectionMode(
            ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
        
        panelMap.put(SearchCategory.ALL.name(),
            allFactory.create(filterList, eventSelectionModel, search));
        panelMap.put(SearchCategory.AUDIO.name(),
            audioFactory.create(filterList, eventSelectionModel, search));
        panelMap.put(SearchCategory.VIDEO.name(),
            videoFactory.create(filterList, eventSelectionModel, search));
        panelMap.put(SearchCategory.IMAGES.name(),
            imagesFactory.create(filterList, eventSelectionModel, search));
        panelMap.put(SearchCategory.DOCUMENTS.name(),
            documentsFactory.create(filterList, eventSelectionModel, search));
        
        for (String name : panelMap.keySet()) {
            BaseResultPanel panel = panelMap.get(name);
            add(new SearchScrollPane(panel), name);
        }
    }
    
    public BaseResultPanel getCurrentPanel() {
        return currentPanel;
    }
    
    /**
     * Changes whether the list view or table view is displayed.
     * @param mode LIST or TABLE
     */
    public void setMode(ModeListener.Mode mode) {
        currentPanel.setMode(mode);
    }
    
    void showCategory(SearchCategory category) {
        String name = category.name();
        cardLayout.show(this, name);
        currentPanel = panelMap.get(name);
        matcherEditor.categoryChanged(category);
    }

    private static class FilterMatcherEditor
    extends AbstractMatcherEditor<VisualSearchResult> {
        
        void categoryChanged(SearchCategory category) {
            if (category == SearchCategory.ALL) {
                fireMatchAll();
            } else {
                final ResultType type = typeForCategory(category);
                fireChanged(new Matcher<VisualSearchResult>() {
                    @Override
                    public boolean matches(VisualSearchResult item) {
                        return item.getCategory() == type;
                    }
                });
            }
        }

        private ResultType typeForCategory(SearchCategory category) {
            switch (category) {
            case AUDIO:
                return ResultType.AUDIO;
            case DOCUMENTS:
                return ResultType.DOCUMENT;
            case IMAGES:
                return ResultType.IMAGE;
            case VIDEO:
                return ResultType.VIDEO;
            default:
                throw new IllegalArgumentException(category.name());
            }
        }
    }
}
