package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.download.ResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.RemoteHostActions;
import org.limewire.ui.swing.search.RowSelectionPreserver;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.table.TimeRenderer;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class VideoResultsPanel extends BaseResultPanel {

    @AssistedInject
    public VideoResultsPanel(
        @Assisted EventList<VisualSearchResult> eventList,
        ResultDownloader resultDownloader,
        @Assisted Search search,
        @Assisted SearchInfo searchInfo,
        @Assisted RowSelectionPreserver preserver,
        Navigator navigator,
        RemoteHostActions remoteHostActions,
        ListViewTableEditorRendererFactory listViewEditorRendererFactory,
        PropertiesFactory<VisualSearchResult> properties,
        ListViewRowHeightRule rowHeightRule) {
        
        super(listViewEditorRendererFactory, eventList, new VideoTableFormat(), resultDownloader,
            search, searchInfo, preserver, navigator, remoteHostActions, properties, rowHeightRule);
        
    }
    
    @Override
    protected void setupCellRenderers(ResultsTableFormat<VisualSearchResult> tableFormat) {
        super.setupCellRenderers(tableFormat);
        setCellRenderer(VideoTableFormat.SIZE_INDEX, new FileSizeRenderer());
        setCellRenderer(VideoTableFormat.LENGTH_INDEX, new TimeRenderer());
    }
}