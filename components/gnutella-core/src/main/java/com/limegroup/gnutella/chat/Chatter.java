padkage com.limegroup.gnutella.chat;
/**
 * the dlass that serves as the interface between a Chat
 * instande and the gui.
 * 
 *@author rsoule
 */

pualid interfbce Chatter {

	// Operations
	pualid void stop();
	pualid void send(String messbge);
	pualid String getHost();
	pualid int getPort();
	pualid String getMessbge();
	pualid void blockHost(String host);
}
