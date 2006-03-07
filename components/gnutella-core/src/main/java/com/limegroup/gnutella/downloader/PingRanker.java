
// Edited for the Learning branch

package com.limegroup.gnutella.downloader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.MessageListener;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPPinger;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.HeadPing;
import com.limegroup.gnutella.messages.vendor.HeadPong;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.util.Cancellable;
import com.limegroup.gnutella.util.DualIterator;
import com.limegroup.gnutella.util.IpPort;

/**
 * 
 * Used with ManagedDownloader and UDPPinger.
 */
public class PingRanker extends SourceRanker implements MessageListener, Cancellable {

    private static final Log LOG = LogFactory.getLog(PingRanker.class);

    /**
     * the pinger to send the pings
     */
    private UDPPinger pinger;
    
    /**
     * new hosts (as RFDs) that we've learned about
     */
    private Set newHosts;
    
    /**
     * Mapping IpPort -> RFD to which we have sent pings.
     * Whenever we send pings to push proxies, each proxy points to the same
     * RFD.  Used to check whether we receive a pong from someone we have sent
     * a ping to.
     */
    private TreeMap pingedHosts;
    
    /**
     * A set containing the unique remote file locations that we have pinged.  It
     * differs from pingedHosts because it contains only RemoteFileDesc objects 
     */
    private Set testedLocations;
    
    /**
     * RFDs that have responded to our pings.
     */
    private TreeSet verifiedHosts;
    
    /**
     * The urn to use to create pings
     */
    private URN sha1;
    
    /**
     * The guid to use for my headPings
     */
    private GUID myGUID;
    
    /**
     * whether the ranker has been stopped.
     */
    private boolean running;
    
    /**
     * The last time we sent a bunch of hosts for pinging.
     */
    private long lastPingTime;
    
    private static final Comparator RFD_COMPARATOR = new RFDComparator();
    
    private static final Comparator ALT_DEPRIORITIZER = new RFDAltDeprioritizer();
    
    protected PingRanker() {
        pinger = new UDPPinger();
        pingedHosts = new TreeMap(IpPort.COMPARATOR);
        testedLocations = new HashSet();
        newHosts = new HashSet();
        verifiedHosts = new TreeSet(RFD_COMPARATOR);
    }
    
    public synchronized boolean addToPool(Collection c)  {
        List l;
        if (c instanceof List)
            l = (List)c;
        else
            l = new ArrayList(c);
        Collections.sort(l,ALT_DEPRIORITIZER);
        return addInternal(l);
    }
    
    /**
     * adds the collection of hosts to to the internal structures
     */
    private boolean addInternal(Collection c) {
        boolean ret = false;
        for (Iterator iter = c.iterator(); iter.hasNext();) { 
            if (addInternal((RemoteFileDesc)iter.next()))
                ret = true;
        }
        
        pingNewHosts();
        return ret;
    }
    
    public synchronized boolean addToPool(RemoteFileDesc host){
        boolean ret = addInternal(host);
        pingNewHosts();
        return ret;
    }
    
    private boolean addInternal(RemoteFileDesc host) {
        // initialize the sha1 if we don't have one
        if (sha1 == null) {
            if( host.getSHA1Urn() != null)
                sha1 = host.getSHA1Urn();
            else    //  BUGFIX:  We can't discard sources w/out a SHA1 when we dont' have  
                    //  a SHA1 for the download, or else it won't be possible to download a
                    //  file from a query hit without a SHA1, if we can received UDP pings
                return testedLocations.add(host); // we can't do anything yet
        }
         
        // do not allow duplicate hosts 
        if (running && knowsAboutHost(host))
                return false;
        
        if(LOG.isDebugEnabled())
            LOG.debug("adding new host "+host+" "+host.getPushAddr());
        
        boolean ret = false;
        
        // don't bother ranking multicasts
        if (host.isReplyToMulticast())
            ret = verifiedHosts.add(host);
        else 
        	ret = newHosts.add(host); // rank
        
        // make sure that if we were stopped, we return true
        ret = ret | !running;
        
        // initialize the guid if we don't have one
        if (myGUID == null && meshHandler != null) {
            myGUID = new GUID(GUID.makeGuid());
            RouterService.getMessageRouter().registerMessageListener(myGUID.bytes(),this);
        }
        
        return ret;
    }
    
    private boolean knowsAboutHost(RemoteFileDesc host) {
        return newHosts.contains(host) || 
            verifiedHosts.contains(host) || 
            testedLocations.contains(host);
    }
    
