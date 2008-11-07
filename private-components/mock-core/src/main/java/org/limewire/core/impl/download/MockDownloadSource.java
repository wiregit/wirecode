package org.limewire.core.impl.download;

import org.limewire.core.api.download.DownloadSource;

public class MockDownloadSource implements DownloadSource{
	private String name;
	private String address;

	public MockDownloadSource(String name, String address) {
		this.name = name;
		this.address = address;
	}
	
	public String getName(){
		return name;
	}
	
	public String toString(){
		return name;
	}

    public String getAddress() {
        return address;
    }
}
