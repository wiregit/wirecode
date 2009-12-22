package org.limewire.ui.swing.search.store;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.cookie.Cookie;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.browser.LimeDomListener;
import org.mozilla.browser.MozillaPanel;
import org.mozilla.dom.NodeFactory;
import org.mozilla.interfaces.nsIDOMEvent;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Listener to handle DOM events on the MozSwing browser.
 */
public class StoreDomListener extends LimeDomListener {

    private static final Log LOG = LogFactory.getLog(StoreDomListener.class);
    
    private final MozillaPanel browserPanel;
    
    private final Map<String, ClickAction> clickActions = 
        new ConcurrentHashMap<String, ClickAction>();
    
    /**
     * Constructs a StoreDomListener for the specified browser panel.
     */
    public StoreDomListener(MozillaPanel browserPanel) {
        this.browserPanel = browserPanel;
    }
    
    /**
     * Associates the specified click action with the specified HTML element 
     * name.
     */
    public void addClickListener(String name, ClickAction clickAction) {
        clickActions.put(name, clickAction);
    }
    
    /**
     * Removes the click action associated with the specified HTML element 
     * name.
     */
    public void removeClickListener(String name) {
        clickActions.remove(name);
    }
    
    /**
     * Handles the specified DOM event.  This method is usually invoked on the
     * Mozilla thread, not the Swing event dispatch thread. 
     */
    @Override
    public void handleEvent(nsIDOMEvent event) {
        // Get event type.
        String type = event.getType();
        
        if ("click".equals(type)) {
            // Get node for click event.
            Node node = NodeFactory.getNodeInstance(event.getTarget());
            LOG.debugf("DOM click on element {0} = {1}", node.getNodeName(), node.getNodeValue());
            
            // Forward click event on HTML element to associated action if
            // available.  The action is registered on the name attribute.
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Node nameNode = getAttribute(node, "name");
                if (nameNode != null) {
                    ClickAction clickAction = clickActions.get(nameNode.getNodeValue());
                    if (clickAction != null) {
                        clickAction.nodeClicked(node);
                        return;
                    }
                }
            }
            
            // Forward click events to superclass.  This handles clicks on
            // anchor (hyperlink) tags.
            super.handleEvent(event);
            
        }
    }
    
//    // TODO REMOVE
//    private void printCookies(String url, List<Cookie> cookieList) {
//        System.out.println("cookies=" + cookieList.size() + ", url=" + url);
//        for (Cookie cookie : cookieList) {
//            System.out.println("-> " + cookie);
//        }
//    }
    
    /**
     * Returns an attribute Node for the specified element node and attribute
     * key.
     */
    private Node getAttribute(Node node, String attribute) {
        NamedNodeMap map = node.getAttributes();
        if (map != null) {
            return map.getNamedItem(attribute);
        }
        return null;
    }
    
    /**
     * Defines a listener for click events on an HTML element.
     */
    public interface ClickAction {
        
        /** Invoked when the specified HTML node is clicked. */
        void nodeClicked(Node node);
    }
    
    /**
     * Defines a listener for load events with cookies.
     */
    public interface LoadCookieAction {
        
        /** Returns domain fragment for valid cookies. */
        String getDomain();
        
        /** Returns true if specified url is valid for cookie retrieval. */
        boolean isUrlValid(String url);
        
        /** Invoked when the specified cookie list is loaded. */
        void cookiesLoaded(List<Cookie> cookieList);
    }
}
