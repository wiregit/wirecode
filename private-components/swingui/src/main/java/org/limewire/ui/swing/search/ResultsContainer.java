package org.limewire.ui.swing.search;

import java.awt.CardLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.search.ResultType;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.AllResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.AudioResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.DocumentsResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.ImagesResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.SearchScrollPane;
import org.limewire.ui.swing.search.resultpanel.VideoResultsPanelFactory;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.swing.EventListModel;
import ca.odell.glazedlists.swing.EventSelectionModel;

/**
 * This class is a panel that displays search results.
 * @see org.limewire.ui.swing.search.SearchResultsPanel.
 */
public class ResultsContainer extends JXPanel {

    private final CardLayout cardLayout;
    private final FilterMatcherEditor matcherEditor;

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
        
        this.cardLayout = new CardLayout();
        setLayout(cardLayout);
        
        this.matcherEditor = new FilterMatcherEditor();
        FilterList<VisualSearchResult> filterList = new FilterList<VisualSearchResult>(visualSearchResults, matcherEditor);
        
        EventListModel<VisualSearchResult> eventListModel =
            new EventListModel<VisualSearchResult>(filterList);
        EventSelectionModel<VisualSearchResult> eventSelectionModel =
            new EventSelectionModel<VisualSearchResult>(filterList);
        eventSelectionModel.setSelectionMode(
            ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
        
        add(new SearchScrollPane(
            allFactory.create(eventListModel, eventSelectionModel, search)),
            SearchCategory.ALL.name());
        
        add(new SearchScrollPane(
            audioFactory.create(eventListModel, eventSelectionModel, search)),
            SearchCategory.AUDIO.name());
        
        add(new SearchScrollPane(
            videoFactory.create(eventListModel, eventSelectionModel, search)),
            SearchCategory.VIDEO.name());
        
        add(new SearchScrollPane(
            imagesFactory.create(eventListModel, eventSelectionModel, search)),
            SearchCategory.IMAGES.name());
        
        add(new SearchScrollPane(
           documentsFactory.create(eventListModel, eventSelectionModel, search)),
           SearchCategory.DOCUMENTS.name());
    }
    
    void showCategory(SearchCategory category) {
        cardLayout.show(this, category.name());
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
