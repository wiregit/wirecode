package org.limewire.ui.swing.downloads;


import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadPiecesInfo;
import org.limewire.core.api.download.DownloadPropertyKey;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.transfer.SourceInfo;
import org.limewire.io.Address;
import org.limewire.util.BaseTestCase;

/**
 * JUnit test case for DownloadStateComparator.
 */
public class DownloadStateComparatorTest extends BaseTestCase {
    
    public DownloadStateComparatorTest(String name){
        super(name);
    }
    
    /**
     * Tests that all DownloadStates are handled by the comparator and no IllegalArgumentException is thrown.
     */
    public void testAllDownloadStatesIncluded(){
        DownloadStateComparator comparator = new DownloadStateComparator();
        for (DownloadState state : DownloadState.values()){
            comparator.compare(new MockDownloadItem(state), new MockDownloadItem(state));
        }
    }
    
    /**
     * A mock DownloadItem that only cares about DownloadState
     */
    private static class MockDownloadItem implements DownloadItem {
        
        private DownloadState state;

        public MockDownloadItem(DownloadState state){
            this.state = state;
        }

        @Override
        public DownloadState getState() {
            return state;
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            // do nothing
        }

        @Override
        public void cancel() {
            // do nothing
        }

        @Override
        public long getAmountLost() {
            // do nothing
            return 0;
        }

        @Override
        public long getAmountVerified() {
            // do nothing
            return 0;
        }

        @Override
        public Category getCategory() {
            // do nothing
            return null;
        }

        @Override
        public Collection<File> getCompleteFiles() {
            // do nothing
            return null;
        }

        @Override
        public long getCurrentSize() {
            // do nothing
            return 0;
        }

        @Override
        public DownloadItemType getDownloadItemType() {
            // do nothing
            return null;
        }

        @Override
        public Object getDownloadProperty(DownloadPropertyKey key) {
            // do nothing
            return null;
        }

        @Override
        public int getDownloadSourceCount() {
            // do nothing
            return 0;
        }

        @Override
        public float getDownloadSpeed() {
            // do nothing
            return 0;
        }

        @Override
        public File getDownloadingFile() {
            // do nothing
            return null;
        }

        @Override
        public ErrorState getErrorState() {
            // do nothing
            return null;
        }

        @Override
        public File getLaunchableFile() {
            // do nothing
            return null;
        }

        @Override
        public int getLocalQueuePriority() {
            // do nothing
            return 0;
        }

        @Override
        public int getPercentComplete() {
            // do nothing
            return 0;
        }

        @Override
        public DownloadPiecesInfo getPiecesInfo() {
            // do nothing
            return null;
        }

        @Override
        public long getRemainingDownloadTime() {
            // do nothing
            return 0;
        }

        @Override
        public long getRemainingTimeInState() {
            // do nothing
            return 0;
        }

        @Override
        public Collection<RemoteHost> getRemoteHosts() {
            // do nothing
            return null;
        }

        @Override
        public int getRemoteQueuePosition() {
            // do nothing
            return 0;
        }

        @Override
        public File getSaveFile() {
            // do nothing
            return null;
        }

        @Override
        public List<Address> getSources() {
            // do nothing
            return null;
        }

        @Override
        public List<SourceInfo> getSourcesDetails() {
            // do nothing
            return null;
        }

        @Override
        public Date getStartDate() {
            // do nothing
            return null;
        }

        @Override
        public String getTitle() {
            // do nothing
            return null;
        }

        @Override
        public long getTotalSize() {
            // do nothing
            return 0;
        }

        @Override
        public boolean isLaunchable() {
            // do nothing
            return false;
        }

        @Override
        public boolean isRelocatable() {
            // do nothing
            return false;
        }

        @Override
        public boolean isStoreDownload() {
            // do nothing
            return false;
        }

        @Override
        public boolean isTryAgainEnabled() {
            // do nothing
            return false;
        }

        @Override
        public void pause() {
            // do nothing
            
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            // do nothing
            
        }

        @Override
        public void resume() {
            // do nothing
            
        }

        @Override
        public void setSaveFile(File saveFile, boolean overwrite) throws DownloadException {
            // do nothing
            
        }

        @Override
        public String getFileName() {
            // do nothing
            return null;
        }

        @Override
        public Object getProperty(FilePropertyKey key) {
            // do nothing
            return null;
        }

        @Override
        public String getPropertyString(FilePropertyKey filePropertyKey) {
            // do nothing
            return null;
        }

        @Override
        public URN getUrn() {
            // do nothing
            return null;
        }
                
        @Override
        public void markAsGood() {
        }
        
        @Override
        public boolean hasBeenMarkedAsGood() {
            return false;
        }
    }
}
