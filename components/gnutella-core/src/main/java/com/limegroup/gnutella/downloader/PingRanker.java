package com.limegroup.gnutella.downloader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
import com.limegroup.gnutella.util.FixedSizeExpiringSet;
import com.limegroup.gnutella.util.ForgetfulHashMap;
import com.limegroup.gnutella.util.HasherSet;
import com.limegroup.gnutella.util.IpPort;

public class PingRanker extends SourceRanker implements MessageListener, Cancellable {

    private HeadPing ping;
    private UDPPinger pinger;
    
    /**
     * new hosts (as RFDs) that we've learned about
     */
    private Set newHosts;
    
    /**
     * IpPorts to whom we have sent a ping out to but have not received a response.
     * Each entry points to the RFD it was extracted from
     */
    private TreeMap pingedHosts;
    
    /**
     * Hosts that have responded to our pings, mapping HeadPong -> RFD
     */
    private TreeMap verifiedHosts;
    
    private static final Comparator PONG_COMPARATOR = new HeadPongComparator();
    
    public PingRanker() {
        pinger = new UDPPinger();
        pingedHosts = new TreeMap(IpPort.COMPARATOR);
        newHosts = new HashSet();
        verifiedHosts = new TreeMap(PONG_COMPARATOR);
    }
    
    public synchronized void addToPool(RemoteFileDesc host){
        newHosts.add(host);
        pingIfNeeded();
    }
    
    public synchronized RemoteFileDesc getBest() throws NoSuchElementException{
        RemoteFileDesc ret;
        
        if (!verifiedHosts.isEmpty()) 
            ret =(RemoteFileDesc) verifiedHosts.remove(verifiedHosts.firstKey());
        else if (!pingedHosts.isEmpty()){
            ret =(RemoteFileDesc) pingedHosts.remove(pingedHosts.firstKey());
        }else {
            ret = getNewRFD();
            newHosts.remove(ret);
        }
        
        pingIfNeeded();
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
        
        // create a ping for the non-firewalled hosts
        HeadPing ping = new HeadPing(getNewRFD().getSHA1Urn(),getPingFlags());
        
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
        HeadPing pushPing = 
            new HeadPing(rfd.getSHA1Urn(),new GUID(rfd.getPushAddr().getClientGUID()),getPingFlags());
        pinger.rank(rfd.getPushProxies(),this,this,pushPing);
        
        for (Iterator iter = rfd.getPushProxies().iterator(); iter.hasNext();) 
            pingedHosts.put(iter.next(),rfd);
        
    }
    
    private RemoteFileDesc getNewRFD() {
        Iterator iter = newHosts.iterator();
        return (RemoteFileDesc) iter.next();
    }
    
    public synchronized boolean hasMore() {
        return !(verifiedHosts.isEmpty() && newHosts.isEmpty() && pingedHosts.isEmpty());
    }
    
    public synchronized void processMessage(Message m, ReplyHandler handler) {
        if (! (m instanceof HeadPong))
            return;
        
        HeadPong pong = (HeadPong)m;
        
        if (!pingedHosts.containsKey(handler))
            return;
        
        RemoteFileDesc rfd = (RemoteFileDesc)pingedHosts.remove(handler);
        
        // if the pong is firewalled, remove the other proxies from the 
        // pinged set
        if (pong.isFirewalled()) {
            for (Iterator iter = rfd.getPushProxies().iterator(); iter.hasNext();) 
                pingedHosts.remove(iter.next());
        }
        
        // if the pong didn't have the file, drop it
        if (!pong.hasFile())
            return;
        
        // extract any altlocs the pong had
        newHosts.addAll(pong.getAllLocsRFD(rfd));
        
        // and sort the host.
        verifiedHosts.put(pong,rfd);
        
    }


    public void registered(byte[] guid) {}

    public void unregistered(byte[] guid) {}
    
    public synchronized boolean isCancelled(){
        return verifiedHosts.size() >= DownloadSettings.MAX_VERIFIED_HOSTS.getValue();
    }
    
    protected synchronized Collection getShareableHosts(){
        Set ret = new HashSet(verifiedHosts.size()+newHosts.size());
        ret.addAll(verifiedHosts.values());
        ret.addAll(newHosts);
        return ret;
    }
    
    /**
     * class that actually does the preferencing of headpongs
     * 
     * HeadPongs with highest number of free slots get the highest priority
     * Within the same queue rank, firewalled hosts get priority
     * Within the same queue/fwall, partial hosts get priority
     * If everything is the same and both pongs are partial, the one that is 
     * currently downloading gets priority
     */
    private static final class HeadPongComparator implements Comparator {
        public int compare(Object a, Object b) {
            HeadPong pongA = (HeadPong)a;
            HeadPong pongB = (HeadPong)b;
            
            if (pongA.getQueueStatus() > pongB.getQueueStatus())
                return 1;
            else if (pongA.getQueueStatus() < pongB.getQueueStatus())
                return -1;
            
            if (pongA.isFirewalled() != pongB.isFirewalled()) {
                if (pongA.isFirewalled())
                    return -1;
                else 
                    return 1;
            }
            
            if (pongA.hasCompleteFile() != pongB.hasCompleteFile()) {
                if (pongA.hasCompleteFile())
                    return 1;
                else
                    return -1;
            }
            
            
            if (pongA.isDownloading() != pongB.isDownloading()) {
                if (pongA.isDownloading())
                    return -1;
                else
                    return 1;
            }
                    
            // the two pongs seem completely the same, but since this is a treemap..
            return pongA.hashCode() - pongB.hashCode();
        }
    }

}
