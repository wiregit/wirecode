package org.limewire.ui.swing.friends;

import java.util.ArrayList;

import junit.framework.TestCase;

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

    private void compareOutput(String input, String expected) {
        String chatText = ChatDocumentBuilder.buildChatText(getMessages(input));
        
        assertTrue(chatText.startsWith(ChatDocumentBuilder.TOP));
        assertTrue(chatText.endsWith(ChatDocumentBuilder.BOTTOM));
        
        chatText = chatText.replace(ChatDocumentBuilder.TOP, "");
        chatText = chatText.replace(ChatDocumentBuilder.BOTTOM, "");
        
        chatText = chatText.replace("<div class=\"me\">foo:</div>", "");
        chatText = chatText.replace("<br/>", "");
        assertEquals(expected, chatText);
    }
    
    private ArrayList<Message> getMessages(String... messages) {
        ArrayList<Message> list = new ArrayList<Message>();
        for(String message : messages) {
            list.add(new MessageImpl("foo", null, message, Message.Type.Sent));
        }
        return list;
    }
}
