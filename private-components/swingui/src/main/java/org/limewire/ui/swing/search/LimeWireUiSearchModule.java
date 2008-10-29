package org.limewire.ui.swing.search;

import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.model.SimilarResultsDetectorFactory;
import org.limewire.ui.swing.search.model.SimilarResultsDetectorFactoryImpl;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.AllResultsPanel;
import org.limewire.ui.swing.search.resultpanel.AllResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.AudioResultsPanel;
import org.limewire.ui.swing.search.resultpanel.AudioResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.DocumentsResultsPanel;
import org.limewire.ui.swing.search.resultpanel.DocumentsResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.ImagesResultsPanel;
import org.limewire.ui.swing.search.resultpanel.ImagesResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.ListViewRowHeightRule;
import org.limewire.ui.swing.search.resultpanel.ListViewRowHeightRuleImpl;
import org.limewire.ui.swing.search.resultpanel.ListViewTableEditorRenderer;
import org.limewire.ui.swing.search.resultpanel.ListViewTableEditorRendererFactory;
import org.limewire.ui.swing.search.resultpanel.OtherResultsPanel;
import org.limewire.ui.swing.search.resultpanel.OtherResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.ProgramResultsPanel;
import org.limewire.ui.swing.search.resultpanel.ProgramResultsPanelFactory;
import org.limewire.ui.swing.search.resultpanel.SearchHeadingDocumentBuilder;
import org.limewire.ui.swing.search.resultpanel.SearchHeadingDocumentBuilderImpl;
import org.limewire.ui.swing.search.resultpanel.SearchResultFromWidget;
import org.limewire.ui.swing.search.resultpanel.SearchResultFromWidgetFactory;
import org.limewire.ui.swing.search.resultpanel.SearchResultPropertiesFactory;
import org.limewire.ui.swing.search.resultpanel.VideoResultsPanel;
import org.limewire.ui.swing.search.resultpanel.VideoResultsPanelFactory;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireUiSearchModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(SearchHandler.class).to(SearchHandlerImpl.class);
        bind(RowSelectionPreserver.class).to(RowSelectionPreserverImpl.class);
        bind(SimilarResultsDetectorFactory.class).to(SimilarResultsDetectorFactoryImpl.class);
        
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
        
        bind(DocumentsResultsPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                DocumentsResultsPanelFactory.class, DocumentsResultsPanel.class));
        
        bind(ImagesResultsPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                ImagesResultsPanelFactory.class, ImagesResultsPanel.class));
        
        bind(OtherResultsPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                OtherResultsPanelFactory.class, OtherResultsPanel.class));
        
        bind(ProgramResultsPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                ProgramResultsPanelFactory.class, ProgramResultsPanel.class));
        
        bind(VideoResultsPanelFactory.class).toProvider(
            FactoryProvider.newFactory(
                VideoResultsPanelFactory.class, VideoResultsPanel.class));
        
        bind(ListViewTableEditorRendererFactory.class).toProvider(
                FactoryProvider.newFactory(
                        ListViewTableEditorRendererFactory.class, ListViewTableEditorRenderer.class));
        
        bind(RemoteHostActions.class).to(RemoteHostActionsImpl.class);

        bind(new TypeLiteral<PropertiesFactory<VisualSearchResult>>(){}).to(SearchResultPropertiesFactory.class);
        
        bind(SearchHeadingDocumentBuilder.class).to(SearchHeadingDocumentBuilderImpl.class);
        
        bind(SearchResultFromWidgetFactory.class).toProvider(
                FactoryProvider.newFactory(
                        SearchResultFromWidgetFactory.class, SearchResultFromWidget.class));
        
        bind(ListViewRowHeightRule.class).to(ListViewRowHeightRuleImpl.class);
    }
}