package com.limegroup.gnutella.chat;
/**
 * this class is the superclass for all possible types
 * of chat.
 * 
 *@author rsoule
 */

import com.limegroup.gnutella.*;

public abstract class Chat implements Chatter {

	// Attributes
	protected ActivityCallback _activityCallback;
	protected ChatManager      _manager;

}
