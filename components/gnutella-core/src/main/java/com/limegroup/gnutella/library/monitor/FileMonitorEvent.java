package com.limegroup.gnutella.library.monitor;

public class FileMonitorEvent {
	private final FileMonitorEventType type;
	private final String watchPath;
	private final String path;

	public FileMonitorEvent(FileMonitorEventType type, String watchPath,
			String path) {
		this.type = type;
		this.watchPath = watchPath;
		this.path = path;
	}

	public String getWatchPath() {
		return watchPath;
	}

	public String getPath() {
		return path;
	}

	public FileMonitorEventType getType() {
		return type;
	}

}
