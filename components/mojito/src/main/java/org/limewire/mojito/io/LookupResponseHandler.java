package org.limewire.mojito.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.ManagedRunnable;
import org.limewire.mojito.entity.LookupEntity;
import org.limewire.mojito.message.ResponseMessage;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.RouteTable.SelectMode;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.settings.LookupSettings;
import org.limewire.mojito.util.ContactsScrubber;
import org.limewire.mojito.util.MaxStack;
import org.limewire.mojito.util.SchedulingUtils;
import org.limewire.mojito.util.ContactsScrubber.Scrubbed;
import org.limewire.security.SecurityToken;

public abstract class LookupResponseHandler<V extends LookupEntity> 
        extends AbstractResponseHandler<V> {
    
    protected static enum Type {
        FIND_NODE,
        FIND_VALUE;
    }
    
    protected final KUID lookupId;
    
    private final LookupManager lookupManager;
    
    private final MaxStack lookupCounter;
    
    private volatile long startTime = -1L;
    
    private volatile ScheduledFuture<?> boostFuture = null;
    
    public LookupResponseHandler(Type type, 
            Context context, KUID lookupId,
            long timeout, TimeUnit unit) {
        super(context, timeout, unit);
        
        this.lookupId = lookupId;
        
        RouteTable routeTable = context.getRouteTable();
        Collection<Contact> contacts 
            = routeTable.select(lookupId, 
                    KademliaSettings.K, SelectMode.ALL);
        
        lookupManager = new LookupManager(context, 
                lookupId, contacts.toArray(new Contact[0]));
        
        lookupCounter = createStack(type);
    }
    
    public LookupResponseHandler(Type type, 
            Context context, KUID lookupId,
            Contact[] contacts,
            long timeout, TimeUnit unit) {
        super(context, timeout, unit);
        
        this.lookupId = lookupId;
        
        lookupManager = new LookupManager(context, 
                lookupId, contacts);
        
        lookupCounter = createStack(type);
    }

    @Override
    protected synchronized void start() throws IOException {
        long boostFrequency = LookupSettings.BOOST_FREQUENCY.getTimeInMillis();
        
        if (0L < boostFrequency) {
            Runnable task = new ManagedRunnable() {
                @Override
                protected void doRun() throws IOException {
                    boost();
                }
            };
            
            boostFuture = SchedulingUtils.scheduleWithFixedDelay(task, 
                    boostFrequency, boostFrequency, TimeUnit.MILLISECONDS);
        }
        
        process(0);
    }
    
    @Override
    protected synchronized void stop() {
        if (boostFuture != null) {
            boostFuture.cancel(true);
        }
        
        super.stop();
    }

    /**
     * 
     */
    private synchronized void boost() throws IOException {
        if (lookupManager.hasNext(true)) {
            long boostTimeout = LookupSettings.BOOST_TIMEOUT.getTimeInMillis();
            if (getLastResponseTime(TimeUnit.MILLISECONDS) >= boostTimeout) {
                try {
                    Contact contact = lookupManager.next();
                    lookup(contact, lookupId, timeout, unit);
                    
                    lookupCounter.push(true);
                } finally {
                    postProcess();
                }
            }
        }
    }
    
    /**
     * 
     */
    private void process(int count) throws IOException {
        try {
            preProcess(count);
            while (lookupCounter.hasFree()) {
               if (!lookupManager.hasNext()) {
                   break;
               }
               
               Contact contact = lookupManager.next();
               lookup(contact, lookupId, timeout, unit);
               
               lookupCounter.push();
            }
        } finally {
            postProcess();
        }
    }
    
    /**
     * 
     */
    private void preProcess(int count) {
        if (startTime == -1L) {
            startTime = System.currentTimeMillis();
        }
        
        lookupCounter.pop(count);
    }
    
    /**
     * 
     */
    private void postProcess() {
        int count = lookupCounter.poll();
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
    protected final void processResponse(RequestHandle request, 
            ResponseMessage response, long time,
            TimeUnit unit) throws IOException {
        try {
            processResponse0(request, response, time, unit);
        } finally {
            process(1);
        }
    }
    
    /**
     * 
     */
    protected abstract void processResponse0(RequestHandle request, 
            ResponseMessage response, long time,
            TimeUnit unit) throws IOException;
    
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
    protected final void processTimeout(RequestHandle request, 
            long time, TimeUnit unit) throws IOException {
        try {
            processTimeout0(request, time, unit);
        } finally {
            process(1);
        }
    }
    
    /**
     * 
     */
    protected void processTimeout0(RequestHandle request, 
            long time, TimeUnit unit) throws IOException {
        lookupManager.handleTimeout(request, time, unit);
    }
    
    /**
     * 
     */
    protected synchronized State getState() {
        if (startTime == -1L) {
            throw new IllegalStateException("startTime=" + startTime);
        }
        
        Entry<Contact, SecurityToken>[] contacts 
            = lookupManager.getContacts();
        
        Contact[] collisions = lookupManager.getCollisions();
        
        int routeTableTimeouts = lookupManager.getRouteTableTimeouts();
        int timeouts = lookupManager.getTimeouts();
        int hop = lookupManager.getCurrentHop();
        long time = System.currentTimeMillis() - startTime;
        
        return new State(lookupId, contacts, collisions, 
                routeTableTimeouts, timeouts, hop, 
                time, TimeUnit.MILLISECONDS);
    }
    
    private class LookupManager {
        
        private final boolean RANDOMIZE = LookupSettings.RANDOMIZE.getValue();
        
        private final boolean EXHAUSTIVE = LookupSettings.EXHAUSTIVE.getValue();
        
        private final Context context;
        
        private final KUID lookupId;
        
        /**
         * 
         */
        private final Set<KUID> init = new HashSet<KUID>();
        
        private final List<Contact> collisions = new ArrayList<Contact>();
        
        private final NavigableMap<Contact, SecurityToken> responses;
        
        private final NavigableSet<Contact> closest;
        
        private final NavigableSet<Contact> query;
        
        private final Map<KUID, Integer> history 
            = new HashMap<KUID, Integer>();
        
        private int currentHop = 0;
        
        private int timeouts = 0;
        
        private int routeTableTimeouts = 0;
        
        public LookupManager(Context context, 
                KUID lookupId, Contact[] contacts) {
            
            this.context = context;
            this.lookupId = lookupId;
            
            Comparator<Contact> comparator 
                = new XorComparator(lookupId);
            
            responses = new TreeMap<Contact, SecurityToken>(comparator);
            closest = new TreeSet<Contact>(comparator);
            query = new TreeSet<Contact>(comparator);
            
            RouteTable routeTable = context.getRouteTable();
            Contact localhost = routeTable.getLocalNode();
            KUID contactId = localhost.getNodeID();
            
            history.put(contactId, 0);
            
            if (0 < contacts.length) {
                addToResponses(localhost, null);
                
                for (Contact contact : contacts) {
                    addToQuery(contact, 1);
                    
                    init.add(contact.getNodeID());
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
            
            Scrubbed scrubbed = ContactsScrubber.scrub(context, src, contacts, 
                    LookupSettings.CONTACTS_SCRUBBER_REQUIRED_RATIO.getValue());
            
            if (!scrubbed.isValid()) {
                return false;
            }
            
            boolean success = addToResponses(src, securityToken);
            if (!success) {
                return false;
            }
            
            RouteTable routeTable = context.getRouteTable();
            int hop = currentHop + 1;
            
            for (Contact contact : scrubbed.getScrubbed()) {
                if (addToQuery(contact, hop)) {
                    routeTable.add(contact);
                }
            }
            
            for (Contact collision : scrubbed.getCollisions()) {
                collisions.add(collision);
            }
            
            return true;
        }
        
        public void handleTimeout(RequestHandle handle, 
                long time, TimeUnit unit) {
            
            KUID contactId = handle.getContactId();
            if (init.contains(contactId)) {
                ++routeTableTimeouts;
            }
            
            ++timeouts;
        }
        
        @SuppressWarnings("unchecked")
        public Entry<Contact, SecurityToken>[] getContacts() {
            return responses.entrySet().toArray(new Entry[0]);
        }
        
        public Contact[] getCollisions() {
            return collisions.toArray(new Contact[0]);
        }
        
        public int getCurrentHop() {
            return currentHop;
        }
        
        public int getTimeouts() {
            return timeouts;
        }
        
        public int getRouteTableTimeouts() {
            return routeTableTimeouts;
        }
        
        private boolean addToResponses(Contact contact, 
                SecurityToken securityToken) {
            
            if (!responses.containsKey(contact)) {
                responses.put(contact, securityToken);
                closest.add(contact);
                
                if (closest.size() > KademliaSettings.K) {
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
                return otherId.isNearerTo(lookupId, contactId);
            }
            return true;
        }
        
        public boolean hasNext() {
            return hasNext(false);
        }
        
        public boolean hasNext(boolean force) {
            if (!query.isEmpty()) {
                Contact contact = query.first();
                if (force || closest.size() < KademliaSettings.K
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
                        
                        if (contacts.size() >= KademliaSettings.K) {
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
        
        private final Contact[] collisions;
        
        private final int hop;
        
        private final int routeTableTimeouts;
        
        private final int timeouts;
        
        private final long time;
        
        private final TimeUnit unit;
        
        private State(KUID key, Entry<Contact, SecurityToken>[] contacts, 
                Contact[] collisions, int routeTableTimeouts, int timeouts, 
                int hop, long time, TimeUnit unit) {
            
            this.key = key;
            this.contacts = contacts;
            
            this.collisions = collisions;
            this.routeTableTimeouts = routeTableTimeouts;
            this.timeouts = timeouts;
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
        
        public Contact[] getCollisions() {
            return collisions;
        }
        
        public int getTimeouts() {
            return timeouts;
        }
        
        public int getRouteTableTimeouts() {
            return routeTableTimeouts;
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
    
    /**
     * 
     */
    public static MaxStack createStack(Type type) {
        int alpha = -1;
        switch (type) {
            case FIND_NODE:
                alpha = LookupSettings.FIND_NODE_PARALLEL_LOOKUPS.getValue();
                break;
            case FIND_VALUE:
                alpha = LookupSettings.FIND_VALUE_PARALLEL_LOOKUPS.getValue();
                break;
        }
        
        return new MaxStack(alpha);
    }
}
