/**
 * 
 */
package org.limewire.core.api.download;

import java.io.File;


public interface DownLoadAction {
    void download(File saveFile, boolean overwrite) throws SaveLocationException;
}