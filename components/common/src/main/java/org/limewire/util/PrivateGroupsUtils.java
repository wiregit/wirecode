package org.limewire.util;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;

/**
 * A class of utility methods for Private Groups
 */
public class PrivateGroupsUtils {
    private PrivateGroupsUtils(){
        
    }

    // a method to automatically create a message object given a username, and the message body
    public static Message createMessage(String localUsername, String remoteuser, String body){
        Message msg = new Message();
        msg.setFrom(localUsername);
        msg.setTo(remoteuser);
        msg.setLanguage("English");
        msg.setSubject("message");
        msg.setType(Type.normal);
        
        if (body!=null)
            msg.setBody(body);
        else{
            //no message supplied by user
            msg.setBody("");
        }
        return msg;
    }
}
