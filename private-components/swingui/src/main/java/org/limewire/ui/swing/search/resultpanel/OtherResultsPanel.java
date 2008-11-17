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

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class OtherResultsPanel extends BaseResultPanel {

    @AssistedInject public OtherResultsPanel(
        @Assisted EventList<VisualSearchResult> eventList,
        OtherTableFormat tableFormat,
        DownloadListManager downloadListManager,
        @Assisted Search search,
        @Assisted SearchInfo searchInfo,
        @Assisted RowSelectionPreserver preserver,
        Navigator navigator,
        RemoteHostActions fromActions,
        ListViewTableEditorRendererFactory listViewEditorRendererFactory,
        PropertiesFactory<VisualSearchResult> properties,
        ListViewRowHeightRule rowHeightRule) {
        
        super(listViewEditorRendererFactory, eventList, tableFormat, downloadListManager,
            search, searchInfo, preserver, navigator, fromActions, properties, rowHeightRule);
        
    }
    
    @Override
    protected void setupCellRenderers(ResultsTableFormat<VisualSearchResult> tableFormat) {
        super.setupCellRenderers(tableFormat);
        setCellRenderer(OtherTableFormat.SIZE_INDEX, new FileSizeRenderer());
    }
}