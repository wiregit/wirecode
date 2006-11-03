package com.limegroup.bittorrent;

import java.io.File;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.bittorrent.Torrent.TorrentState;
import com.limegroup.bittorrent.handshaking.IncomingConnectionHandler;
import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ConnectionAcceptor;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.SpeedConstants;
import com.limegroup.gnutella.io.AbstractNBSocket;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.EventDispatcher;
import com.limegroup.gnutella.util.FileUtils;

/**
 * Class which manages active torrents and dispatching of 
 * incoming BT connections.
 *   
 * Active torrents are torrents either in downloading or 
 * seeding state.
 * 
 * There number of active torrents cannot exceed certain limit.
 * 
 * After a torrent finishes its download, it stays in seeding state
 * indefinitely.  If the user wishes to start a new torrent download 
 * and the limit for active torrents is reached, the seeding torrent
 * with the best upload:download ratio gets terminated.
 * 
 * If active torrent limit is reached and none of the torrents are seeding,
 * the new torrent is queued.
 */
public class TorrentManager 
implements ConnectionAcceptor, TorrentEventListener, 
EventDispatcher<TorrentEvent, TorrentEventListener> {
	

	private static final Log LOG = LogFactory.getLog(TorrentManager.class);

	/**
	 * The set of active torrents.
	 */
	private Set <ManagedTorrent>_active = new HashSet<ManagedTorrent>();
	
	/**
	 * The set of active torrents that are seeding.
	 * INVARIANT: subset of _active.
	 */
	private Set <ManagedTorrent>_seeding = new HashSet<ManagedTorrent>();
	
	/**
	 * Set of torrents that are about to get started.
	 */
	private Set <ManagedTorrent> _starting = new HashSet<ManagedTorrent>();
	
	private List <TorrentEventListener> listeners = 
		new CopyOnWriteArrayList<TorrentEventListener>();

    /** The File Manager */
    private FileManager fileManager;
    
    /**
	 * Initializes this. Always call this method before starting any torrents.
	 */
	public void initialize() {
		if (LOG.isDebugEnabled())
			LOG.debug("initializing TorrentManager");
		
		// register ourselves as an acceptor.
        StringBuilder word = new StringBuilder();
		word.append((char)19);
		word.append("BitTorrent");
		RouterService.getConnectionDispatcher().addConnectionAcceptor(
				this,
				new String[]{word.toString()},
				false,false);
		
        fileManager = RouterService.getFileManager();
        
		// we are a torrent event listener too.
		listeners.add(this);
	}
	
	/**
	 * @return number of allowed torrents for this speed.. this should
	 * probably be a setting
	 */
	private static int getMaxActiveTorrents() { 
		
		// windows 98 has very small connection limit, allow a single torrent only
		if (CommonUtils.isWindows() && !CommonUtils.isGoodWindows())
			return 1;
		
		int speed = ConnectionSettings.CONNECTION_SPEED.getValue();
		if (speed <= SpeedConstants.MODEM_SPEED_INT) return 1;
		else if (speed <= SpeedConstants.CABLE_SPEED_INT) return 3;
		else if (speed <= SpeedConstants.T1_SPEED_INT) return 3;
		else return 5;
	}
	
	/**
	 * @return the number of connections per torrent we'll try to maintain.
	 * This is somewhat arbitrary
	 */
	public static int getMaxTorrentConnections() {
		
		// windows 98 50 connection limit
		if (CommonUtils.isWindows() && !CommonUtils.isGoodWindows())
			return 30;
		
		if (ConnectionSettings.CONNECTION_SPEED.getValue() <= 
			SpeedConstants.MODEM_SPEED_INT)
			return 40;
		return 150;
	}
	
	public void addEventListener(TorrentEventListener listener) {
		listeners.add(listener);
	}
	
	public void removeEventListener(TorrentEventListener listener) {
		listeners.remove(listener);
	}
	
	public void dispatchEvent(TorrentEvent evt) {
		for(TorrentEventListener l : listeners) {
			if (l != evt.getSource())
				l.handleTorrentEvent(evt);
		}
	}
	
	public void handleTorrentEvent(TorrentEvent evt) {
		if (evt.getSource() == this)
			return;
		ManagedTorrent t = evt.getTorrent();
		switch(evt.getType()) {
		case STARTING: torrentStarting(t); break;
		case STARTED: torrentStarted(t); break;
        case DOWNLOADING: shareTorrent(t); break;
		case STOPPED: torrentStopped(t); break;
		case COMPLETE: torrentComplete(t); break;
        
        // the below aren't handled specially...
        case STOP_APPROVED:
        case STOP_REQUESTED:
		}
	}
	
	/**
	 * @return active torrent for the given infoHash, null if no such.
	 */
	public synchronized ManagedTorrent getTorrentForHash(byte[] infoHash) {
		for (ManagedTorrent torrent : _active) {
			if (Arrays.equals(torrent.getInfoHash(), infoHash))
				return torrent;
		}
		return null;
	}
	
	public void acceptConnection(String word, Socket sock) {
		IncomingConnectionHandler.instance().handleIncoming(
				(AbstractNBSocket)sock, this);
	}

	private synchronized void torrentComplete(ManagedTorrent t) {
		Assert.that(_active.contains(t));
		_seeding.add(t);
	}
	
	private synchronized void torrentStarting(ManagedTorrent t) {
		_starting.add(t);
	}

	private synchronized void torrentStarted(ManagedTorrent t) {
		Assert.that(_starting.contains(t));
		_starting.remove(t);
		// if the user is adding a seeding torrent.. 
		// effectively restart it
		if (_seeding.remove(t))
			_active.remove(t);
			
		// when force-start is implemented, this needs to become a while
        // and we need to have a way to potentially stop a started torrent
        // from really starting. (or disable some of the force-started ones),
        // or etc....
		if (_active.size() >= getMaxActiveTorrents()) {
			ManagedTorrent best = null;
			for (ManagedTorrent torrent : _seeding) {
				if (best == null || torrent.getRatio() > best.getRatio())
					best = torrent;
			}
			if (best != null) 
				best.stop();
		}
		
		_active.add(t);
	}

	private synchronized void torrentStopped(ManagedTorrent t) {
		_active.remove(t);
		_seeding.remove(t);
        
		FileDesc fd = unshareTorrent(t);
        
        //if the tracker failed: delete torrent
        if(fd != null && t.getState().equals(TorrentState.TRACKER_FAILURE)){
            fd.getFile().delete();
        }
	}
	
	public synchronized boolean allowNewTorrent() {
		return _starting.size() + 
		_active.size() - _seeding.size() < getMaxActiveTorrents();
	}
	
	public synchronized int getNumActiveTorrents() {
		return _active.size();
	}
	
	synchronized boolean hasNonSeeding() {
		return _active.size() > _seeding.size();
	}
	
	public boolean killTorrentForFile(File f) {
		ManagedTorrent found = null;
		synchronized(this) {
			for (ManagedTorrent t: _active) {
				TorrentFileSystem system = t.getMetaInfo().getFileSystem();
				if (system.conflicts(f) ||
						system.conflictsIncomplete(f)) {
					found = t;
					break;
				}
			}
		}
		
		if (found != null) {
			found.stop();
			return true;
		} else 
			return false;
	}
    
    /**
     * Shares the torrent by adding it to the FileManager
     */
    private synchronized void shareTorrent(ManagedTorrent t) {
        if(!SharingSettings.SHARE_TORRENT_META_FILES.getValue()) {
            return;
        }
        
        File f = getSharedTorrentMetaDataFile(t.getMetaInfo());
        if(f == null || !FileUtils.isFilePhysicallyShareable(f)) {
            return;
        }
        
        fileManager.addTorrentMetaDataFile(f);
    }
    
    /**
     * Unshares the torrent from the FileManager.
     * @return The FileDesc of the object removed from the FileManager. 
     * Can be null. 
     */
    private synchronized FileDesc unshareTorrent(ManagedTorrent t) {
        File f = getSharedTorrentMetaDataFile(t.getMetaInfo());
        if(f == null) {
            return null;
        }
        FileDesc fd = fileManager.stopSharingFile(f);
        return fd;
    }
    
    
    /**
     * Returns the expected shared .torrent meta data file. Can be null.
     */
    public File getSharedTorrentMetaDataFile(BTMetaInfo info) {
        String fileName = info.getFileSystem().getName().concat(".torrent");
        File f = new File(FileManager.TORRENT_META_DATA_SHARE, fileName);
        return f;
    }
}	
