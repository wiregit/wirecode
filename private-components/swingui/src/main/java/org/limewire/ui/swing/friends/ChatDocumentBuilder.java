package org.limewire.ui.swing.friends;

import java.net.URL;
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
                    "body{background-color: #E8E8E8}" +
                    ".me { " +
                        "color: #0D3065;" +
                        "margin: 0 8px;" +
                        "font-weight: bold;}" +
                    ".them { " +
                        "color: #771324;" + 
                        "margin: 0 8px;" +
                        "font-weight: bold;}" +
                    ".roundcont {" +
                        "width: 98%;" +
                        "background-color: #ffffff;" +
                        "color: #000;}" +
                    ".roundcont p {" +
                        "margin: 0 8px;}" +
                    ".roundtop {" + 
                        "background: url(" + getURL("/org/limewire/ui/swing/mainframe/resources/icons/friends/top-right.png") +") no-repeat top right;}" +
                    ".roundbottom {" +
                        "background: url(" + getURL("/org/limewire/ui/swing/mainframe/resources/icons/friends/bottom-right.png") + ") no-repeat top right;}" +
                    "img.corner {" +
                       "border: none;" +
                       "display: block;" +
                    "}" +
                "</style>" +
            "</head>" +
            "<body>";
    
    private static String ROUND_CONTAINER_TOP = 
        "<div class=\"roundcont\">" +
            "<div class=\"roundtop\">" +
              "<img src=\"" + getURL("/org/limewire/ui/swing/mainframe/resources/icons/friends/top-left.png") + "\" class=\"corner\" style=\"display: none\" />" +
            "</div>";
    
    private static String ROUND_CONTAINER_BOTTOM =
            "<div class=\"roundbottom\">" +
              "<img src=\"" + getURL("/org/limewire/ui/swing/mainframe/resources/icons/friends/bottom-left.png") + "\" class=\"corner\" style=\"display: none\" />" +
            "</div>" +
        "</div>" +
        "<br/>";

    private static String BOTTOM = 
        "</body>" +
        "</html>";
    
    private static String getURL(String path) {
        URL resource = ChatDocumentBuilder.class.getResource(path);
        return resource != null ? resource.toExternalForm() : "";
    }
    
    public static String buildChatText(ArrayList<Message> messages) {
        StringBuilder builder = new StringBuilder();
        builder.append(TOP);
        
        for(int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            if(!isPreviousFromSameSender(message, messages, i)) {
                builder.append(ROUND_CONTAINER_TOP);
                builder.append("<div class=\"")
                .append(message.getType() == Type.Sent ? "me" : "them")
                .append("\">")
                .append(message.getSenderName())
                .append(":</div>");
            }
            builder.append("<p>")
            .append(message.getMessageText())
            .append("</p>");
            
            builder.append(isNextFromSameSender(message, messages, i) ? "" : ROUND_CONTAINER_BOTTOM);
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
