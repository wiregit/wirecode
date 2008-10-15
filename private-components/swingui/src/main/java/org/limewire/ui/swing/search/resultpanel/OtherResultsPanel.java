package org.limewire.ui.swing.search.resultpanel;


import org.limewire.core.api.download.ResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.FromActions;
import org.limewire.ui.swing.search.RowSelectionPreserver;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class OtherResultsPanel extends BaseResultPanel {

    @AssistedInject public OtherResultsPanel(
        @Assisted EventList<VisualSearchResult> eventList,
        OtherTableFormat tableFormat,
        ResultDownloader resultDownloader,
        @Assisted Search search,
        @Assisted SearchInfo searchInfo,
        @Assisted RowSelectionPreserver preserver,
        Navigator navigator,
        FromActions fromActions,
        ListViewTableEditorRendererFactory listViewEditorRendererFactory) {
        
        super(listViewEditorRendererFactory, eventList, tableFormat, resultDownloader,
            search, searchInfo, preserver, navigator, fromActions);
        
    }
}