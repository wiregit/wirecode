package org.limewire.mojito.handler.response2;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent2.AsyncFuture;
import org.limewire.mojito.concurrent2.ExecutorUtils;
import org.limewire.mojito.concurrent2.ManagedRunnable;
import org.limewire.mojito.entity.LookupEntity;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.RouteTable.SelectMode;
import org.limewire.mojito.settings.LookupSettings;
import org.limewire.mojito.util.ContactsScrubber;
import org.limewire.security.SecurityToken;

public abstract class LookupResponseHandler<V extends LookupEntity> extends AbstractResponseHandler<V> {
    
    private static final ScheduledThreadPoolExecutor BOOST 
        = ExecutorUtils.newSingleThreadScheduledExecutor(
            "BoostLookupThread");
    
    private static final long BOOST_FREQUENCY = 1000L;
    
    private static final int ALPHA = 4;
    
    private volatile long boostFrequency = BOOST_FREQUENCY;
    
    protected final KUID key;
    
    private final LookupManager lookupManager;
    
    private final ProcessCounter lookupCounter;
    
    private volatile long timeout = 3000L;
    
    private volatile long boostTimeout = 3000L;
    
    private volatile int alpha = ALPHA;
    
    private volatile long startTime = -1L;
    
    private volatile ScheduledFuture<?> boostFuture = null;
    
    public LookupResponseHandler(Context context, KUID key) {
        super(context);
        
        this.key = key;
        
        lookupManager = new LookupManager(context, key);
        lookupCounter = new ProcessCounter(alpha);
    }

    @Override
    protected synchronized void doStart(AsyncFuture<V> future) throws Exception {
        long boostFrequency = this.boostFrequency;
        
        if (0L < boostFrequency) {
            Runnable task = new ManagedRunnable() {
                @Override
                protected void doRun() throws IOException {
                    boost();
                }
            };
            
            boostFuture = BOOST.scheduleWithFixedDelay(task, 
                    boostFrequency, boostFrequency, TimeUnit.MILLISECONDS);
        }
        
        process(0);
    }
    
    @Override
    protected void doStop(AsyncFuture<V> future) {
        if (boostFuture != null) {
            boostFuture.cancel(true);
        }
    }

    /**
     * 
     */
    private synchronized void boost() throws IOException {
        if (lookupManager.hasNext(true)) {
            long boostTimeout = this.boostTimeout;
            if (getLastResponseTime(TimeUnit.MILLISECONDS) >= boostTimeout) {
                try {
                    Contact contact = lookupManager.next();
                    lookup(contact, key, timeout, TimeUnit.MILLISECONDS);
                    lookupCounter.increment(true);
                } finally {
                    postProcess();
                }
            }
        }
    }
    
    /**
     * 
     */
    private void process(int decrement) throws IOException {
        try {
            preProcess(decrement);
            while (lookupCounter.hasNext()) {
               if (!lookupManager.hasNext()) {
                   break;
               }
               
               Contact contact = lookupManager.next();
               lookup(contact, key, timeout, TimeUnit.MILLISECONDS);
               
               lookupCounter.increment();
            }
        } finally {
            postProcess();
        }
    }
    
    /**
     * 
     */
    private void preProcess(int decrement) {
        if (startTime == -1L) {
            startTime = System.currentTimeMillis();
        }
        
        lookupCounter.decrement(decrement);
    }
    
    /**
     * 
     */
    private void postProcess() {
        int count = lookupCounter.get();
        if (count == 0) {
            State state = getState();
            complete(state);
        }
    }
    
    /**
     * 
     */
    protected abstract void lookup(Contact dst, KUID key, 
            long timeout, TimeUnit unit) throws IOException;
    
    /**
     * 
     */
    protected abstract void complete(State state);
    
    /**
     * 
     */
    @Override
    protected final void processResponse(ResponseMessage message, 
            long time, TimeUnit unit) throws IOException {
        try {
            processResponse0(message, time, unit);
        } finally {
            process(1);
        }
    }
    
    /**
     * 
     */
    protected abstract void processResponse0(ResponseMessage message, 
            long time, TimeUnit unit) throws IOException;
    
    /**
     * 
     */
    protected boolean processContacts(Contact src, SecurityToken securityToken, 
            Contact[] contacts, long time, TimeUnit unit) {
        return lookupManager.handleResponse(src, securityToken, 
                contacts, time, unit);
    }
    
