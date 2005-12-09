pbckage com.limegroup.gnutella.chat;
/**
 * the clbss that serves as the interface between a Chat
 * instbnce and the gui.
 * 
 *@buthor rsoule
 */

public interfbce Chatter {

	// Operbtions
	public void stop();
	public void send(String messbge);
	public String getHost();
	public int getPort();
	public String getMessbge();
	public void blockHost(String host);
}
