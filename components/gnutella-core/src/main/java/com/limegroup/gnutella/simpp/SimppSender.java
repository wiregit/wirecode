package com.limegroup.gnutella.simpp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.collection.Tuple;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.ServiceScheduler;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.MessageSentEvent;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.ConnectionLifecycleListener;
import com.limegroup.gnutella.messagehandlers.MessageHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.SimppRequestVM;
import com.limegroup.gnutella.messages.vendor.SimppVM;

/**
 * Class responsible of managing the sending out of simpp messages to peers.
 * <p>
 * A single queue is maintained for peers requesting the simpp message and 
 * one simpp message at a time is sent to a peer. After that the sender waits
 * for the simpp message to be fully sent before sending the next message.
 * <p>
 * Simpp message requestors are removed from the queue in two cases:
 * 
 * 1. The connection dies.
 * 2. They send a capabilities vm signaling that they have a simpp version
 *    greater than or equal to the one this peer has
 */
@EagerSingleton
public class SimppSender {
    
    private static final Log LOG = LogFactory.getLog(SimppSender.class);

    private final Provider<SimppManager> simppManager;

    /**
     * Lock object to synchronize access to {@link #requestQueue},
     * {@link #current} and {@link #timeoutFuture}.
     */
    private final Object lock = new Object();
    /**
     * Queue of simpp requestors and their simpp request message. Invariance:
     * each requestor is only once in the queue.
     */
    private final Queue<Tuple<SimppRequestVM, ReplyHandler>> requestQueue = new LinkedList<Tuple<SimppRequestVM, ReplyHandler>>();
    /**
     * The requestor that is currently being serviced with a simpp message.
     */
    private Tuple<SimppRequestVM, ReplyHandler> current;
    /**
     * Future of timeout task that checks that a simpp vm was sent within one
     * minute.
     */
    private ScheduledFuture<?> timeoutFuture;
    
    private final ScheduledExecutorService scheduledExecutorService;

    @Inject
    public SimppSender(Provider<SimppManager> simppManager, @Named("backgroundExecutor") ScheduledExecutorService scheduledExecutorService) {
        this.simppManager = simppManager;
        this.scheduledExecutorService = scheduledExecutorService;
    }
    
    @Inject
    void register(MessageRouter messageRouter) {
        messageRouter.addMessageHandler(SimppRequestVM.class, new SimppRequestVMMessageHandler());
        messageRouter.addMessageHandler(CapabilitiesVM.class, new CapabilitiesVMMessageHandler());
    }
    
    @Inject
    void register(ConnectionManager connectionManager) {
        connectionManager.addEventListener(new ConnectionHandler());
    }
    
    @Inject
    void register(ListenerSupport<MessageSentEvent> messageSentListenerSupport) {
        messageSentListenerSupport.addListener(new SimppMessageSentHandler());
    }
    
    @Inject
    void register(@Named("backgroundExecutor") ScheduledExecutorService scheduledExecutorService,
            ServiceScheduler serviceScheduler) {
        // solely for testing purposes, take snapshot every minute and ensure the queue doesn't stagnate
        final AtomicReference<List<Tuple<SimppRequestVM, ReplyHandler>>> snapshot = new AtomicReference<List<Tuple<SimppRequestVM,ReplyHandler>>>();
        long delay = ConnectionSettings.SIMPP_SEND_TIMEOUT.getValue() * 2;
        serviceScheduler.scheduleWithFixedDelay("simpp sender snapshot", new Runnable() {
            @Override
            public void run() {
                LOG.debug("snapshotting");
                List<Tuple<SimppRequestVM, ReplyHandler>> list;
                synchronized (lock) {
                    list = new ArrayList<Tuple<SimppRequestVM,ReplyHandler>>(requestQueue);
                }
                List<Tuple<SimppRequestVM, ReplyHandler>> old = snapshot.get();
                if (!list.isEmpty() && old != null) {
                    assert !list.equals(old); 
                }
                snapshot.set(list);
            }
        }, delay, delay, TimeUnit.MILLISECONDS, scheduledExecutorService);
    }

