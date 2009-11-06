package org.limewire.ui.swing.search;

import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.search.model.SearchResultsModel;

public interface BrowseFailedMessagePanelFactory {
    public BrowseFailedMessagePanel create(HeaderBarDecorator headerBarDecorator, SearchResultsModel searchResultsModel);
}
