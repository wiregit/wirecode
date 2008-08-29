package org.limewire.ui.swing.friends;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.limewire.ui.swing.friends.Message.Type;

public class ChatDocumentBuilderTest extends TestCase {
    public void testBuildChatTextWithLinks() {
        compareOutput("hey http://www.foo.com there",
                      "hey <a href=\"http://www.foo.com\">http://www.foo.com</a> there");
        
        compareOutput("hey foo.com there", 
                      "hey <a href=\"http://foo.com\">foo.com</a> there");
        
        compareOutput("hey www.foo.com there", 
                      "hey <a href=\"http://www.foo.com\">www.foo.com</a> there");

        compareOutput("hey https://www.foo.com there", 
                      "hey <a href=\"https://www.foo.com\">https://www.foo.com</a> there");

        compareOutput("hey www.foo there", 
                      "hey www.foo there");
    }
    
    public void testBuildChatTextWithMarkup() {
        compareOutput("hey <b>foo</b> there", 
                      "hey &lt;b&gt;foo&lt;/b&gt; there");
    }
    
    public void testBuildChatTextForNullMessage() {
        compareOutput(null, "<div class=\"typing\">foo is typing a message...</div>", Message.Type.Typing);
    }

    private void compareOutput(String input, String expected) {
        compareOutput(input, expected, Message.Type.Sent);
    }

    private void compareOutput(String input, String expected, Type type) {
        String chatText = ChatDocumentBuilder.buildChatText(getMessages(type, input));
        
        assertTrue(chatText.startsWith(ChatDocumentBuilder.TOP));
        assertTrue(chatText.endsWith(ChatDocumentBuilder.BOTTOM));
        
        chatText = chatText.replace(ChatDocumentBuilder.TOP, "");
        chatText = chatText.replace(ChatDocumentBuilder.BOTTOM, "");
        
        chatText = chatText.replace("<div class=\"me\">foo:</div>", "");
        chatText = chatText.replace("<br/>", "");
        assertEquals(expected, chatText);
    }
    
    private ArrayList<Message> getMessages(Type type, String... messages) {
        ArrayList<Message> list = new ArrayList<Message>();
        for(String message : messages) {
            list.add(new MessageImpl("foo", null, message, type));
        }
        return list;
    }
}
