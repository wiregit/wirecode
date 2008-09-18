package org.limewire.ui.swing.images;

import java.io.File;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.jdesktop.application.Resource;
import org.limewire.collection.FixedsizeForgetfulHashMap;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ThumbnailManagerImpl implements ThumbnailManager {
    
    private final Map<File,Icon> thumbnails = new FixedsizeForgetfulHashMap<File,Icon>(1000);
    
    @Resource
    private Icon loadIcon;
    @Resource
    private Icon errorIcon;
    
    @Inject
    public ThumbnailManagerImpl() {
        GuiUtils.assignResources(this); 
    }

    @Override
    public Icon getThumbnailForFile(File file) {
        if(file == null)
            return null;
        Icon icon = thumbnails.get(file);
        if(icon == null) {
            thumbnails.put(file, loadIcon);
            ImageExecutorService.submit(new ThumbnailCallable(thumbnails, file, errorIcon));
        }
        return icon;
    }
    
    @Override
    public Icon getThumbnailForFile(File file, JComponent callback) {
        if(file == null)
            return null;
        Icon icon = thumbnails.get(file);
        if(icon == null) {
            thumbnails.put(file, loadIcon);
            ImageExecutorService.submit(new ThumbnailCallable(thumbnails, file, errorIcon, callback));
        }
        return icon;
    }

    @Override
    public boolean isThumbnailForFileAvailable(File file) {
        return thumbnails.containsKey(file);
    }
}
