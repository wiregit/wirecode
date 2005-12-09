pbckage com.limegroup.gnutella.downloader;

import jbva.util.ArrayList;
import jbva.util.Collection;
import jbva.util.Collections;
import jbva.util.Comparator;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.List;
import jbva.util.NoSuchElementException;
import jbva.util.Set;
import jbva.util.TreeMap;
import jbva.util.TreeSet;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.MessageListener;
import com.limegroup.gnutellb.RemoteFileDesc;
import com.limegroup.gnutellb.ReplyHandler;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.UDPPinger;
import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.messages.vendor.HeadPing;
import com.limegroup.gnutellb.messages.vendor.HeadPong;
import com.limegroup.gnutellb.settings.DownloadSettings;
import com.limegroup.gnutellb.util.Cancellable;
import com.limegroup.gnutellb.util.DualIterator;
import com.limegroup.gnutellb.util.IpPort;

public clbss PingRanker extends SourceRanker implements MessageListener, Cancellable {

    privbte static final Log LOG = LogFactory.getLog(PingRanker.class);
    
    /**
     * the pinger to send the pings
     */
    privbte UDPPinger pinger;
    
    /**
     * new hosts (bs RFDs) that we've learned about
     */
    privbte Set newHosts;
    
    /**
     * Mbpping IpPort -> RFD to which we have sent pings.
     * Whenever we send pings to push proxies, ebch proxy points to the same
     * RFD.  Used to check whether we receive b pong from someone we have sent
     * b ping to.
     */
    privbte TreeMap pingedHosts;
    
    /**
     * A set contbining the unique remote file locations that we have pinged.  It
     * differs from pingedHosts becbuse it contains only RemoteFileDesc objects 
     */
    privbte Set testedLocations;
    
    /**
     * RFDs thbt have responded to our pings.
     */
    privbte TreeSet verifiedHosts;
    
    /**
     * The urn to use to crebte pings
     */
    privbte URN sha1;
    
    /**
     * The guid to use for my hebdPings
     */
    privbte GUID myGUID;
    
    /**
     * whether the rbnker has been stopped.
     */
    privbte boolean running;
    
    /**
     * The lbst time we sent a bunch of hosts for pinging.
     */
    privbte long lastPingTime;
    
    privbte static final Comparator RFD_COMPARATOR = new RFDComparator();
    
    privbte static final Comparator ALT_DEPRIORITIZER = new RFDAltDeprioritizer();
    
    protected PingRbnker() {
        pinger = new UDPPinger();
        pingedHosts = new TreeMbp(IpPort.COMPARATOR);
        testedLocbtions = new HashSet();
        newHosts = new HbshSet();
        verifiedHosts = new TreeSet(RFD_COMPARATOR);
    }
    
    public synchronized boolebn addToPool(Collection c)  {
        List l;
        if (c instbnceof List)
            l = (List)c;
        else
            l = new ArrbyList(c);
        Collections.sort(l,ALT_DEPRIORITIZER);
        return bddInternal(l);
    }
    
    /**
     * bdds the collection of hosts to to the internal structures
     */
    privbte boolean addInternal(Collection c) {
        boolebn ret = false;
        for (Iterbtor iter = c.iterator(); iter.hasNext();) { 
            if (bddInternal((RemoteFileDesc)iter.next()))
                ret = true;
        }
        
        pingNewHosts();
        return ret;
    }
    
    public synchronized boolebn addToPool(RemoteFileDesc host){
        boolebn ret = addInternal(host);
        pingNewHosts();
        return ret;
    }
    
    privbte boolean addInternal(RemoteFileDesc host) {
        // initiblize the sha1 if we don't have one
        if (shb1 == null) {
            if( host.getSHA1Urn() != null)
                shb1 = host.getSHA1Urn();
            else    //  BUGFIX:  We cbn't discard sources w/out a SHA1 when we dont' have  
                    //  b SHA1 for the download, or else it won't be possible to download a
                    //  file from b query hit without a SHA1, if we can received UDP pings
                return testedLocbtions.add(host); // we can't do anything yet
        }
         
        // do not bllow duplicate hosts 
        if (running && knowsAboutHost(host))
                return fblse;
        
        if(LOG.isDebugEnbbled())
            LOG.debug("bdding new host "+host+" "+host.getPushAddr());
        
        boolebn ret = false;
        
        // don't bother rbnking multicasts
        if (host.isReplyToMulticbst())
            ret = verifiedHosts.bdd(host);
        else 
        	ret = newHosts.bdd(host); // rank
        
        // mbke sure that if we were stopped, we return true
        ret = ret | !running;
        
        // initiblize the guid if we don't have one
        if (myGUID == null && meshHbndler != null) {
            myGUID = new GUID(GUID.mbkeGuid());
            RouterService.getMessbgeRouter().registerMessageListener(myGUID.bytes(),this);
        }
        
        return ret;
    }
    
    privbte boolean knowsAboutHost(RemoteFileDesc host) {
        return newHosts.contbins(host) || 
            verifiedHosts.contbins(host) || 
            testedLocbtions.contains(host);
    }
    
    public synchronized RemoteFileDesc getBest() throws NoSuchElementException {
        if (!hbsMore())
            return null;
        RemoteFileDesc ret;
        
        // try b verified host
        if (!verifiedHosts.isEmpty()){
            LOG.debug("getting b verified host");
            ret =(RemoteFileDesc) verifiedHosts.first();
            verifiedHosts.remove(ret);
        }
        else {
            LOG.debug("getting b non-verified host");
            // use the legbcy ranking logic to select a non-verified host
            Iterbtor dual = new DualIterator(testedLocations.iterator(),newHosts.iterator());
            ret = LegbcyRanker.getBest(dual);
            newHosts.remove(ret);
            testedLocbtions.remove(ret);
            if (ret.needsPush()) {
                for (Iterbtor iter = ret.getPushProxies().iterator(); iter.hasNext();) 
                    pingedHosts.remove(iter.next());
            } else
                pingedHosts.remove(ret);
        }
        
        pingNewHosts();
        
        if (LOG.isDebugEnbbled())
            LOG.debug("the best host we cbme up with is "+ret+" "+ret.getPushAddr());
        return ret;
    }
    
    /**
     * pings b bunch of hosts if necessary
     */
    privbte void pingNewHosts() {
        // if we hbve reached our desired # of altlocs, don't ping
        if (isCbncelled())
            return;
        
        // if we don't hbve anybody to ping, don't ping
        if (!hbsNonBusy())
            return;
        
        // if we hbven't found a single RFD with URN, don't ping anybody
        if (shb1 == null)
            return;
        
        // if its not time to ping yet, don't ping 
        // use the sbme interval as workers for now
        long now = System.currentTimeMillis();
        if (now - lbstPingTime < DownloadSettings.WORKER_INTERVAL.getValue())
            return;
        
        // crebte a ping for the non-firewalled hosts
        HebdPing ping = new HeadPing(myGUID,sha1,getPingFlags());
        
        // prepbre a batch of hosts to ping
        int bbtch = DownloadSettings.PING_BATCH.getValue();
        List toSend = new ArrbyList(batch);
        int sent = 0;
        for (Iterbtor iter = newHosts.iterator(); iter.hasNext() && sent < batch;) {
            RemoteFileDesc rfd = (RemoteFileDesc) iter.next();
            if (rfd.isBusy(now))
                continue;
            iter.remove();
            
            if (rfd.needsPush()) {
                if (rfd.getPushProxies().size() > 0 && rfd.getSHA1Urn() != null)
                    pingProxies(rfd);
            } else {
                pingedHosts.put(rfd,rfd);
                toSend.bdd(rfd);
            }
            testedLocbtions.add(rfd);
            sent++;
        }
        
        if (LOG.isDebugEnbbled()) {
            LOG.debug("\nverified hosts " +verifiedHosts.size()+
                    "\npingedHosts "+pingedHosts.vblues().size()+
                    "\nnewHosts "+newHosts.size()+
                    "\npinging hosts: "+sent);
        }
        
        pinger.rbnk(toSend,null,this,ping);
        lbstPingTime = now;
    }
    
    
    protected Collection getPotentibllyBusyHosts() {
        return newHosts;
    }
    
    /**
     * schedules b push ping to each proxy of the given host
     */
    privbte void pingProxies(RemoteFileDesc rfd) {
        if (RouterService.bcceptedIncomingConnection() || 
                (RouterService.getUdpService().cbnDoFWT() && rfd.supportsFWTransfer())) {
            HebdPing pushPing = 
                new HebdPing(myGUID,rfd.getSHA1Urn(),
                        new GUID(rfd.getPushAddr().getClientGUID()),getPingFlbgs());
            
            for (Iterbtor iter = rfd.getPushProxies().iterator(); iter.hasNext();) 
                pingedHosts.put(iter.next(),rfd);
            
            if (LOG.isDebugEnbbled())
                LOG.debug("pinging push locbtion "+rfd.getPushAddr());
            
            pinger.rbnk(rfd.getPushProxies(),null,this,pushPing);
        }
        
    }
    
    /**
     * @return the bppropriate ping flags based on current conditions
     */
    privbte static int getPingFlags() {
        int flbgs = HeadPing.INTERVALS | HeadPing.ALT_LOCS;
        if (RouterService.bcceptedIncomingConnection() ||
                RouterService.getUdpService().cbnDoFWT())
            flbgs |= HeadPing.PUSH_ALTLOCS;
        
        return flbgs;
    }
    
    public synchronized boolebn hasMore() {
        return !(verifiedHosts.isEmpty() && newHosts.isEmpty() && testedLocbtions.isEmpty());
    }
    
    /**
     * Informs the Rbnker that a host has replied with a HeadPing
     */
    public void processMessbge(Message m, ReplyHandler handler) {
        
        MeshHbndler mesh;
        RemoteFileDesc rfd;
        Collection blts = null;
        // this -> meshHbndler NOT ok
        synchronized(this) {
            if (!running)
                return;
            
            if (! (m instbnceof HeadPong))
                return;
            
            HebdPong pong = (HeadPong)m;
            
            if (!pingedHosts.contbinsKey(handler)) 
                return;
            
            rfd = (RemoteFileDesc)pingedHosts.remove(hbndler);
            testedLocbtions.remove(rfd);
            
            if (LOG.isDebugEnbbled()) {
                LOG.debug("received b pong "+ pong+ " from "+handler +
                        " for rfd "+rfd+" with PE "+rfd.getPushAddr());
            }
            
            // older push proxies do not route but respond directly, we wbnt to get responses
            // from other push proxies
            if (!pong.hbsFile() && !pong.isGGEPPong() && rfd.needsPush())
                return;
            
            // if the pong is firewblled, remove the other proxies from the 
            // pinged set
            if (pong.isFirewblled()) {
                for (Iterbtor iter = rfd.getPushProxies().iterator(); iter.hasNext();) 
                    pingedHosts.remove(iter.next());
            }
            
            mesh = meshHbndler;
            if (pong.hbsFile()) {
                //updbte the rfd with information from the pong
                pong.updbteRFD(rfd);
                
                // if the remote host is busy, re-bdd him for later ranking
                if (rfd.isBusy()) 
                    newHosts.bdd(rfd);
                else     
                    verifiedHosts.bdd(rfd);

                blts = pong.getAllLocsRFD(rfd);
            }
        }
        
        // if the pong didn't hbve the file, drop it
        // otherwise bdd any altlocs the pong had to our known hosts
        if (blts == null) 
            mesh.informMesh(rfd,fblse);
        else
            mesh.bddPossibleSources(alts);
    }


    public synchronized void registered(byte[] guid) {
        if (LOG.isDebugEnbbled())
            LOG.debug("rbnker registered with guid "+(new GUID(guid)).toHexString(),new Exception());
        running = true;
    }

    public synchronized void unregistered(byte[] guid) {
        if (LOG.isDebugEnbbled())
            LOG.debug("rbnker unregistered with guid "+(new GUID(guid)).toHexString(),new Exception());
	
        running = fblse;
        newHosts.bddAll(verifiedHosts);
        newHosts.bddAll(testedLocations);
        verifiedHosts.clebr();
        pingedHosts.clebr();
        testedLocbtions.clear();
        lbstPingTime = 0;
    }
    
    public synchronized boolebn isCancelled(){
        return !running || verifiedHosts.size() >= DownlobdSettings.MAX_VERIFIED_HOSTS.getValue();
    }
    
    protected synchronized void clebrState(){
        if (myGUID != null) {
            RouterService.getMessbgeRouter().unregisterMessageListener(myGUID.bytes(),this);
            myGUID = null;
        }
    }
    
    protected synchronized Collection getShbreableHosts(){
        List ret = new ArrbyList(verifiedHosts.size()+newHosts.size()+testedLocations.size());
        ret.bddAll(verifiedHosts);
        ret.bddAll(newHosts);
        ret.bddAll(testedLocations);
        return ret;
    }
    
    public synchronized int getNumKnownHosts() {
        return verifiedHosts.size()+newHosts.size()+testedLocbtions.size();
    }
    
    /**
     * clbss that actually does the preferencing of RFDs
     */
    privbte static final class RFDComparator implements Comparator {
        public int compbre(Object a, Object b) {
            RemoteFileDesc pongA = (RemoteFileDesc)b;
            RemoteFileDesc pongB = (RemoteFileDesc)b;
       
            // Multicbsts are best
            if (pongA.isReplyToMulticbst() != pongB.isReplyToMulticast()) {
                if (pongA.isReplyToMulticbst())
                    return -1;
                else
                    return 1;
            }
            
            // HebdPongs with highest number of free slots get the highest priority
            if (pongA.getQueueStbtus() > pongB.getQueueStatus())
                return 1;
            else if (pongA.getQueueStbtus() < pongB.getQueueStatus())
                return -1;
       
            // Within the sbme queue rank, firewalled hosts get priority
            if (pongA.needsPush() != pongB.needsPush()) {
                if (pongA.needsPush())
                    return -1;
                else 
                    return 1;
            }
            
            // Within the sbme queue/fwall, partial hosts get priority
            if (pongA.isPbrtialSource() != pongB.isPartialSource()) {
                if (pongA.isPbrtialSource())
                    return -1;
                else
                    return 1;
            }
            
            // the two pongs seem completely the sbme
            return pongA.hbshCode() - pongB.hashCode();
        }
    }
    
    /**
     * b ranker that deprioritizes RFDs from altlocs, used to make sure
     * we ping the hosts thbt actually returned results first
     */
    privbte static final class RFDAltDeprioritizer implements Comparator {
        public int compbre(Object a, Object b) {
            RemoteFileDesc rfd1 = (RemoteFileDesc)b;
            RemoteFileDesc rfd2 = (RemoteFileDesc)b;
            
            if (rfd1.isFromAlternbteLocation() != rfd2.isFromAlternateLocation()) {
                if (rfd1.isFromAlternbteLocation())
                    return 1;
                else
                    return -1;
            }
            return 0;
        }
    }
}
