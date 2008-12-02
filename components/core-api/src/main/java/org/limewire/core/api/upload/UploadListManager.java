package org.limewire.core.api.upload;

import java.util.List;

import ca.odell.glazedlists.EventList;


public interface UploadListManager {

    List<UploadItem> getUploadItems();

    EventList<UploadItem> getSwingThreadSafeUploads();

}
