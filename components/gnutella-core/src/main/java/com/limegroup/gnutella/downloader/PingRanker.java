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
import com.limegroup.gnutella.util.IpPort;

public class PingRanker extends SourceRanker implements MessageListener, Cancellable {

    private static final Log LOG = LogFactory.getLog(PingRanker.class);
    
    /**
     * a cached instance of the ping to send to non-firewalled hosts
     */
    private HeadPing ping;
    
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
     * RFD.
     */
    private TreeMap pingedHosts;
    
    /**
     * RFDs that have responded to our pings.
     */
    private TreeSet verifiedHosts;
    
    /**
     * IpPorts/PEs of people that we have ever known about - used to filter
     * incoming altlocs (and eventually to prepare bloom filters) 
     */
    private Set everybody,everybodyPush;
    
    /**
     * The urn to use to create pings
     */
    private URN sha1;
    
    /**
     * whether the ranker has been stopped.
     */
    private volatile boolean stopped;
    
    private static final Comparator RFD_COMPARATOR = new RFDComparator();
    
    private static final Comparator ALT_DEPRIORITIZER = new RFDAltDeprioritizer();
    
    public PingRanker() {
        pinger = new UDPPinger();
        pingedHosts = new TreeMap(IpPort.COMPARATOR);
        newHosts = new HashSet();
        verifiedHosts = new TreeSet(RFD_COMPARATOR);
        everybody = new TreeSet(IpPort.COMPARATOR);
        everybodyPush = new HashSet();
    }
    
    public synchronized void addToPool(Collection c)  {
        List l;
        if (c instanceof List)
            l = (List)c;
        else
            l = new ArrayList(c);
        
        Collections.sort(l,ALT_DEPRIORITIZER);
        super.addToPool(l);
    }
    
    public synchronized void addToPool(RemoteFileDesc host){
        if (sha1 == null && host.getSHA1Urn() != null)
            sha1 = host.getSHA1Urn();
        
        if (host.needsPush()) 
            everybodyPush.add(host.getPushAddr());
        else
            everybody.add(host);
        
        if(LOG.isDebugEnabled())
            LOG.debug("adding new host "+host);
        
        newHosts.add(host);
        pingIfNeeded();
    }
    
    private void addIfNew(RemoteFileDesc host) {
        if (LOG.isDebugEnabled())
            LOG.debug("potentially adding new host "+host);
        
        if (host.needsPush() && everybodyPush.contains(host.getPushAddr()))
                return;
        else if (everybody.contains(host))
                return;
        
        addToPool(host);
    }
    
    public synchronized RemoteFileDesc getBest() throws NoSuchElementException{
        LOG.debug("trying to get best host...");
        RemoteFileDesc ret;
        
        // try a verified host
        if (!verifiedHosts.isEmpty()){ 
            ret =(RemoteFileDesc) verifiedHosts.first();
            verifiedHosts.remove(ret);
        }
        else {
            // use the legacy ranking logic to select a non-verified host
            LegacyRanker r = new LegacyRanker();
            r.addToPool(pingedHosts.values());
            r.addToPool(newHosts);
            ret = r.getBest();
            newHosts.remove(ret);
            pingedHosts.values().remove(ret);
        }
        
        pingIfNeeded();
        
        if (LOG.isDebugEnabled())
            LOG.debug("the best host we came up with is "+ret+" "+ret.getPushAddr());
        return ret;
    }
    
    /**
     * pings a bunch of hosts if necessary
     */
    private void pingIfNeeded() {
        // if we have reached our desired # of altlocs, don't ping
        if (isCancelled())
            return;
        
        // if we don't have anybody to ping, don't ping
        if (newHosts.isEmpty())
            return;
        
        // if we haven't found a single RFD with URN, don't ping anybody
        if (sha1 == null)
            return;
        
        LOG.debug("will ping some hosts");
        
        // create a ping for the non-firewalled hosts
        HeadPing ping = new HeadPing(sha1,getPingFlags());
        
        // iterate through the list and select some hosts to ping
        List toSend = new ArrayList();
        for (Iterator iter = newHosts.iterator(); iter.hasNext();) {
            RemoteFileDesc rfd = (RemoteFileDesc) iter.next();
            iter.remove();
            
            if (rfd.needsPush())
                schedulePushPings(rfd);
            else
                toSend.add(rfd);
        }
        
        // put the rfds in the pingedMap.  For direct hosts, they map
        // to themselves 
        for (Iterator iter = toSend.iterator(); iter.hasNext();) {
            Object o = iter.next();
            pingedHosts.put(o,o);
        }
        
        if (LOG.isDebugEnabled())
            LOG.debug("pinging hosts: "+toSend);
        
        pinger.rank(toSend,this,this,ping);
    }
    
