package com.limegroup.gnutella.search;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;

/**
 * This class is a factory for creating <tt>QueryRequest</tt> instances
 * for dynamic queries.  Dynamic queries adjust to the varying conditions of
 * a query, such as the number of results received, the number of nodes
 * hit or theoretically hit, etc.  This class makes it convenient to 
 * rapidly generate <tt>QueryRequest</tt>s with similar characteristics, 
 * such as guids, the query itself, the xml query, etc, but with customized
 * settings, such as the TTL.
 */
public final class QueryHandler {
	
	/**
	 * Constant for the query quid.
	 */
	private final GUID GUID;

	/**
	 * Constamnt for the query string.
	 */
	private final String QUERY;

	/**
	 * Constant for the xml query string.
	 */
	private final String XML_QUERY;

	/**
	 * Constant for the types of urns to request -- any type for these
	 * queries.
	 */
	private static final Set URN_TYPES;

	/**
	 * Constant for the urns to request -- an empty set for the new
	 * queries issued by this factory.
	 */
	private static final Set QUERY_URNS;

	/**
	 * Constant handle to the <tt>MessageRouter</tt> instance.
	 */
	private static final MessageRouter MESSAGE_ROUTER =
		RouterService.getMessageRouter();

	/**
	 * Constant handle to the <tt>ConnectionManager</tt> instance.
	 */
	private static final ConnectionManager CONNECTION_MANAGER =
		RouterService.getConnectionManager();;

	/**
	 * Variable for the number of hosts that have been queried.
	 */
	private int _hostsQueried = 0;

	/**
	 * Variable for the next time after which a query should be sent.
	 */
	private long _nextQueryTime = 0;

	/**
	 * Variable for the <tt>RouteTableEntry</tt> for this query -- used
	 * to access the number of replies returned.
	 */
	private RouteTable.RouteTableEntry _routeTableEntry;

	// statically initialize an unmodifiable set of urn types so that
	// they'll be available for all instances
	static {
		Set urnTypes  = new HashSet();
		urnTypes.add(UrnType.ANY_TYPE);
		URN_TYPES = Collections.unmodifiableSet(urnTypes);
		QUERY_URNS = Collections.unmodifiableSet(new HashSet());
	}

	/**
	 * Private constructor to ensure that only this class creates new
	 * <tt>QueryFactory</tt> instances.
	 */
	private QueryHandler(GUID guid, String query, String xmlQuery) {
		GUID = guid;
		QUERY = query;
		XML_QUERY = xmlQuery;
	}

	/**
	 * Factory constructor for generating a new <tt>QueryHandler</tt> 
	 * for the given set of query criteria.
	 *
	 * @param guid the guid for all <tt>QueryRequest</tt> instances  
	 *  generated from this query handler
	 * @param query the query string
	 * @param xmlQuery the xml query string
	 */
	public static QueryHandler createHandler(byte[] guid, String query, 
											 String xmlQuery) {
		return new QueryHandler(new GUID(guid), query, xmlQuery);
	}

	/**
	 * Factory method for creating new <tt>QueryRequest</tt> instances with
	 * the same guid, query, xml query, urn types, etc.
	 *
	 * @param ttl the time to live of the new query
	 * @return a new <tt>QueryRequest</tt> instance with all of the 
	 *  pre-defined parameters and the specified TTL
	 * @throw <tt>IllegalArgumentException</tt> if the ttl is not within
	 *  what is considered reasonable bounds
	 */
	public QueryRequest createQuery(byte ttl) {
		if(ttl < 1 || ttl > 8) 
			throw new IllegalArgumentException("ttl too high: "+ttl);
		return new QueryRequest(GUID.bytes(), ttl, 0, QUERY, XML_QUERY, false, 
								URN_TYPES, QUERY_URNS, null,
								!RouterService.acceptedIncomingConnection());
	}

	/**
	 * Accessor for the <tt>GUID</tt> for this query.
	 *
	 * @return the <tt>GUID</tt> for this query
	 */
	public GUID getGUID() {
		return GUID;
	}

	/**
	 * Returns whether or not this query should be executed given the current
	 * system time.
	 *
	 * @param sysTime the "current" system time
	 * @return <tt>true</tt> if the query should be executed, <tt>false</tt>
	 *  otherwise
	 */
	//public boolean shouldQuery(long sysTime) {
	//if(sysTime > _nextQueryTime) return true;
	//}

	/**
	 * Sets the <tt>RouteTableEntry</tt> for this query.
	 *
	 * @param entry the <tt>RouteTableEntry</tt> to add
	 */
	public void setRouteTableEntry(RouteTable.RouteTableEntry entry) {
		if(entry == null) {
			throw new NullPointerException("null route table entry");
		}
		_routeTableEntry = entry;
	}
	
	/**
	 * Sends the query to the current connections.  If the query is not
	 * yet ready to be processed, this returns immediately.
	 *
	 * @throws <tt>NullPointerException</tt> if the route table entry
	 *  is <tt>null</tt>
	 */
	public void sendQuery() {
		// do not allow the route table entry to be null
		if(_routeTableEntry == null) {
			throw new NullPointerException("null route table entry");
		}
		if(hasEnoughResults()) return;
		if(System.currentTimeMillis() < _nextQueryTime) return;
		List list = CONNECTION_MANAGER.getInitializedConnections2();
		int length = list.size();
		for(int i=0; i<length; i++) {
			ManagedConnection mc = (ManagedConnection)list.get(i);			
			QueryRequest query = createQuery(i, length-i);
			MESSAGE_ROUTER.sendQueryRequest(query, mc, null);
			_nextQueryTime = System.currentTimeMillis() + 6*1000;
		}		
	}
	
	/**
	 * Returns whether or not this query has received enough results.
	 *
	 * @return <tt>true</tt> if this query has received enough results,
	 *  <tt>false</tt> otherwise
	 */
	public boolean hasEnoughResults() {
		return _routeTableEntry.getRepliesRouted() >= 300;
	}

	/**
	 * Creates a new <tt>QueryRequest</tt> instsnce, dynamically setting the
	 * TTL based on a variety of factors. 
	 *
	 * @param queriedConnections the number of connections that have already
	 *  been queried
	 * @param qh the <tt>QueryHandler</tt> instance that generates
	 *  queries for this dynamic query 
	 * @param remainingConnections the number of connections that have yet
	 *  to be queried
	 * @param results the number of results received so far for this query
	 */
	private QueryRequest createQuery(int queriedConnections, 
									 int remainingConnections) {
		//System.out.println("MessageRouter::createQuery::results: "+results); 
		//System.out.println("MessageRouter::createQuery::remaining connections: "+
		//				   remainingConnections); 
		//System.out.println(); 
		//try {
		//int sleepTime = 16000 - queriedConnections*1000;
		//sleepTime = Math.max(sleepTime, 1000);
		//Thread.sleep(sleepTime);
		//} catch(InterruptedException e) {
		//e.printStackTrace();
		//}
		return createQuery((byte)2);		
	}
}
