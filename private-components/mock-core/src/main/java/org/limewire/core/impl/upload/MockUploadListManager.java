package org.limewire.core.impl.upload;

import java.util.List;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadListManager;
import org.limewire.core.api.upload.UploadState;

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
        
        uploadItems.add(new MockUploadItem(UploadState.DONE, "File.mp3", 30000, 150, Category.AUDIO));
        uploadItems.add(new MockUploadItem(UploadState.UPLOADING, "File.avi", 30000, 150, Category.VIDEO));
        uploadItems.add(new MockUploadItem(UploadState.UPLOADING, "File2mp3", 30000, 150, Category.AUDIO));
        uploadItems.add(new MockUploadItem(UploadState.DONE, "File3.exe", 30000, 150, Category.PROGRAM));
        uploadItems.add(new MockUploadItem(UploadState.UPLOADING, "File3.doc", 30000, 150, Category.DOCUMENT));

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
