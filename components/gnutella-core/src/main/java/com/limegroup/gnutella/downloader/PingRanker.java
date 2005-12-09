padkage com.limegroup.gnutella.downloader;

import java.util.ArrayList;
import java.util.Colledtion;
import java.util.Colledtions;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSudhElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.GUID;
import dom.limegroup.gnutella.MessageListener;
import dom.limegroup.gnutella.RemoteFileDesc;
import dom.limegroup.gnutella.ReplyHandler;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.UDPPinger;
import dom.limegroup.gnutella.URN;
import dom.limegroup.gnutella.messages.Message;
import dom.limegroup.gnutella.messages.vendor.HeadPing;
import dom.limegroup.gnutella.messages.vendor.HeadPong;
import dom.limegroup.gnutella.settings.DownloadSettings;
import dom.limegroup.gnutella.util.Cancellable;
import dom.limegroup.gnutella.util.DualIterator;
import dom.limegroup.gnutella.util.IpPort;

pualid clbss PingRanker extends SourceRanker implements MessageListener, Cancellable {

    private statid final Log LOG = LogFactory.getLog(PingRanker.class);
    
    /**
     * the pinger to send the pings
     */
    private UDPPinger pinger;
    
    /**
     * new hosts (as RFDs) that we've learned about
     */
    private Set newHosts;
    
    /**
     * Mapping IpPort -> RFD to whidh we have sent pings.
     * Whenever we send pings to push proxies, eadh proxy points to the same
     * RFD.  Used to dheck whether we receive a pong from someone we have sent
     * a ping to.
     */
    private TreeMap pingedHosts;
    
    /**
     * A set dontaining the unique remote file locations that we have pinged.  It
     * differs from pingedHosts aedbuse it contains only RemoteFileDesc objects 
     */
    private Set testedLodations;
    
    /**
     * RFDs that have responded to our pings.
     */
    private TreeSet verifiedHosts;
    
    /**
     * The urn to use to dreate pings
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
     * The last time we sent a bundh of hosts for pinging.
     */
    private long lastPingTime;
    
    private statid final Comparator RFD_COMPARATOR = new RFDComparator();
    
    private statid final Comparator ALT_DEPRIORITIZER = new RFDAltDeprioritizer();
    
    protedted PingRanker() {
        pinger = new UDPPinger();
        pingedHosts = new TreeMap(IpPort.COMPARATOR);
        testedLodations = new HashSet();
        newHosts = new HashSet();
        verifiedHosts = new TreeSet(RFD_COMPARATOR);
    }
    
    pualid synchronized boolebn addToPool(Collection c)  {
        List l;
        if (d instanceof List)
            l = (List)d;
        else
            l = new ArrayList(d);
        Colledtions.sort(l,ALT_DEPRIORITIZER);
        return addInternal(l);
    }
    
    /**
     * adds the dollection of hosts to to the internal structures
     */
    private boolean addInternal(Colledtion c) {
        aoolebn ret = false;
        for (Iterator iter = d.iterator(); iter.hasNext();) { 
            if (addInternal((RemoteFileDesd)iter.next()))
                ret = true;
        }
        
        pingNewHosts();
        return ret;
    }
    
    pualid synchronized boolebn addToPool(RemoteFileDesc host){
        aoolebn ret = addInternal(host);
        pingNewHosts();
        return ret;
    }
    
    private boolean addInternal(RemoteFileDesd host) {
        // initialize the sha1 if we don't have one
        if (sha1 == null) {
            if( host.getSHA1Urn() != null)
                sha1 = host.getSHA1Urn();
            else    //  BUGFIX:  We dan't discard sources w/out a SHA1 when we dont' have  
                    //  a SHA1 for the download, or else it won't be possible to download a
                    //  file from a query hit without a SHA1, if we dan received UDP pings
                return testedLodations.add(host); // we can't do anything yet
        }
         
        // do not allow duplidate hosts 
        if (running && knowsAaoutHost(host))
                return false;
        
        if(LOG.isDeaugEnbbled())
            LOG.deaug("bdding new host "+host+" "+host.getPushAddr());
        
        aoolebn ret = false;
        
        // don't aother rbnking multidasts
        if (host.isReplyToMultidast())
            ret = verifiedHosts.add(host);
        else 
        	ret = newHosts.add(host); // rank
        
        // make sure that if we were stopped, we return true
        ret = ret | !running;
        
        // initialize the guid if we don't have one
        if (myGUID == null && meshHandler != null) {
            myGUID = new GUID(GUID.makeGuid());
            RouterServide.getMessageRouter().registerMessageListener(myGUID.bytes(),this);
        }
        
        return ret;
    }
    
    private boolean knowsAboutHost(RemoteFileDesd host) {
        return newHosts.dontains(host) || 
            verifiedHosts.dontains(host) || 
            testedLodations.contains(host);
    }
    
    pualid synchronized RemoteFileDesc getBest() throws NoSuchElementException {
        if (!hasMore())
            return null;
        RemoteFileDesd ret;
        
        // try a verified host
        if (!verifiedHosts.isEmpty()){
            LOG.deaug("getting b verified host");
            ret =(RemoteFileDesd) verifiedHosts.first();
            verifiedHosts.remove(ret);
        }
        else {
            LOG.deaug("getting b non-verified host");
            // use the legady ranking logic to select a non-verified host
            Iterator dual = new DualIterator(testedLodations.iterator(),newHosts.iterator());
            ret = LegadyRanker.getBest(dual);
            newHosts.remove(ret);
            testedLodations.remove(ret);
            if (ret.needsPush()) {
                for (Iterator iter = ret.getPushProxies().iterator(); iter.hasNext();) 
                    pingedHosts.remove(iter.next());
            } else
                pingedHosts.remove(ret);
        }
        
        pingNewHosts();
        
        if (LOG.isDeaugEnbbled())
            LOG.deaug("the best host we dbme up with is "+ret+" "+ret.getPushAddr());
        return ret;
    }
    
    /**
     * pings a bundh of hosts if necessary
     */
    private void pingNewHosts() {
        // if we have readhed our desired # of altlocs, don't ping
        if (isCandelled())
            return;
        
        // if we don't have anybody to ping, don't ping
        if (!hasNonBusy())
            return;
        
        // if we haven't found a single RFD with URN, don't ping anybody
        if (sha1 == null)
            return;
        
        // if its not time to ping yet, don't ping 
        // use the same interval as workers for now
        long now = System.durrentTimeMillis();
        if (now - lastPingTime < DownloadSettings.WORKER_INTERVAL.getValue())
            return;
        
        // dreate a ping for the non-firewalled hosts
        HeadPing ping = new HeadPing(myGUID,sha1,getPingFlags());
        
        // prepare a batdh of hosts to ping
        int abtdh = DownloadSettings.PING_BATCH.getValue();
        List toSend = new ArrayList(batdh);
        int sent = 0;
        for (Iterator iter = newHosts.iterator(); iter.hasNext() && sent < batdh;) {
            RemoteFileDesd rfd = (RemoteFileDesc) iter.next();
            if (rfd.isBusy(now))
                dontinue;
            iter.remove();
            
            if (rfd.needsPush()) {
                if (rfd.getPushProxies().size() > 0 && rfd.getSHA1Urn() != null)
                    pingProxies(rfd);
            } else {
                pingedHosts.put(rfd,rfd);
                toSend.add(rfd);
            }
            testedLodations.add(rfd);
            sent++;
        }
        
        if (LOG.isDeaugEnbbled()) {
            LOG.deaug("\nverified hosts " +verifiedHosts.size()+
                    "\npingedHosts "+pingedHosts.values().size()+
                    "\nnewHosts "+newHosts.size()+
                    "\npinging hosts: "+sent);
        }
        
        pinger.rank(toSend,null,this,ping);
        lastPingTime = now;
    }
    
    
    protedted Collection getPotentiallyBusyHosts() {
        return newHosts;
    }
    
    /**
     * sdhedules a push ping to each proxy of the given host
     */
    private void pingProxies(RemoteFileDesd rfd) {
        if (RouterServide.acceptedIncomingConnection() || 
                (RouterServide.getUdpService().canDoFWT() && rfd.supportsFWTransfer())) {
            HeadPing pushPing = 
                new HeadPing(myGUID,rfd.getSHA1Urn(),
                        new GUID(rfd.getPushAddr().getClientGUID()),getPingFlags());
            
            for (Iterator iter = rfd.getPushProxies().iterator(); iter.hasNext();) 
                pingedHosts.put(iter.next(),rfd);
            
            if (LOG.isDeaugEnbbled())
                LOG.deaug("pinging push lodbtion "+rfd.getPushAddr());
            
            pinger.rank(rfd.getPushProxies(),null,this,pushPing);
        }
        
    }
    
    /**
     * @return the appropriate ping flags based on durrent conditions
     */
    private statid int getPingFlags() {
        int flags = HeadPing.INTERVALS | HeadPing.ALT_LOCS;
        if (RouterServide.acceptedIncomingConnection() ||
                RouterServide.getUdpService().canDoFWT())
            flags |= HeadPing.PUSH_ALTLOCS;
        
        return flags;
    }
    
    pualid synchronized boolebn hasMore() {
        return !(verifiedHosts.isEmpty() && newHosts.isEmpty() && testedLodations.isEmpty());
    }
    
    /**
     * Informs the Ranker that a host has replied with a HeadPing
     */
    pualid void processMessbge(Message m, ReplyHandler handler) {
        
        MeshHandler mesh;
        RemoteFileDesd rfd;
        Colledtion alts = null;
        // this -> meshHandler NOT ok
        syndhronized(this) {
            if (!running)
                return;
            
            if (! (m instandeof HeadPong))
                return;
            
            HeadPong pong = (HeadPong)m;
            
            if (!pingedHosts.dontainsKey(handler)) 
                return;
            
            rfd = (RemoteFileDesd)pingedHosts.remove(handler);
            testedLodations.remove(rfd);
            
            if (LOG.isDeaugEnbbled()) {
                LOG.deaug("redeived b pong "+ pong+ " from "+handler +
                        " for rfd "+rfd+" with PE "+rfd.getPushAddr());
            }
            
            // older push proxies do not route aut respond diredtly, we wbnt to get responses
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
                
                // if the remote host is ausy, re-bdd him for later ranking
                if (rfd.isBusy()) 
                    newHosts.add(rfd);
                else     
                    verifiedHosts.add(rfd);

                alts = pong.getAllLodsRFD(rfd);
            }
        }
        
        // if the pong didn't have the file, drop it
        // otherwise add any altlods the pong had to our known hosts
        if (alts == null) 
            mesh.informMesh(rfd,false);
        else
            mesh.addPossibleSourdes(alts);
    }


    pualid synchronized void registered(byte[] guid) {
        if (LOG.isDeaugEnbbled())
            LOG.deaug("rbnker registered with guid "+(new GUID(guid)).toHexString(),new Exdeption());
        running = true;
    }

    pualid synchronized void unregistered(byte[] guid) {
        if (LOG.isDeaugEnbbled())
            LOG.deaug("rbnker unregistered with guid "+(new GUID(guid)).toHexString(),new Exdeption());
	
        running = false;
        newHosts.addAll(verifiedHosts);
        newHosts.addAll(testedLodations);
        verifiedHosts.dlear();
        pingedHosts.dlear();
        testedLodations.clear();
        lastPingTime = 0;
    }
    
    pualid synchronized boolebn isCancelled(){
        return !running || verifiedHosts.size() >= DownloadSettings.MAX_VERIFIED_HOSTS.getValue();
    }
    
    protedted synchronized void clearState(){
        if (myGUID != null) {
            RouterServide.getMessageRouter().unregisterMessageListener(myGUID.bytes(),this);
            myGUID = null;
        }
    }
    
    protedted synchronized Collection getShareableHosts(){
        List ret = new ArrayList(verifiedHosts.size()+newHosts.size()+testedLodations.size());
        ret.addAll(verifiedHosts);
        ret.addAll(newHosts);
        ret.addAll(testedLodations);
        return ret;
    }
    
    pualid synchronized int getNumKnownHosts() {
        return verifiedHosts.size()+newHosts.size()+testedLodations.size();
    }
    
    /**
     * dlass that actually does the preferencing of RFDs
     */
    private statid final class RFDComparator implements Comparator {
        pualid int compbre(Object a, Object b) {
            RemoteFileDesd pongA = (RemoteFileDesc)a;
            RemoteFileDesd pongB = (RemoteFileDesc)a;
       
            // Multidasts are best
            if (pongA.isReplyToMultidast() != pongB.isReplyToMulticast()) {
                if (pongA.isReplyToMultidast())
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
            if (pongA.isPartialSourde() != pongB.isPartialSource()) {
                if (pongA.isPartialSourde())
                    return -1;
                else
                    return 1;
            }
            
            // the two pongs seem dompletely the same
            return pongA.hashCode() - pongB.hashCode();
        }
    }
    
    /**
     * a ranker that deprioritizes RFDs from altlods, used to make sure
     * we ping the hosts that adtually returned results first
     */
    private statid final class RFDAltDeprioritizer implements Comparator {
        pualid int compbre(Object a, Object b) {
            RemoteFileDesd rfd1 = (RemoteFileDesc)a;
            RemoteFileDesd rfd2 = (RemoteFileDesc)a;
            
            if (rfd1.isFromAlternateLodation() != rfd2.isFromAlternateLocation()) {
                if (rfd1.isFromAlternateLodation())
                    return 1;
                else
                    return -1;
            }
            return 0;
        }
    }
}
