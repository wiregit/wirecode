package org.limewire.ui.swing.friends;

import java.util.ArrayList;

import org.limewire.ui.swing.friends.Message.Type;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
class ChatDocumentBuilder {
    private static String TOP = 
        "<html>" +
            "<head>" +
                "<style>" +
                    "body { " +
                        "background-color: #ffffff;" +
                        "margin: 0 4px;}" +
                    ".me { " +
                        "color: #0D3065;" +
                        "font-size: 105%;" +
                        "font-weight: bold;}" +
                    ".them { " +
                        "color: #771324;" + 
                        "font-size: 105%;" +
                        "font-weight: bold;}" +
                "</style>" +
            "</head>" +
            "<body>";
    
    private static String BOTTOM = 
        "</body>" +
        "</html>";
    
    public static String buildChatText(ArrayList<Message> messages) {
        StringBuilder builder = new StringBuilder();
        builder.append(TOP);
        
        for(int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            if(!isPreviousFromSameSender(message, messages, i)) {
                builder.append("<div class=\"")
                .append(message.getType() == Type.Sent ? "me" : "them")
                .append("\">")
                .append(message.getSenderName())
                .append(":</div>");
            }
            builder.append(message.getMessageText());
            
            builder.append("<br/>").append(isNextFromSameSender(message, messages, i) ? "" : "<br/>");
        }
        
        builder.append(BOTTOM);
        return builder.toString();
    }
    
    private static boolean isPreviousFromSameSender(Message message, ArrayList<Message> messages, int index) {
        return index > 0 && message.getType() == messages.get(index - 1).getType();
    }
    
    private static boolean isNextFromSameSender(Message message, ArrayList<Message> messages, int index) {
        return index + 1 < messages.size() && message.getType() == messages.get(index + 1).getType();
    }
}