    /**
     * Removes <code>handler</code> from the queue.
     */
    void removeReplyHandlerFromQueue(ReplyHandler handler) {
        LOG.debug("remove handler from queue");
        synchronized (lock) {
            for (Iterator<Tuple<SimppRequestVM, ReplyHandler>> i = requestQueue.iterator(); i.hasNext();) {
                Tuple<SimppRequestVM, ReplyHandler> tuple = i.next();
                ReplyHandler other = tuple.getSecond();
                if (other == handler) {
                    LOG.debugf("removing {0} from queue", handler);
                    i.remove();
                    return;
                }
            }
        }
    }
    
    /**
     * Sends the simpp message to the next requestor in the the queue.
     * <p>
     * Should not be called while lock is held, since it calls
     * {@link #sendSimppVM(SimppRequestVM, ReplyHandler)}
     * which calls potentially blocking methods outside of our control.
     * <p>
     * If it doesn't have data to send it will go through all entries
     * in the queue and try to send to them.
     * 
     * @param currentReplyHandler if not null, only send to the first
     * one in the queue if currentReplyHandler is the current one.
     */
    void sendToNextInQueue() {
        LOG.debug("send simpp to next in queue");
        SimppRequestVM simppRequest = null;
        ReplyHandler replyHandler = null;
        byte[] data = null;
        synchronized (lock) {
            while (true) {
                current = requestQueue.poll();
                if (current == null) {
                    LOG.debug("queue is empty");
                    return;
                }
                simppRequest = current.getFirst();
                replyHandler = current.getSecond();
                data = getDataToSend(simppRequest);
                if (data.length > 0) {
                    LOG.debugf("sending to {0}", replyHandler);
                    final ReplyHandler staleReplyHandler = replyHandler;
                    if (timeoutFuture != null) {
                        timeoutFuture.cancel(false);
                    }
                    timeoutFuture = scheduledExecutorService.schedule(new Runnable() {
                        @Override
                        public void run() {
                            LOG.debugf("time out for {0}", staleReplyHandler);
                            sendToNextInQueueIfCurrent(staleReplyHandler);
                        }
                    }, ConnectionSettings.SIMPP_SEND_TIMEOUT.getValue(), TimeUnit.MILLISECONDS);
                    break;
                } else {
                    LOG.debugf("no data to send to: {0}", replyHandler);
                }
            }
        }
        sendSimppVM(simppRequest, data, replyHandler);
    }

    /**
     * Sends simpp to the next requestor in the queue if <code>staleReplyHandler</code>
     * is the current reply handler.
     */
    void sendToNextInQueueIfCurrent(ReplyHandler staleReplyHandler) {
        LOG.debug("send to next if current");
        boolean sendToNext = false;
        synchronized (lock) {
            if (current != null && current.getSecond() == staleReplyHandler) {
                LOG.debug("stale handler is current one");
                sendToNext = true;
                if (timeoutFuture != null) {
                    LOG.debugf("cancel future");
                    timeoutFuture.cancel(false);
                    timeoutFuture = null;
                }
            }
        }
        if (sendToNext) {
            sendToNextInQueue();
        }
    }
    
    /**
     * Removes <code>staleReplyHandler</code> from the queue of requestors,
     * or, if <code>staleReplyHandler</code> is the current requestor being
     * serviced, starts sending to the next requestor in the queue.
     */
    void removeStaleReplyHandler(ReplyHandler staleReplyHandler) {
        LOG.debug("remove stale reply handler");
        boolean sendToNext = false;
        synchronized (lock) {
            if (current != null && current.getSecond() == staleReplyHandler) {
                LOG.debug("stale handler is current one");
                sendToNext = true;
            } else {
                removeReplyHandlerFromQueue(staleReplyHandler);
            }
        }
        if (sendToNext) {
            sendToNextInQueue();
        }
    }
    
