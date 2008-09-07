package org.limewire.ui.swing.friends;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.limewire.ui.swing.friends.Message.Type;
import org.limewire.xmpp.api.client.ChatState;

public class ChatDocumentBuilderTest extends TestCase {
    public void testBuildChatTextWithLinks() {
        compareOutput("hey http://www.foo.com there",
                      "<div class=\"me\">me:</div>hey <a href=\"http://www.foo.com\">http://www.foo.com</a> there<br/>");
        
        compareOutput("hey foo.com there", 
                      "<div class=\"me\">me:</div>hey <a href=\"http://foo.com\">foo.com</a> there<br/>");
        
        compareOutput("hey www.foo.com there", 
                      "<div class=\"me\">me:</div>hey <a href=\"http://www.foo.com\">www.foo.com</a> there<br/>");

        compareOutput("hey https://www.foo.com there", 
                      "<div class=\"me\">me:</div>hey <a href=\"https://www.foo.com\">https://www.foo.com</a> there<br/>");

        compareOutput("hey www.foo there", 
                      "<div class=\"me\">me:</div>hey www.foo there<br/>");
    }
    
    public void testBuildChatTextWithMarkup() {
        compareOutput("hey <b>foo</b> there", 
                      "<div class=\"me\">me:</div>hey &lt;b&gt;foo&lt;/b&gt; there<br/>");
    }
    
    public void testBuildChatTextForRealMessage() {
        compareOutput("<div class=\"them\">you:</div>hey there<br/>", ChatState.active, 
                new Type[] {Type.Received}, 
                new String[] {"hey there"});
    }

    public void testBuildChatTextForExternallyInitiatedConversationWhenTheyAreTyping() {
        StringBuilder conversation = new StringBuilder();
        conversation.append("<div class=\"them\">you:</div>heynow<br/>")
                    .append("<div class=\"typing\">you is typing a message...</div><br/>");
        compareOutput(conversation.toString(), ChatState.composing, 
                new Type[] {Type.Received},  
                new String[] {"heynow"});
    }

    public void testBuildChatTextForExternallyInitiatedConversationWhenTheyArePaused() {
        StringBuilder conversation = new StringBuilder();
        conversation.append("<div class=\"them\">you:</div>heynow<br/>")
        .append("<div class=\"typing\">you has entered text</div><br/>");
        compareOutput(conversation.toString(), ChatState.paused, 
                new Type[] {Type.Received},  
                new String[] {"heynow"});
    }

    public void testBuildChatTextShowingTypingMessagesRemovedAfterReceivingFollowupMessage() {
        StringBuilder conversation = new StringBuilder();
        conversation.append("<div class=\"them\">you:</div>heynow<br/>")
                    .append("foobar<br/>");
        compareOutput(conversation.toString(), ChatState.active,
                new Type[] {Type.Received, Type.Received},  
                new String[] {"heynow", "foobar"});
    }
    
    public void testBuildChatTextForATwoPartyExchangeThatIsExternallyInitiated() {
        StringBuilder conversation = new StringBuilder();
        conversation.append("<div class=\"them\">you:</div>heynow<br/><br/>")
                    .append("<div class=\"me\">me:</div>yo<br/><br/>")
                    .append("<div class=\"them\">you:</div>fooey<br/>");
        
        compareOutput(conversation.toString(), ChatState.active, 
                new Type[] {Type.Received, Type.Sent, Type.Received}, 
                new String[] {"heynow", "yo", "fooey"});
    }

    public void testBuildChatTextForMessagesISendMoreThan60SecondsApart() {
        StringBuilder conversation = new StringBuilder();
        conversation.append("<div class=\"me\">me:</div>heynow<br/><br/>")
        .append("<div class=\"me\">me:</div>yo<br/>");
        
        List<Message> messages = Arrays.asList(new Message[] {
          new MockMessage(null, "heynow", 0, "me", Type.Sent),      
          new MockMessage(null, "yo", 600001, "me", Type.Sent),      
        });
        compareOutput(conversation.toString(), null, new ArrayList<Message>(messages));
    }

    private void compareOutput(String input, String expected) {
        compareOutput(expected, ChatState.active, new Type[] {Type.Sent}, input);
    }

    private void compareOutput(String expected, ChatState state, Type[] type, String... input) {
        compareOutput(expected, state, getMessages(type, input));
    }

    private void compareOutput(String expected, ChatState state, ArrayList<Message> messages) {
        String chatText = ChatDocumentBuilder.buildChatText(messages, state);
        
        assertTrue(chatText.startsWith(ChatDocumentBuilder.TOP));
        assertTrue(chatText.endsWith(ChatDocumentBuilder.BOTTOM));
        
        chatText = chatText.replace(ChatDocumentBuilder.TOP, "");
        chatText = chatText.replace(ChatDocumentBuilder.BOTTOM, "");
        
        assertEquals(expected, chatText);
    }
    
    private ArrayList<Message> getMessages(Type types[], String... messages) {
        ArrayList<Message> list = new ArrayList<Message>();
        for(int i = 0; i < messages.length; i++) {
            Type type = types[i];
            list.add(new MessageImpl(type == Type.Sent ? "me" : "you", null, messages[i], type));
        }
        return list;
    }
}
