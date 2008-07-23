package org.limewire.core.impl.download;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadSource;
import org.limewire.core.api.download.DownloadState;


public class MockDownloadItem implements DownloadItem {

	private String title;
	private volatile double currentSize = 0;
	private final double totalSize;
	//guarded by this
	private boolean running = true;
	private DownloadState state = DownloadState.DOWNLOADING;
	private List<DownloadSource> downloadSources;
	private Category category;
	
	private final PropertyChangeSupport support = new PropertyChangeSupport(this);

	//TODO: change constructor
	public MockDownloadItem(String title, double totalSize, DownloadState state, Category category) {
		this.title = title;
		this.category = category;
		this.totalSize = totalSize;
		this.state = state;
		downloadSources = new ArrayList<DownloadSource>();
		if (this.state == DownloadState.DOWNLOADING) {
			start();
		}
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
		//TODO - check for div by zero?
		return (int) (100 * getCurrentSize() / getTotalSize());
	}

	public double getCurrentSize() {
		return currentSize;
	}

	private synchronized void setCurrentSize(double newSize) {
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

	public double getTotalSize() {
		return totalSize;
	}

	public String getRemainingDownloadTime() {
		return "1:42 minutes";
	}

	public synchronized void cancel() {
		setRunning(false);
		setState(DownloadState.CANCELLED);
	}

	public synchronized void pause() {
		setRunning(false);
		setState(DownloadState.PAUSED);
	}

	public synchronized void resume() {
		setRunning(true);
		start();
		setState(DownloadState.DOWNLOADING);
	}

	private synchronized boolean isRunning() {
		return running;
	}

	private synchronized void setRunning(boolean running) {
		this.running = running;
	}

	private synchronized void start() {
		new Thread() {
			public void run() {
				while (isRunning() && getCurrentSize() < getTotalSize()) {
					setCurrentSize(getCurrentSize() + .5);
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
	public synchronized DownloadState getState() {
		return state;
	}

	//TODO: should be public?  or should state be handled internally?
	private synchronized void setState(DownloadState state) {
		DownloadState oldState = this.state;
		this.state = state;
		support.firePropertyChange("state", oldState, state);
		//setChanged();
		//notifyObservers();
	}
	
	public void addDownloadSource(DownloadSource source){
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
	public List<DownloadSource> getSources() {
		return downloadSources;
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
    public int getQueuePosition() {
        return 2;
    }

    @Override
    public String getRemainingStateTime() {
        // TODO Auto-generated method stub
        return "1:20";
    }

    @Override
    public ErrorState getErrorState() {
        // TODO Auto-generated method stub
        return ErrorState.NONE;
    }



}
