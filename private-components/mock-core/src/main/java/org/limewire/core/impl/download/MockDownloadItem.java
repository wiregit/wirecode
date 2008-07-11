package org.limewire.core.impl.download;

import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadSource;
import org.limewire.core.api.download.DownloadState;


public class MockDownloadItem extends DownloadItem {

	private String title;
	private volatile double currentSize = 0;
	private final double totalSize;
	//guarded by this
	private boolean running = true;
	private DownloadState state = DownloadState.DOWNLOADING;
	private List<DownloadSource> downloadSources;
	private Category category;

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

	public String getTitle() {
		return title;
	}

	public int getPercent() {
		//TODO - check for div by zero?
		return (int) (100 * getCurrentSize() / getTotalSize());
	}

	public double getCurrentSize() {
		return currentSize;
	}

	private synchronized void setCurrentSize(double newSize) {
		this.currentSize = newSize > getTotalSize() ? getTotalSize() : newSize;
		if (currentSize == getTotalSize()) {
			setState(DownloadState.DONE);
		}
		setChanged();
		notifyObservers();
	}

	public double getTotalSize() {
		return totalSize;
	}

	public String getRemainingTime() {
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
		this.state = state;
		setChanged();
		notifyObservers();
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
	public DownloadSource getDownloadSource(int index){
		return downloadSources.get(index);
	}

	@Override
	public Category getCategory() {
		return category;
	}



}
