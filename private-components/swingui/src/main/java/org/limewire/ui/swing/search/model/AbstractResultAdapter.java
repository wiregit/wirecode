package org.limewire.ui.swing.search.model;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayResult;
import org.limewire.ui.swing.util.PropertiableFileUtils;
import org.limewire.util.Objects;

/**
 * Base class for implementations of VisualSearchResult.
 */
abstract class AbstractResultAdapter implements VisualSearchResult, Comparable<VisualSearchResult> {

    private final VisualSearchResultStatusListener changeListener;
    
    private boolean preExistingDownload;    
    private RowDisplayResult rowDisplayResult;
    
    /**
     * Constructs an AbstractResultAdapter with the specified services.
     */
    AbstractResultAdapter(VisualSearchResultStatusListener changeListener) {
        this.changeListener = changeListener;
    }
    
    /**
     * Notifies change listener that the specified property name has changed
     * value.
     */
    protected void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        if (oldValue != newValue) {
            changeListener.resultChanged(this, propertyName, oldValue, newValue);
        }
    }

    /**
     * Notifies change listener that the specified property name has changed
     * value.
     */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (!Objects.equalOrNull(oldValue, newValue)) {
            changeListener.resultChanged(this, propertyName, oldValue, newValue);
        }
    }
    
    @Override
    public String getNameProperty(boolean useAudioArtist) {
        return PropertiableFileUtils.getNameProperty(this, useAudioArtist);
    }
    
    @Override
    public String getPropertyString(FilePropertyKey key) {
        Object value = getProperty(key);
        if (value != null) {
            return value.toString();
        } else {
            return null;
        }
    }
    
    @Override
    public boolean isPreExistingDownload() {
        return preExistingDownload;
    }

    @Override
    public void setPreExistingDownload(boolean preExistingDownload) {
        this.preExistingDownload = preExistingDownload;
    }
    
    @Override
    public RowDisplayResult getRowDisplayResult() {
        return rowDisplayResult;
    }
    
    @Override
    public void setRowDisplayResult(RowDisplayResult rowDisplayResult) {
        this.rowDisplayResult = rowDisplayResult;
    }
    
    @Override
    public int compareTo(VisualSearchResult vsr) {
        if (vsr == null) {
            return -1;
        }
        
        return getHeading().compareTo(vsr.getHeading());
    }
}
