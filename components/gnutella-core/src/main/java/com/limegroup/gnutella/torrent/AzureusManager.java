package com.limegroup.gnutella.torrent;
import java.util.Iterator;
import java.util.List;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.ui.common.StartServer;
import org.gudy.azureus2.ui.common.UIConst;
import org.gudy.azureus2.ui.common.Main.StartSocket;
import org.gudy.azureus2.ui.console.multiuser.UserManager;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.limegroup.gnutella.gui.FinalizeListener;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.util.CommonUtils;

public class AzureusManager implements FinalizeListener{

	private AzureusCore core;
	
	public AzureusManager() {
	}
	
	public void initialize() {
		GUIMediator.addFinalizeListener(this);
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
	            core = null;
                //TODO temporary hack
                SESecurityManager.exitVM(1);
	        }catch( AzureusCoreException e ){
	            
	            System.out.println( "Stop fails:" );
	            
	            e.printStackTrace();
	        }
	    }
	}
	
	public void doFinalize() {
	    shutdown();
	}	
}