    /**
     * @return true if <code>handler</code> is in the queue of requestors 
     */
    boolean queueContains(ReplyHandler handler) {
        synchronized (lock) {
            for (Tuple<SimppRequestVM, ReplyHandler> tuple : requestQueue) {
                if (tuple.getSecond() == handler) {
                    LOG.debugf("queue contains: {0}", handler);
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Enqueues <code>handler's</code> request for a simpp message or sends it
     * to the <code>handler</code> directly if the queue is empty.  
     */
    void enqueueSimppRequest(final SimppRequestVM simppRequest, final ReplyHandler handler) {
        if (LOG.isDebugEnabled())
            LOG.debugf("{0} handle request {1}", handler, simppManager.get().getVersion());
        Tuple<SimppRequestVM, ReplyHandler> requestTuple = new Tuple<SimppRequestVM, ReplyHandler>(simppRequest, handler);
        boolean send = false;
        synchronized (lock) {
            if (current != null && current.getSecond() == handler) {
                LOG.debugf("currently being serviced {0}", handler);
                return;
            }
            if (queueContains(handler)) {
                LOG.debugf("already in queue {0}", handler);
                return;
            }
            LOG.debugf("queuing simpp requestor: {0}", handler);
            requestQueue.add(requestTuple);
            // send to next in queue, if no current one and it's the
            // first one in the queue
            send = current == null && requestQueue.size() == 1;
        }
        if (send) {
            LOG.debugf("sending immediately: {0}", handler);
            sendToNextInQueue();
        }
    }
    
    private byte[] getDataToSend(SimppRequestVM simppRequest) {
        return simppRequest.isOldRequest() ? simppManager.get().getOldUpdateResponse()
                : simppManager.get().getSimppBytes();    
    }
    
    /**
     * Sends a simpp message to <code>replyHandler</code>.
     * <p>
     * If there is no simpp data to send, goes to next in queue. 
     */
    private void sendSimppVM(SimppRequestVM simppRequest, byte[] data, ReplyHandler replyHandler) {
        assert data.length > 0;
        SimppVM simppVM = SimppVM.createSimppResponse(simppRequest, data);
        try {
            replyHandler.handleSimppVM(simppVM);
        } catch (IOException iox) {
            throw new RuntimeException(iox);
        }
    }

    /**
     * Listens for incoming {@link SimppRequestVM} messages and enqueues
     * the requestors.
     */
    private class SimppRequestVMMessageHandler implements MessageHandler {
        @Override
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            enqueueSimppRequest((SimppRequestVM)msg, handler);
        }
    }
    
    /**
     * Listens for incoming {@link CapabilitiesVM} messages and removes the
     * sender from the request queue if it already has this peer's simpp version.
     */
    private class CapabilitiesVMMessageHandler implements MessageHandler {
        @Override
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            int currentSimppVersion = simppManager.get().getVersion();
            if (((CapabilitiesVM)msg).supportsSIMPP() >= currentSimppVersion) {
                if (LOG.isDebugEnabled())
                    LOG.debugf("{0} already has version {1}", handler, currentSimppVersion);
                removeReplyHandlerFromQueue(handler);
            }
        }
    }

    /**
     * Listens for disconnect events of peer connections and removes the stale
     * connections from the queue of requests.
     */
    private class ConnectionHandler implements ConnectionLifecycleListener {
        @Override
        public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent event) {
            if (event.isDisconnectedEvent() || event.isConnectionClosedEvent()) {
                LOG.debugf("disconnection event: {0}", event);
                removeStaleReplyHandler(event.getConnection());
            }
        }
    }
    
    /**
     * Listens for {@link MessageSentEvent} of simpp vms and then schedules
     * the simpp to be sent to the next requestor in the queue.
     */
    private class SimppMessageSentHandler implements EventListener<MessageSentEvent> {
        @Override
        public void handleEvent(MessageSentEvent event) {
            if (event.getData() instanceof SimppVM) {
                LOG.debugf("done sending to {0}", event);
                sendToNextInQueueIfCurrent(event.getSource());
            }
        }
    }
}