    public synchronized RemoteFileDesc getBest() throws NoSuchElementException {
        if (!hasMore())
            return null;
        RemoteFileDesc ret;
        
        // try a verified host
        if (!verifiedHosts.isEmpty()){
            LOG.debug("getting a verified host");
            ret =(RemoteFileDesc) verifiedHosts.first();
            verifiedHosts.remove(ret);
        }
        else {
            LOG.debug("getting a non-verified host");
            // use the legacy ranking logic to select a non-verified host
            Iterator dual = new DualIterator(testedLocations.iterator(),newHosts.iterator());
            ret = LegacyRanker.getBest(dual);
            newHosts.remove(ret);
            testedLocations.remove(ret);
            if (ret.needsPush()) {
                for (Iterator iter = ret.getPushProxies().iterator(); iter.hasNext();) 
                    pingedHosts.remove(iter.next());
            } else
                pingedHosts.remove(ret);
        }
        
        pingNewHosts();
        
        if (LOG.isDebugEnabled())
            LOG.debug("the best host we came up with is "+ret+" "+ret.getPushAddr());
        return ret;
    }
    
    /**
     * pings a bunch of hosts if necessary
     */
    private void pingNewHosts() {
        // if we have reached our desired # of altlocs, don't ping
        if (isCancelled())
            return;
        
        // if we don't have anybody to ping, don't ping
        if (!hasNonBusy())
            return;
        
        // if we haven't found a single RFD with URN, don't ping anybody
        if (sha1 == null)
            return;
        
        // if its not time to ping yet, don't ping 
        // use the same interval as workers for now
        long now = System.currentTimeMillis();
        if (now - lastPingTime < DownloadSettings.WORKER_INTERVAL.getValue())
            return;
        
        // create a ping for the non-firewalled hosts
        HeadPing ping = new HeadPing(myGUID,sha1,getPingFlags());
        
        // prepare a batch of hosts to ping
        int batch = DownloadSettings.PING_BATCH.getValue();
        List toSend = new ArrayList(batch);
        int sent = 0;
        for (Iterator iter = newHosts.iterator(); iter.hasNext() && sent < batch;) {
            RemoteFileDesc rfd = (RemoteFileDesc) iter.next();
            if (rfd.isBusy(now))
                continue;
            iter.remove();
            
            if (rfd.needsPush()) {
                if (rfd.getPushProxies().size() > 0 && rfd.getSHA1Urn() != null)
                    pingProxies(rfd);
            } else {
                pingedHosts.put(rfd,rfd);
                toSend.add(rfd);
            }
            testedLocations.add(rfd);
            sent++;
        }
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("\nverified hosts " +verifiedHosts.size()+
                    "\npingedHosts "+pingedHosts.values().size()+
                    "\nnewHosts "+newHosts.size()+
                    "\npinging hosts: "+sent);
        }
        
