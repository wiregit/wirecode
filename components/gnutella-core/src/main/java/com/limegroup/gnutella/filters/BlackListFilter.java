package com.limegroup.gnutella.filters;

import java.util.*;
import com.limegroup.gnutella.*;

/**This class provides an interface to the list of ip addresses that
 * have been black listed. 
 *There are two ways for an IP address to the added to the blacklisted 
 *list. One - directly by the user, through the GUI. Second, the anti spam 
 *stuff Chris is writing can update the list.
 * @ author Sumeet Thadani
 */
class BlackListFilter extends SpamFilter{
    
    private Vector badHosts = new Vector();
    private ConnectionManager cm;
    private static BlackListFilter instance;
    private String allHosts;

    //constructors
    protected BlackListFilter(){
	allHosts = SettingsManager.instance().getBannedIps();
	StringTokenizer st = new StringTokenizer(allHosts,";");
	while(st.hasMoreTokens()){
	    badHosts.add(st.nextToken());
	}
    }
    
    /** To ensure the singleton pattern
     */
    public static BlackListFilter instance(){
	if (instance == null)
	    instance = new BlackListFilter();
	return instance;
    }
    
    /** Adds a bad host to the bad hosts list if it was not already there */
    public void add(String badGuy){
	if (badHosts.contains(badGuy))
	    return; // no duplicates please
	badHosts.add(badGuy);
	if (allHosts.equals(""))
	    allHosts = ""+badGuy;
	else
	    allHosts = allHosts+";"+badGuy;
	SettingsManager.instance().setBannedIps(allHosts);//write to disk
    }
    
    /** Removes a good host from the bad hosts list */
    public void delete(String goodGuy){
	String newAllHosts= "";
	if (!(badHosts.contains(goodGuy)))
	    return; // no need to remove somthing thats not there
	badHosts.removeElement(goodGuy);
	//resonctruct the list
	for(int i =0; i< badHosts.size();i++){
	    newAllHosts = (String)badHosts.get(i);
	    newAllHosts = newAllHosts + ";";
	}
	allHosts = newAllHosts;
	SettingsManager.instance().setBannedIps(allHosts);
    }
    
    /** Checks if a given host is in the the bad hosts list 
     *  This method will be called when accepting an incomming 
     *  or outgoing connection. It returns true if the given host is
     *  a bad guy
     */
    public boolean check(String host){
	if (badHosts.contains(host))
	    return true;
	return false;
    }

    /** This method will be called from Connection, when Query Replies or Ping Replies come in
     *  We cannot findout about host ip addresses in Ping Requests and 	Query Requests 
     * (for anonymity reasons)
     */
    public boolean allow(Message m){
	String ip;
	if( (m instanceof PingReply)){
	    PingReply pr = (PingReply)m;
	    ip = pr.getIP();
	}
	else if ( (m instanceof QueryReply) ){
	    QueryReply qr = (QueryReply)m;
	    ip = qr.getIP();
	}
	else // we dont want to block other kinds of messages
	    return true;
	// now check the ip
	if (badHosts.contains(ip))
	    return false;
	return true;
    }
    
    /*
    //unit tests
    public static void main(String args[]){
	BlackListFilter fil = BlackListFilter.instance(new ConnectionManager());
	System.out.println("value "+ fil.check("192.192.192.197"));
    }
    */
}
