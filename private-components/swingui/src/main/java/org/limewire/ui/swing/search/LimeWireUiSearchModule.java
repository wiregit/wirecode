package org.limewire.ui.swing.search;

import org.limewire.ui.swing.search.resultpanel.AllResultsPanel;
import org.limewire.ui.swing.search.resultpanel.AllResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.AudioResultsPanel;
import org.limewire.ui.swing.search.resultpanel.AudioResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.DocumentsResultsPanel;
import org.limewire.ui.swing.search.resultpanel.DocumentsResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.ImagesResultsPanel;
import org.limewire.ui.swing.search.resultpanel.ImagesResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.VideoResultsPanel;
import org.limewire.ui.swing.search.resultpanel.VideoResultsPanelFactory;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;


public class LimeWireUiSearchModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(SearchHandler.class).to(SearchHandlerImpl.class);
        
        bind(SearchResultsPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                SearchResultsPanelFactory.class, SearchResultsPanel.class));
        
        bind(ResultsContainerFactory.class).toProvider(
            FactoryProvider.newFactory(
                ResultsContainerFactory.class, ResultsContainer.class));
        
        bind(AllResultsPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                AllResultsPanelFactory.class, AllResultsPanel.class));
        
        bind(AudioResultsPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                AudioResultsPanelFactory.class, AudioResultsPanel.class));
        
        bind(ImagesResultsPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                ImagesResultsPanelFactory.class, ImagesResultsPanel.class));
        
        bind(DocumentsResultsPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                DocumentsResultsPanelFactory.class, DocumentsResultsPanel.class));
        
        bind(VideoResultsPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                VideoResultsPanelFactory.class, VideoResultsPanel.class));
    }

}
