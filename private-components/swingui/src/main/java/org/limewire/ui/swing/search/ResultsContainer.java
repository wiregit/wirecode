package org.limewire.ui.swing.search;

import java.awt.CardLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.search.ResultType;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.AllResultsPanel;
import org.limewire.ui.swing.search.resultpanel.AudioResultsPanel;
import org.limewire.ui.swing.search.resultpanel.DocumentsResultsPanel;
import org.limewire.ui.swing.search.resultpanel.ImagesResultsPanel;
import org.limewire.ui.swing.search.resultpanel.SearchScrollPane;
import org.limewire.ui.swing.search.resultpanel.VideoResultsPanel;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.matchers.Matcher;

class ResultsContainer extends JXPanel {
    
    private final CardLayout cardLayout;
    
    ResultsContainer(EventList<VisualSearchResult> visualSearchResults) {
        this.cardLayout = new CardLayout();
        setLayout(cardLayout);
        
        add(new SearchScrollPane(new AllResultsPanel(visualSearchResults)), SearchCategory.ALL.name());
        add(new SearchScrollPane(new AudioResultsPanel(filter(visualSearchResults, ResultType.AUDIO))), SearchCategory.AUDIO.name());
        add(new SearchScrollPane(new VideoResultsPanel(filter(visualSearchResults, ResultType.VIDEO))), SearchCategory.VIDEO.name());
        add(new SearchScrollPane(new ImagesResultsPanel(filter(visualSearchResults, ResultType.IMAGE))), SearchCategory.IMAGES.name());
        add(new SearchScrollPane(new DocumentsResultsPanel(filter(visualSearchResults, ResultType.DOCUMENT))), SearchCategory.DOCUMENTS.name());
    }
    
    void showCategory(SearchCategory category) {
        cardLayout.show(this, category.name());
    }
    
    private EventList<VisualSearchResult> filter(EventList<VisualSearchResult> eventList, final ResultType resultType) {
        return new FilterList<VisualSearchResult>(eventList, new Matcher<VisualSearchResult>() {
            @Override
            public boolean matches(VisualSearchResult item) {
                return item.getCategory() == resultType;
            }
        });
    }

}