    /**
     * 
     */
    @Override
    protected final void processTimeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time, TimeUnit unit) throws IOException {
        try {
            processTimeout0(nodeId, dst, message, time, unit);
        } finally {
            process(1);
        }
    }
    
    /**
     * 
     */
    protected void processTimeout0(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time, TimeUnit unit) throws IOException {
        lookupManager.handleTimeout(time, unit);
    }
    
    /**
     * 
     */
    protected synchronized State getState() {
        if (startTime == -1L) {
            throw new IllegalStateException("startTime=" + startTime);
        }
        
        Entry<Contact, SecurityToken>[] contacts = lookupManager.getContacts();
        int hop = lookupManager.getCurrentHop();
        long time = System.currentTimeMillis() - startTime;
        
        return new State(key, contacts, hop, 
                time, TimeUnit.MILLISECONDS);
    }
    
    private static class LookupManager {
        
        private static final int K = 20;
        
        private static boolean EXHAUSTIVE = false;
        
        private static boolean RANDOMIZE = false;
        
        private final Context context;
        
        private final KUID key;
        
        private final List<Contact> collisions = new ArrayList<Contact>();
        
        private final NavigableMap<Contact, SecurityToken> responses;
        
        private final NavigableSet<Contact> closest;
        
        private final NavigableSet<Contact> query;
        
        private final Map<KUID, Integer> history 
            = new HashMap<KUID, Integer>();
        
        private int currentHop = 0;
        
        private int timeouts = 0;
        
        public LookupManager(Context context, KUID key) {
            this.context = context;
            this.key = key;
            
            Comparator<Contact> comparator 
                = new XorComparator(key);
            
            responses = new TreeMap<Contact, SecurityToken>(comparator);
            closest = new TreeSet<Contact>(comparator);
            query = new TreeSet<Contact>(comparator);
            
            RouteTable routeTable = context.getRouteTable();
            Contact localhost = routeTable.getLocalNode();
            KUID contactId = localhost.getNodeID();
            
            history.put(contactId, 0);
            Collection<Contact> contacts 
                = routeTable.select(key, K, SelectMode.ALL);
            
            if (!contacts.isEmpty()) {
                addToResponses(localhost, null);
                
                for (Contact contact : contacts) {
                    addToQuery(contact, 1);
                }
            }
        }
        
        public boolean handleResponse(Contact src, SecurityToken securityToken,
                Contact[] contacts, long time, TimeUnit unit) {
            
            // Nodes that are currently bootstrapping return no Contacts!
            if (contacts.length == 0) {
                if (LookupSettings.ACCEPT_EMPTY_FIND_NODE_RESPONSES.getValue()) {
                    return addToResponses(src, securityToken);
                }
                
                return true;
            }
            
            ContactsScrubber scrubber = ContactsScrubber.scrub(
                    context, src, contacts, 
                    LookupSettings.CONTACTS_SCRUBBER_REQUIRED_RATIO.getValue());
            if (!scrubber.isValidResponse()) {
                return false;
            }
            
            boolean success = addToResponses(src, securityToken);
            if (!success) {
                return false;
            }
            
            RouteTable routeTable = context.getRouteTable();
            int hop = currentHop + 1;
            
            for (Contact contact : scrubber.getScrubbed()) {
                if (addToQuery(contact, hop)) {
                    routeTable.add(contact);
                }
            }
            
            collisions.addAll(scrubber.getCollisions());
            
            return true;
        }
        
        public void handleTimeout(long time, TimeUnit unit) {
            ++timeouts;
        }
        
        @SuppressWarnings("unchecked")
        public Entry<Contact, SecurityToken>[] getContacts() {
            return responses.entrySet().toArray(new Entry[0]);
        }
        
        public int getCurrentHop() {
            return currentHop;
        }
        
        public int getTimeouts() {
            return timeouts;
        }
        
        private boolean addToResponses(Contact contact, 
                SecurityToken securityToken) {
            
            if (!responses.containsKey(contact)) {
                responses.put(contact, securityToken);
                closest.add(contact);
                
                if (closest.size() > K) {
                    closest.pollLast();
                }
                
                KUID contactId = contact.getNodeID();
                currentHop = history.get(contactId);
                return true;
            }
            return false;
        }
        
        private boolean addToQuery(Contact contact, int hop) {
            KUID contactId = contact.getNodeID();
            if (!history.containsKey(contactId)) {
                history.put(contactId, hop);
                query.add(contact);
                return true;
            }
            return false;
        }
        
        private boolean isCloserThanClosest(Contact other) {
            if (!closest.isEmpty()) {
                Contact contact = closest.last();
                KUID contactId = contact.getNodeID();
                KUID otherId = other.getNodeID();
                return otherId.isNearerTo(key, contactId);
            }
            return true;
        }
        
        public boolean hasNext() {
            return hasNext(false);
        }
        
        public boolean hasNext(boolean force) {
            if (!query.isEmpty()) {
                Contact contact = query.first();
                if (force || closest.size() < K
                        || isCloserThanClosest(contact)
                        || EXHAUSTIVE) {
                    return true;
                }
            }
            return false;
        }
        
        public Contact next() {
            Contact contact = null;
            
            if (RANDOMIZE) {
                if (!query.isEmpty()) {
                    // TODO: There is a better way to do it!
                    List<Contact> contacts = new ArrayList<Contact>();
                    for (Contact c : query) {
                        contacts.add(c);
                        
                        if (contacts.size() >= K) {
                            break;
                        }
                    }
                    
                    contact = contacts.get((int)(Math.random() * contacts.size()));
                    query.remove(contact);
                }
            } else {
                contact = query.pollFirst();
            }
            
            if (contact == null) {
                throw new NoSuchElementException();
            }
            
            return contact;
        }
    }
    
    private static class XorComparator implements Comparator<Contact> {

        private final KUID key;
        
        public XorComparator(KUID key) {
            this.key = key;
        }
        
        @Override
        public int compare(Contact o1, Contact o2) {
            return o1.getNodeID().xor(key).compareTo(o2.getNodeID().xor(key));
        }
    }
    
    public static class State {
        
        private final KUID key;
        
        private final Entry<Contact, SecurityToken>[] contacts;
        
        private final int hop;
        
        private final long time;
        
        private final TimeUnit unit;
        
        private State(KUID key, Entry<Contact, SecurityToken>[] contacts, 
                int hop, long time, TimeUnit unit) {
            this.key = key;
            this.contacts = contacts;
            this.hop = hop;
            this.time = time;
            this.unit = unit;
        }
        
        public KUID getKey() {
            return key;
        }
        
        public Entry<Contact, SecurityToken>[] getContacts() {
            return contacts;
        }
        
        public int getHop() {
            return hop;
        }
        
        public long getTime(TimeUnit unit) {
            return unit.convert(time, this.unit);
        }
        
        public long getTimeInMillis() {
            return getTime(TimeUnit.MILLISECONDS);
        }
    }
}
