package org.limewire.core.impl.upload;

import java.util.List;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadListManager;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList.Connector;

public class MockUploadListManager implements UploadListManager {

    private EventList<UploadItem> uploadItems;

    public MockUploadListManager() {
        Connector<UploadItem> uploadConnector = GlazedLists.beanConnector(UploadItem.class);
        uploadItems = GlazedListsFactory.swingThreadProxyEventList(GlazedListsFactory
                .observableElementList(new BasicEventList<UploadItem>(), uploadConnector));

    }

    @Override
    public EventList<UploadItem> getSwingThreadSafeUploads() {
        return uploadItems;
    }

    @Override
    public List<UploadItem> getUploadItems() {
        return uploadItems;
    }

}
