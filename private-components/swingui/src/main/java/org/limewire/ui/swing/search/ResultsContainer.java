package org.limewire.ui.swing.search;

import java.awt.CardLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.search.resultpanel.AllResultsPanel;
import org.limewire.ui.swing.search.resultpanel.AudioResultsPanel;
import org.limewire.ui.swing.search.resultpanel.DocumentsResultsPanel;
import org.limewire.ui.swing.search.resultpanel.ImagesResultsPanel;
import org.limewire.ui.swing.search.resultpanel.VideoResultsPanel;

class ResultsContainer extends JXPanel {
    
    private final CardLayout cardLayout;
    
    ResultsContainer() {
        this.cardLayout = new CardLayout();
        setLayout(cardLayout);
        
        add(new AllResultsPanel(), SearchCategory.ALL.name());
        add(new AudioResultsPanel(), SearchCategory.AUDIO.name());
        add(new VideoResultsPanel(), SearchCategory.VIDEO.name());
        add(new ImagesResultsPanel(), SearchCategory.IMAGES.name());
        add(new DocumentsResultsPanel(), SearchCategory.DOCUMENTS.name());
    }
    
    void showCategory(SearchCategory category) {
        cardLayout.show(this, category.name());
    }

}
