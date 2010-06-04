package org.limewire.core.impl.download;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.limewire.common.URN;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadPiecesInfo;
import org.limewire.core.api.download.DownloadPropertyKey;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.malware.AntivirusUpdateType;
import org.limewire.core.api.transfer.SourceInfo;
import org.limewire.io.Address;
import org.limewire.listener.SwingSafePropertyChangeSupport;

/**
 * Implementation of DownloadItem for mock core.
 */
public class MockDownloadItem implements DownloadItem {

	private volatile String title;
	private volatile long currentSize = 0;
    private volatile long remainingQueueTime = 9999;
	private final long totalSize;
	//guarded by this
	private volatile boolean running = true;
	private volatile DownloadState state = DownloadState.DOWNLOADING;
	private final List<Address> downloadSources;
	private final Category category;
    private final DownloadItemType downloadItemType;
	
	private ErrorState errorState = ErrorState.NONE;
	
	private final PropertyChangeSupport support = new SwingSafePropertyChangeSupport(this);
    private int queuePostion = 2;
    private URN urn;
    
	public MockDownloadItem(String title, long totalSize, DownloadState state, Category category) {
	    this(DownloadItemType.GNUTELLA, title, totalSize, state, category);
	}

    public MockDownloadItem(DownloadItemType type, String title, long totalSize,
            DownloadState state, Category category) {
        this.downloadItemType = type;
        this.title = title;
        this.category = category;
        this.totalSize = totalSize;
        this.state = state;
        downloadSources = new ArrayList<Address>();
        if (this.state == DownloadState.DOWNLOADING) {
            start();
        }
    }
	
	@Override
	public boolean isTryAgainEnabled() {
	    return false;
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener){
		support.addPropertyChangeListener(listener);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener listener){
		support.removePropertyChangeListener(listener);
	}

	public String getTitle() {
		return title;
	}

	public int getPercentComplete() {
	    long totalSize = getTotalSize();
	    return totalSize > 0 ? (int) (100 * getCurrentSize() / totalSize) : 0;
	}

	public long getCurrentSize() {
		return currentSize;
	}

	public void setCurrentSize(long newSize) {
		double oldSize = this.currentSize;
		this.currentSize = newSize > getTotalSize() ? getTotalSize() : newSize;
		if (currentSize == getTotalSize()) {
			setState(DownloadState.DONE);
		} else {
			support.firePropertyChange("currentSize", oldSize, currentSize);
		}
//		setChanged();
//		notifyObservers();
	}

	public long getTotalSize() {
		return totalSize;
	}

	public long getRemainingDownloadTime() {
        return (long) ((getTotalSize() - getCurrentSize()) / getDownloadSpeed());
    }

	public void cancel() {
		setRunning(false);
		setState(DownloadState.CANCELLED);
	}

	public void pause() {
		setRunning(false);
		setState(DownloadState.PAUSED);
	}

	public void resume() {
		setRunning(true);
		start();
		setState(DownloadState.DOWNLOADING);
	}

	private boolean isRunning() {
		return running;
	}

	private void setRunning(boolean running) {
		this.running = running;
	}

	private void start() {
		new Thread() {
            @Override
			public void run() {
				while (isRunning() && getCurrentSize() < getTotalSize()) {
					setCurrentSize(getCurrentSize() + 5);
					setRemainingQueueTime(getRemainingTimeInState() - 1);
					try {
						sleep(500);
					} catch (InterruptedException e) {
						// eat InterruptedException
					}
				}
			}
          
		}.start();
	}

	@Override
	public DownloadState getState() {
		return state;
	}

	public void setState(DownloadState state) {
		DownloadState oldState = this.state;
		this.state = state;
		support.firePropertyChange("state", oldState, state);
		//setChanged();
		//notifyObservers();
	}
	
	public void addDownloadSource(Address source){
		downloadSources.add(source);
	}

	@Override
	public int getDownloadSourceCount() {
		return downloadSources.size();
	}
	
	
	/**
	 * @throws ArrayIndexOutOfBoundsException
	 */
	@Override
	public List<Address> getSources() {
		return downloadSources;
	}
	
    @Override
    public List<SourceInfo> getSourcesDetails() {
        return Collections.emptyList();
    }


	@Override
	public Category getCategory() {
		return category;
	}

    @Override
    public float getDownloadSpeed() {
        return 56;
    }

    @Override
    public int getRemoteQueuePosition() {
        return queuePostion;
    }
    
    public void setQueuePosition(int position) {
        queuePostion = position;
    }


    @Override
    public ErrorState getErrorState() {
        return errorState;
    }
    
    public void setErrorState(ErrorState errorState){
        this.errorState = errorState;
    }

    @Override
    public long getRemainingTimeInState() {
        return remainingQueueTime;
    }

    private void setRemainingQueueTime(long l) {
        remainingQueueTime = l;
    }
 
    @Override
    public int getLocalQueuePriority() {
        return 0;
    }

    @Override
    public boolean isLaunchable() {
        return true;
    }

    @Override
    public File getDownloadingFile() {
        return new File("A FILE!");
    }
    
    @Override
    public File getLaunchableFile() {
        return getDownloadingFile();
    }

    @Override
    public URN getUrn() {
        return urn;
    }
    
    void setUrn(URN urn) {
        this.urn = urn;
    }
    
    @Override
    public String getFileName() {
        return title;
    }
    
    @Override
    public void setSaveFile(File saveFile, boolean overwrite) throws DownloadException {
        // Do nothing
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        return null;
    }

    @Override
    public String getPropertyString(FilePropertyKey filePropertyKey) {
        return title;
    }

    @Override
    public Collection<RemoteHost> getRemoteHosts() {
        return Collections.emptyList();
    }

    @Override
    public Date getStartDate() {
        return new Date();
    }

    @Override
    public boolean isStoreDownload() {
        return false;
    }

    @Override
    public File getSaveFile() {
        return null;
    }

    @Override
    public boolean isRelocatable() {
        return true;
    }

    @Override
    public Collection<File> getCompleteFiles() {
        return Collections.singletonList(getDownloadingFile());
    }

    @Override
    public DownloadItemType getDownloadItemType() {
        return downloadItemType;
    }

    @Override
    public Object getDownloadProperty(DownloadPropertyKey key) {
        switch (key) {
        case ANTIVIRUS_UPDATE_TYPE: 
            return AntivirusUpdateType.FULL;
        default:
            return null;
        }
    }

    @Override
    public DownloadPiecesInfo getPiecesInfo() {
        return null;
    }

    @Override
    public long getAmountLost() {
        return 0;
    }

    @Override
    public long getAmountVerified() {
        return 0;
    }

}
