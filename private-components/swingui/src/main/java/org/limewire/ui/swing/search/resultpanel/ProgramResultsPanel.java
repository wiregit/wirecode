package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.download.ResultDownloader;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.actions.FromActions;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.RowSelectionPreserver;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class ProgramResultsPanel extends BaseResultPanel {

    @AssistedInject
    public ProgramResultsPanel(
        @Assisted EventList<VisualSearchResult> eventList,
        ProgramTableFormat tableFormat,
        ResultDownloader resultDownloader,
        @Assisted Search search,
        @Assisted SearchInfo searchInfo,
        @Assisted RowSelectionPreserver preserver,
        Navigator navigator,
        FromActions fromActions) {
        super(eventList, tableFormat, resultDownloader,
            search, searchInfo, preserver, navigator, fromActions);
    }
}