    /**
     * @return the appropriate ping flags based on current conditions
     */
    private int getPingFlags() {
        int flags = HeadPing.INTERVALS | HeadPing.ALT_LOCS;
        if (RouterService.acceptedIncomingConnection() ||
                RouterService.getUdpService().canDoFWT())
            flags |= HeadPing.PUSH_ALTLOCS;
        
        return flags;
    }
    
    /**
     * schedules a push ping to each proxy of the given host
     */
    private void schedulePushPings(RemoteFileDesc rfd) {
        if (RouterService.acceptedIncomingConnection() || 
                (RouterService.getUdpService().canDoFWT() && rfd.supportsFWTransfer())) {
            HeadPing pushPing = 
                new HeadPing(rfd.getSHA1Urn(),new GUID(rfd.getPushAddr().getClientGUID()),getPingFlags());
            
            for (Iterator iter = rfd.getPushProxies().iterator(); iter.hasNext();) 
                pingedHosts.put(iter.next(),rfd);
            
            if (LOG.isDebugEnabled())
                LOG.debug("pinging push locatioin "+rfd.getPushAddr());
            
            pinger.rank(rfd.getPushProxies(),this,this,pushPing);
        }
        
    }
    
    public synchronized boolean hasMore() {
        return !(verifiedHosts.isEmpty() && newHosts.isEmpty() && pingedHosts.isEmpty());
    }
    
    public synchronized void processMessage(Message m, ReplyHandler handler) {
        if (stopped)
            return;
        
        if (! (m instanceof HeadPong))
            return;
        
        HeadPong pong = (HeadPong)m;
        
        if (!pingedHosts.containsKey(handler))
            return;
        
        RemoteFileDesc rfd = (RemoteFileDesc)pingedHosts.remove(handler);
        
        if (LOG.isDebugEnabled())
            LOG.debug("received a pong "+ pong+ " from "+handler +" for rfd "+rfd+" with PE "+rfd.getPushAddr());
        
        // if the pong is firewalled, remove the other proxies from the 
        // pinged set
        if (pong.isFirewalled()) {
            for (Iterator iter = rfd.getPushProxies().iterator(); iter.hasNext();) 
                pingedHosts.remove(iter.next());
        }
        
        // if the pong didn't have the file, drop it
        if (!pong.hasFile())
            return;

        // update the rfd with information from the pong
        pong.updateRFD(rfd);
        
        // extract any altlocs the pong had and filter ones we know about
        for (Iterator iter = pong.getAllLocsRFD(rfd).iterator(); iter.hasNext();) {
            RemoteFileDesc current = (RemoteFileDesc) iter.next();
            addIfNew(current);
        }
        
        // and sort the host.
        verifiedHosts.add(rfd);
        
    }


    public void registered(byte[] guid) {}

    public void unregistered(byte[] guid) {}
    
    public synchronized boolean isCancelled(){
        return stopped || verifiedHosts.size() >= DownloadSettings.MAX_VERIFIED_HOSTS.getValue();
    }
    
    public void stop() {
        stopped = true;
    }
    
    protected synchronized Collection getShareableHosts(){
        Set ret = new HashSet(verifiedHosts.size()+newHosts.size()+pingedHosts.size());
        ret.addAll(verifiedHosts);
        ret.addAll(newHosts);
        ret.addAll(pingedHosts.values());
        return ret;
    }
    
    /**
     * class that actually does the preferencing of RFDs
     * 
     * HeadPongs with highest number of free slots get the highest priority
     * Within the same queue rank, firewalled hosts get priority
     * Within the same queue/fwall, partial hosts get priority
     */
    private static final class RFDComparator implements Comparator {
        public int compare(Object a, Object b) {
            RemoteFileDesc pongA = (RemoteFileDesc)a;
            RemoteFileDesc pongB = (RemoteFileDesc)b;
            
            if (pongA.getQueueStatus() > pongB.getQueueStatus())
                return 1;
            else if (pongA.getQueueStatus() < pongB.getQueueStatus())
                return -1;
            
            if (pongA.needsPush() != pongB.needsPush()) {
                if (pongA.needsPush())
                    return -1;
                else 
                    return 1;
            }
            
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
     * a quick-and-dirty ranker that deprioritizes RFDs from altlocs.
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