        pinger.rank(toSend,null,this,ping);
        lastPingTime = now;
    }
    
    
    protected Collection getPotentiallyBusyHosts() {
        return newHosts;
    }
    
    /**
     * schedules a push ping to each proxy of the given host
     */
    private void pingProxies(RemoteFileDesc rfd) {
        if (RouterService.acceptedIncomingConnection() || 
                (RouterService.getUdpService().canDoFWT() && rfd.supportsFWTransfer())) {
            HeadPing pushPing = 
                new HeadPing(myGUID,rfd.getSHA1Urn(),
                        new GUID(rfd.getPushAddr().getClientGUID()),getPingFlags());
            
            for (Iterator iter = rfd.getPushProxies().iterator(); iter.hasNext();) 
                pingedHosts.put(iter.next(),rfd);
            
            if (LOG.isDebugEnabled())
                LOG.debug("pinging push location "+rfd.getPushAddr());
            
            pinger.rank(rfd.getPushProxies(),null,this,pushPing);
        }
        
    }
    
    /**
     * @return the appropriate ping flags based on current conditions
     */
    private static int getPingFlags() {
        int flags = HeadPing.INTERVALS | HeadPing.ALT_LOCS;
        if (RouterService.acceptedIncomingConnection() ||
                RouterService.getUdpService().canDoFWT())
            flags |= HeadPing.PUSH_ALTLOCS;
        
        return flags;
    }
    
    public synchronized boolean hasMore() {
        return !(verifiedHosts.isEmpty() && newHosts.isEmpty() && testedLocations.isEmpty());
    }
    
    /**
     * Informs the Ranker that a host has replied with a HeadPing
     */
    public void processMessage(Message m, ReplyHandler handler) {
        
        MeshHandler mesh;
        RemoteFileDesc rfd;
        Collection alts = null;
        // this -> meshHandler NOT ok
        synchronized(this) {
            if (!running)
                return;
            
            if (! (m instanceof HeadPong))
                return;
            
            HeadPong pong = (HeadPong)m;
            
            if (!pingedHosts.containsKey(handler)) 
                return;
            
            rfd = (RemoteFileDesc)pingedHosts.remove(handler);
            testedLocations.remove(rfd);
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("received a pong "+ pong+ " from "+handler +
                        " for rfd "+rfd+" with PE "+rfd.getPushAddr());
            }
            
            // older push proxies do not route but respond directly, we want to get responses
            // from other push proxies
            if (!pong.hasFile() && !pong.isGGEPPong() && rfd.needsPush())
                return;
            
            // if the pong is firewalled, remove the other proxies from the 
            // pinged set
            if (pong.isFirewalled()) {
                for (Iterator iter = rfd.getPushProxies().iterator(); iter.hasNext();) 
                    pingedHosts.remove(iter.next());
            }
            
            mesh = meshHandler;
            if (pong.hasFile()) {
                //update the rfd with information from the pong
                pong.updateRFD(rfd);
                
                // if the remote host is busy, re-add him for later ranking
                if (rfd.isBusy()) 
                    newHosts.add(rfd);
                else     
                    verifiedHosts.add(rfd);

                alts = pong.getAllLocsRFD(rfd);
            }
        }
        
        // if the pong didn't have the file, drop it
        // otherwise add any altlocs the pong had to our known hosts
        if (alts == null) 
            mesh.informMesh(rfd,false);
        else
            mesh.addPossibleSources(alts);
    }


    public synchronized void registered(byte[] guid) {
        if (LOG.isDebugEnabled())
            LOG.debug("ranker registered with guid "+(new GUID(guid)).toHexString(),new Exception());
        running = true;
    }

    public synchronized void unregistered(byte[] guid) {
        if (LOG.isDebugEnabled())
            LOG.debug("ranker unregistered with guid "+(new GUID(guid)).toHexString(),new Exception());
	
        running = false;
        newHosts.addAll(verifiedHosts);
        newHosts.addAll(testedLocations);
        verifiedHosts.clear();
        pingedHosts.clear();
        testedLocations.clear();
        lastPingTime = 0;
    }
    
    public synchronized boolean isCancelled(){
        return !running || verifiedHosts.size() >= DownloadSettings.MAX_VERIFIED_HOSTS.getValue();
    }
    
    protected synchronized void clearState(){
        if (myGUID != null) {
            RouterService.getMessageRouter().unregisterMessageListener(myGUID.bytes(),this);
            myGUID = null;
        }
    }
    
    protected synchronized Collection getShareableHosts(){
        List ret = new ArrayList(verifiedHosts.size()+newHosts.size()+testedLocations.size());
        ret.addAll(verifiedHosts);
        ret.addAll(newHosts);
        ret.addAll(testedLocations);
        return ret;
    }
    
    public synchronized int getNumKnownHosts() {
        return verifiedHosts.size()+newHosts.size()+testedLocations.size();
    }
    
    /**
     * class that actually does the preferencing of RFDs
     */
    private static final class RFDComparator implements Comparator {
        public int compare(Object a, Object b) {
            RemoteFileDesc pongA = (RemoteFileDesc)a;
            RemoteFileDesc pongB = (RemoteFileDesc)b;
       
            // Multicasts are best
            if (pongA.isReplyToMulticast() != pongB.isReplyToMulticast()) {
                if (pongA.isReplyToMulticast())
                    return -1;
                else
                    return 1;
            }
            
            // HeadPongs with highest number of free slots get the highest priority
            if (pongA.getQueueStatus() > pongB.getQueueStatus())
                return 1;
            else if (pongA.getQueueStatus() < pongB.getQueueStatus())
                return -1;
       
            // Within the same queue rank, firewalled hosts get priority
            if (pongA.needsPush() != pongB.needsPush()) {
                if (pongA.needsPush())
                    return -1;
                else 
                    return 1;
            }
            
            // Within the same queue/fwall, partial hosts get priority
            if (pongA.isPartialSource() != pongB.isPartialSource()) {
                if (pongA.isPartialSource())
                    return -1;
                else
                    return 1;
            }
            
            // the two pongs seem completely the same
            return pongA.hashCode() - pongB.hashCode();
        }
    }
    
    /**
     * a ranker that deprioritizes RFDs from altlocs, used to make sure
     * we ping the hosts that actually returned results first
     */
    private static final class RFDAltDeprioritizer implements Comparator {
        public int compare(Object a, Object b) {
            RemoteFileDesc rfd1 = (RemoteFileDesc)a;
            RemoteFileDesc rfd2 = (RemoteFileDesc)b;
            
            if (rfd1.isFromAlternateLocation() != rfd2.isFromAlternateLocation()) {
                if (rfd1.isFromAlternateLocation())
                    return 1;
                else
                    return -1;
            }
            return 0;
        }
    }
}
