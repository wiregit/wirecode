package org.limewire.util;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;

public class PrivateGroupsUtils {
    public PrivateGroupsUtils(){
        
    }

   
    public static Message createMessage(String username, String body){
        Message msg = new Message();
        msg.setFrom(username);
        msg.setLanguage("English");
        msg.setSubject("message");
        msg.setType(Type.normal);
        msg.setBody(body);
        return msg;
    }

}
