package org.limewire.core.api.download;

import java.util.Observable;



/**
 * A single download
 *
 */
//TODO: replace observer with listener and change to interface
public abstract class DownloadItem extends Observable {
	

	public static enum Category {
		VIDEO, AUDIO, DOCUMENT, IMAGE
	};

	public DownloadItem() {
	}
	
	public abstract DownloadState getState();

	public abstract String getTitle();

	public abstract int getPercent();

	public abstract double getCurrentSize();

	public abstract double getTotalSize();

	public abstract String getRemainingTime();

	public abstract void cancel();

	public abstract void pause();

	public abstract void resume();
		
	public abstract void addDownloadSource(DownloadSource source);

	public abstract int getDownloadSourceCount();

	public abstract DownloadSource getDownloadSource(int index);
	
	public abstract Category getCategory();

}
