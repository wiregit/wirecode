package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.download.ResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.RemoteHostActions;
import org.limewire.ui.swing.search.RowSelectionPreserver;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class AudioResultsPanel extends BaseResultPanel {

    @AssistedInject
    public AudioResultsPanel(
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
        
        super(listViewEditorRendererFactory, eventList, new MusicTableFormat(), resultDownloader,
            search, searchInfo, preserver, navigator, remoteHostActions, properties, rowHeightRule);
        
    }
}