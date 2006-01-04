package com.limegroup.gnutella.torrent;
import java.util.Iterator;
import java.util.List;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.AzureusCoreFactory;

public class AzureusManager {

	private AzureusCore core;
    
	public AzureusManager() {
	}
	
	public void initialize() {
		try {
			core = AzureusCoreFactory.create();
			core.start();
	      TorrentDownloaderFactory.initManager(core.getGlobalManager(), true, true, COConfigurationManager.getStringParameter("Default save path") );
	      addExistingDownloads();
		}catch( AzureusCoreException e ){
	    	System.out.println( "Start fails:" );
	    	e.printStackTrace();
	    }
	}
	
	private void addExistingDownloads() {
		List dlmanagers = core.getGlobalManager().getDownloadManagers();
		for (Iterator iter = dlmanagers.iterator(); iter.hasNext();) {
			DownloadManager manager = (DownloadManager) iter.next();
			new AzTorrentDownloader(manager);
		}
	}
	
	public GlobalManager getGlobalManager() {
		return core.getGlobalManager();
	}
	
	public void shutdown() {
	    if ( core != null ){
	        try{
                core.stop();
	        }catch( AzureusCoreException e ){
	            
	            System.out.println( "Stop fails:" );
	            
	            e.printStackTrace();
	        }
	    }
	}
}
