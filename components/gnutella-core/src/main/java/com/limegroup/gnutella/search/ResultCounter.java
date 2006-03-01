
// Commented for the Learning branch

package com.limegroup.gnutella.search;

/**
 * Interface for a class that counts the number of results for a given query. 
 * This can easily be used to add a result counting mixin to any class choosing
 * to add this functionality.
 * 
 * Only the nested RouteTable.RouteTableEntry class implements the ResultCounter interface.
 * In the RouteTable class, routeTable.routeReply() and routeTable.tryToRouteReply() return RouteTableEntry objects.
 * They return them cast to the ResultCounter interface, letting code only call getNumResults() on them.
 */
public interface ResultCounter {

	/**
     * Get the number of reply packets with this GUID that we've routed back to this remote computer.
     * 
     * In a RouteTable, message GUIDs map to RouteTableEntry objects.
     * The RouteTableEntry counts this number of reply packets, and keeps the ReplyHandler which made the request.
     * 
     * The nested RouteTableEntry class implements the ResultCounter interface.
     * This lets external code call this getNumResults() method.
     * 
     * @return The number of reply packets we've routed to this remote computer for the request packet with this GUID
	 */
	int getNumResults();
}
