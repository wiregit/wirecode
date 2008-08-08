package org.limewire.ui.swing.friends;

import java.net.URL;
import java.util.List;

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
    
    public static String buildChatText(List<Message> messages) {
        StringBuilder builder = new StringBuilder();
        builder.append(TOP);
        
        for(Message message : messages) {
            builder.append(ROUND_CONTAINER_TOP)
                .append("<div class=\"")
                .append(message.getType() == Type.Sent ? "me" : "them")
                .append("\">")
                .append(message.getSenderName())
                .append(":</div><p>")
                .append(message.getMessageText())
                .append("</p>")
                .append(ROUND_CONTAINER_BOTTOM);
        }
        
        builder.append(BOTTOM);
        return builder.toString();
    }
}
