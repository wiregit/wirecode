package org.limewire.core.impl.upload;

import com.limegroup.gnutella.Uploader;

public interface UploadListener {
    
    public void uploadAdded(Uploader uploader);

    public void uploadRemoved(Uploader uploader);
}
