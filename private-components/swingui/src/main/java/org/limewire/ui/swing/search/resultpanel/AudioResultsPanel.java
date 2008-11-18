package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.RemoteHostActions;
import org.limewire.ui.swing.search.RowSelectionPreserver;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.table.QualityRenderer;
import org.limewire.ui.swing.table.TimeRenderer;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class AudioResultsPanel extends BaseResultPanel {

    @AssistedInject
    public AudioResultsPanel(
        @Assisted EventList<VisualSearchResult> eventList,
        DownloadListManager downloadListManager,
        @Assisted Search search,
        @Assisted SearchInfo searchInfo,
        @Assisted RowSelectionPreserver preserver,
        Navigator navigator,
        RemoteHostActions remoteHostActions,
        ListViewTableEditorRendererFactory listViewEditorRendererFactory,
        PropertiesFactory<VisualSearchResult> properties,
        ListViewRowHeightRule rowHeightRule, 
        SaveLocationExceptionHandler saveLocationExceptionHandler) {
        
        super(listViewEditorRendererFactory, eventList, new MusicTableFormat(), downloadListManager,
            search, searchInfo, preserver, navigator, remoteHostActions, properties, rowHeightRule, saveLocationExceptionHandler);
        
    }
    
    @Override
    protected void setupCellRenderers(ResultsTableFormat<VisualSearchResult> tableFormat) {
        super.setupCellRenderers(tableFormat);
        setCellRenderer(MusicTableFormat.SIZE_INDEX, new FileSizeRenderer());
        setCellRenderer(MusicTableFormat.LENGTH_INDEX, new TimeRenderer());
        setCellRenderer(MusicTableFormat.QUALITY_INDEX, new QualityRenderer());
    }
}