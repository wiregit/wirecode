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
public final class QueryFactory {
	
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
	private QueryFactory(GUID guid, String query, String xmlQuery) {
		GUID = guid;
		QUERY = query;
		XML_QUERY = xmlQuery;
	}

	public static QueryFactory createFactory(byte[] guid, String query, 
											 String xmlQuery) {
		return new QueryFactory(new GUID(guid), query, xmlQuery);
	}

	/**
	 * Factory method for creating new <tt>QueryRequest</tt> instances with
	 * the same guid, query, xml query, urn types, etc.
	 *
	 * @param ttl the time to live of the new query
	 * @return a new <tt>QueryRequest</tt> instance with all of the 
	 *  pre-defined parameters and the specified TTL
	 */
	public QueryRequest createQuery(byte ttl) {
		return new QueryRequest(GUID.bytes(), ttl, 0, QUERY, XML_QUERY, false, 
								URN_TYPES, QUERY_URNS, null,
								!RouterService.acceptedIncomingConnection());
	}

	public GUID getGUID() {
		return GUID;
	}
}
