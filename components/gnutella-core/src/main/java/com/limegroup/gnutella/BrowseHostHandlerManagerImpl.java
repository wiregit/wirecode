package com.limegroup.gnutella;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.net.SocketsManager;
import org.limewire.service.ErrorService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.BrowseHostHandler.PushRequestDetails;
import com.limegroup.gnutella.downloader.PushDownloadManager;
import com.limegroup.gnutella.messages.MessageFactory;

@Singleton
public class BrowseHostHandlerManagerImpl implements BrowseHostHandlerManager {

    private static final Log LOG = LogFactory.getLog(BrowseHostHandlerManagerImpl.class);

    /** Map from serventID to BrowseHostHandler instance. */
    private final Map<GUID, BrowseHostHandler.PushRequestDetails> _pushedHosts = new HashMap<GUID, BrowseHostHandler.PushRequestDetails>();

    private final Provider<ActivityCallback> activityCallback;
    private final SocketsManager socketsManager;
    private final Provider<PushDownloadManager> pushDownloadManager;
    private final Provider<ForMeReplyHandler> forMeReplyHandler;

    private final MessageFactory messageFactory;

    @Inject
    public BrowseHostHandlerManagerImpl(@Named("backgroundExecutor")
    ScheduledExecutorService backgroundExecutor,
            Provider<ActivityCallback> activityCallback,
            SocketsManager socketsManager,
            Provider<PushDownloadManager> pushDownloadManager,
            Provider<ForMeReplyHandler> forMeReplyHandler,
            MessageFactory messageFactory) {
        this.activityCallback = activityCallback;
        this.socketsManager = socketsManager;
        this.pushDownloadManager = pushDownloadManager;
        this.forMeReplyHandler = forMeReplyHandler;
        this.messageFactory = messageFactory;

        // TODO: move an an initializer
        backgroundExecutor.scheduleWithFixedDelay(new Expirer(), 0, 5000, TimeUnit.MILLISECONDS);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.BrowseHostHandlerManager#createBrowseHostHandler(com.limegroup.gnutella.ActivityCallback,
     *      com.limegroup.gnutella.GUID, com.limegroup.gnutella.GUID)
     */
    public BrowseHostHandler createBrowseHostHandler(GUID guid, GUID serventID) {
        return new BrowseHostHandler(guid, serventID, 
                    new BrowseHostCallback() {
                        public void putInfo(GUID serventId, PushRequestDetails details) {
                            synchronized(_pushedHosts) {
                                _pushedHosts.put(serventId, details);
                            }                
                        }
                    },
                activityCallback.get(), socketsManager, pushDownloadManager,
                forMeReplyHandler, messageFactory);
    }

    /** @return true if the Push was handled by me. */
    public boolean handlePush(int index, GUID serventID, final Socket socket) {
        boolean retVal = false;
        LOG.trace("BHH.handlePush(): entered.");
        // if (index == SPECIAL_INDEX)
        // ; // you'd hope, but not necessary...

        BrowseHostHandler.PushRequestDetails prd = null;
        synchronized (_pushedHosts) {
            prd = _pushedHosts.remove(serventID);
        }
        if (prd != null) {
            final BrowseHostHandler.PushRequestDetails finalPRD = prd;
            ThreadExecutor.startThread(new Runnable() {
                public void run() {
                    try {
                        finalPRD.getBrowseHostHandler().browseExchange(socket);
                    } catch (IOException ohWell) {
                        LOG.debug("error while push transfer", ohWell);
                        finalPRD.getBrowseHostHandler().failed();
                    }
                }
            }, "BrowseHost");
            retVal = true;
        } else
            LOG.debug("BHH.handlePush(): no matching BHH.");

        LOG.trace("BHH.handlePush(): returning.");
        return retVal;
    }

    /** Can be run to invalidate pushes that we are waiting for.... */
    private class Expirer implements Runnable {
        public void run() {
            try {
                Set<GUID> toRemove = new HashSet<GUID>();
                synchronized (_pushedHosts) {
                    for (GUID key : _pushedHosts.keySet()) {
                        BrowseHostHandler.PushRequestDetails currPRD = _pushedHosts
                                .get(key);
                        if ((currPRD != null) && (currPRD.isExpired())) {
                            LOG.debug("Expirer.run(): expiring a badboy.");
                            toRemove.add(key);
                            currPRD.getBrowseHostHandler().failed();
                        }
                    }
                    for (GUID key : toRemove)
                        _pushedHosts.remove(key);
                }
            } catch (Throwable t) {
                ErrorService.error(t);
            }
        }
    }

}
