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
import org.limewire.mojito.message.LookupRequest;
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

/**
 * An abstract implementation of a {@link ResponseHandler} that handles
 * lookups (<tt>FIND_NODE</tt> and <tt>FIND_VALUE</tt>).
 */
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
     * Tries to spawn additional lookups (boosting) if we haven't received
     * any responses for a certain amount of time.
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
     * Processes the given {@code count} number of responses and 
     * spawns more lookups.
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
     * Decrements the {@link #lookupCounter} by the given number
     * of responses.
     */
    private void preProcess(int count) {
        if (startTime == -1L) {
            startTime = System.currentTimeMillis();
        }
        
        lookupCounter.pop(count);
    }
    
    /**
     * Terminates the lookup if there is nothing left to do.
     */
    private void postProcess() {
        int count = lookupCounter.poll();
        if (count == 0) {
            State state = getState();
            complete(state);
        }
    }
    
    /**
     * Sends a {@link LookupRequest} to the given {@link Contact}.
     */
    protected abstract void lookup(Contact dst, KUID key, 
            long timeout, TimeUnit unit) throws IOException;
    
    /**
     * Called upon completion.
     */
    protected abstract void complete(State state);
    
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
     * Called for each {@link ResponseMessage}.
     */
    protected abstract void processResponse0(RequestHandle request, 
            ResponseMessage response, long time,
            TimeUnit unit) throws IOException;
    
    /**
     * Called for each {@link ResponseMessage}.
     */
    protected boolean processContacts(Contact src, SecurityToken securityToken, 
            Contact[] contacts, long time, TimeUnit unit) {
        return lookupManager.handleResponse(src, securityToken, 
                contacts, time, unit);
    }
    
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
     * Called for each timeout.
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
    
    /**
     * The {@link LookupManager} manages the lookup process.
     */
    private class LookupManager {
        
        private final boolean randomize 
            = LookupSettings.RANDOMIZE.getValue();
        
        private final boolean exchaustive 
            = LookupSettings.EXHAUSTIVE.getValue();
        
        private final Context context;
        
        private final KUID lookupId;
        
        /**
         * The initial set of {@link Contact} {@link KUID}s that were
         * picked from the {@link RouteTable}.
         */
        private final Set<KUID> init = new HashSet<KUID>();
        
        /**
         * All {@link Contact}s that collide with the localhost
         */
        private final List<Contact> collisions = new ArrayList<Contact>();
        
        /**
         * {@link Contact}s from which we received responses.
         */
        private final NavigableMap<Contact, SecurityToken> responses;
        
        /**
         * This is a sub-set of {@link #responses} where we keep only
         * the K-closest {@link Contact}s.
         */
        private final NavigableSet<Contact> closest;
        
        /**
         * A set of {@link Contact}s to query.
         */
        private final NavigableSet<Contact> query;
        
        /**
         * A map of {@link Contact}s we've sent requests to. To be 
         * more precise it's a map of their {@link KUID}s and the 
         * hop number.
         */
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
            KUID contactId = localhost.getContactId();
            
            history.put(contactId, 0);
            
            if (0 < contacts.length) {
                addToResponses(localhost, null);
                
                for (Contact contact : contacts) {
                    addToQuery(contact, 1);
                    
                    init.add(contact.getContactId());
                }
            }
        }
        
        /**
         * Called for each {@link ResponseMessage}.
         */
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
        
        /**
         * Called for each timeout.
         */
        public void handleTimeout(RequestHandle handle, 
                long time, TimeUnit unit) {
            
            KUID contactId = handle.getContactId();
            if (init.contains(contactId)) {
                ++routeTableTimeouts;
            }
            
            ++timeouts;
        }
        
        /**
         * Returns all {@link Contact}s and their {@link SecurityToken}s.
         */
        @SuppressWarnings("unchecked")
        public Entry<Contact, SecurityToken>[] getContacts() {
            return responses.entrySet().toArray(new Entry[0]);
        }
        
        /**
         * Returns all {@link Contact}s that collide with the localhost
         */
        public Contact[] getCollisions() {
            return collisions.toArray(new Contact[0]);
        }
        
        /**
         * Returns the current hop.
         */
        public int getCurrentHop() {
            return currentHop;
        }
        
        /**
         * Returns the number of timeouts that occurred during the lookup.
         */
        public int getTimeouts() {
            return timeouts;
        }
        
        /**
         * Returns the number of {@link RouteTable} timeouts that occurred
         * during the lookup. In other words, the number of {@link Contact}s
         * from our {@link RouteTable} that failed to respond.
         */
        public int getRouteTableTimeouts() {
            return routeTableTimeouts;
        }
        
        /**
         * Adds the given {@link Contact} and {@link SecurityToken} to
         * the list of nodes that responded to our lookup requests.
         */
        private boolean addToResponses(Contact contact, 
                SecurityToken securityToken) {
            
            if (!responses.containsKey(contact)) {
                responses.put(contact, securityToken);
                closest.add(contact);
                
                if (closest.size() > KademliaSettings.K) {
                    closest.pollLast();
                }
                
                KUID contactId = contact.getContactId();
                currentHop = history.get(contactId);
                return true;
            }
            return false;
        }
        
        /**
         * Add the given {@link Contact} to the to-be-queried list.
         */
        private boolean addToQuery(Contact contact, int hop) {
            KUID contactId = contact.getContactId();
            if (!history.containsKey(contactId)) {
                history.put(contactId, hop);
                query.add(contact);
                return true;
            }
            return false;
        }
        
        /**
         * Returns {@code true} if the given {@link Contact} is closer to 
         * the {@link #lookupId} than our {@link Contact} that is furthest 
         * away from it.
         */
        private boolean isCloserThanClosest(Contact other) {
            if (!closest.isEmpty()) {
                Contact contact = closest.last();
                KUID contactId = contact.getContactId();
                KUID otherId = other.getContactId();
                return otherId.isNearerTo(lookupId, contactId);
            }
            return true;
        }
        
        /**
         * Returns {@code true} if there are {@link Contact}s that
         * are worthwhile to be queried.
         */
        public boolean hasNext() {
            return hasNext(false);
        }
        
        /**
         * Returns {@code true} if there are {@link Contact}s that
         * are worthwhile to be queried.
         */
        public boolean hasNext(boolean force) {
            if (!query.isEmpty()) {
                Contact contact = query.first();
                if (force || closest.size() < KademliaSettings.K
                        || isCloserThanClosest(contact)
                        || exchaustive) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Returns the next {@link Contact} to be queried.
         */
        public Contact next() {
            Contact contact = null;
            
            if (randomize) {
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
    
    /**
     * An implementation of {@link Comparator} that compares 
     * {@link Contact}s by their XOR-distance to a given {@link KUID}.
     */
    private static class XorComparator implements Comparator<Contact> {

        private final KUID key;
        
        public XorComparator(KUID key) {
            this.key = key;
        }
        
        @Override
        public int compare(Contact o1, Contact o2) {
            return o1.getContactId().xor(key)
                    .compareTo(o2.getContactId().xor(key));
        }
    }
    
    /**
     * The final state of the lookup.
     */
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
     * Creates a {@link MaxStack} for the given {@link Type}.
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
