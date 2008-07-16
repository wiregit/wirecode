package org.limewire.core.impl.download;

import java.beans.PropertyChangeListener;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadSource;
import org.limewire.core.api.download.DownloadState;


public class CoreDownloadItem implements DownloadItem {

    @Override
    public void addDownloadSource(DownloadSource source) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void cancel() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Category getCategory() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getCurrentSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public DownloadSource getDownloadSource(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getDownloadSourceCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getPercent() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getRemainingTime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DownloadState getState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getTitle() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getTotalSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void pause() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void resume() {
        // TODO Auto-generated method stub
        
    }

}
