package org.limewire.ui.swing.library.sharing;

import java.awt.CardLayout;
import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class LibrarySharingPanel {

    @Resource private Color backgroundColor;
    @Resource private Color borderColor;
    
    private static final String LOGIN_VIEW = "LOGIN_VIEW";
    private static final String EDITABLE_VIEW = "EDITABLE_VIEW";
    private static final String NONEDITABLE_VIEW = "NONEDITABLE_VIEW";
    
    private final Provider<LibrarySharingLoginPanel> loginPanel;
    private final Provider<LibrarySharingNonEditablePanel> nonEditablePanel;
    private final Provider<LibrarySharingEditablePanel> editablePanel;
    
    private final JPanel component;
    
    private final CardLayout layout = new CardLayout();
    
    private final Map<String, JComponent> layoutMap = new HashMap<String, JComponent>();
    
    private SharedFileList currentList;
    
    @Inject
    public LibrarySharingPanel(Provider<LibrarySharingLoginPanel> loginPanel,
            Provider<LibrarySharingNonEditablePanel> nonEditablePanel,
            Provider<LibrarySharingEditablePanel> editablePanel) {
        this.loginPanel = loginPanel;
        this.nonEditablePanel = nonEditablePanel;
        this.editablePanel = editablePanel;
                
        GuiUtils.assignResources(this);
        
        component = new JPanel();
        component.setBackground(backgroundColor);
        
        component.setBorder(BorderFactory.createMatteBorder(0,0,0,1, borderColor));
               
        component.setLayout(layout);
    }
    
    public void showLoginView() {
        if(!layoutMap.containsKey(LOGIN_VIEW)) {
            JComponent newComponent = loginPanel.get().getComponent();
            component.add(newComponent, LOGIN_VIEW);
            layoutMap.put(LOGIN_VIEW, newComponent);
        }         
        layout.show(component, LOGIN_VIEW);
    }
    
    public void showEditableView() {
        if(!layoutMap.containsKey(EDITABLE_VIEW)) {
            JComponent newComponent = editablePanel.get().getComponent();
            component.add(newComponent, EDITABLE_VIEW);
            layoutMap.put(EDITABLE_VIEW, newComponent);
        }         
        editablePanel.get().updateTableModel(Collections.unmodifiableList(currentList.getFriendIds()));
        layout.show(component, EDITABLE_VIEW);
    }
    
    public void showNonEditableView() {
        if(!layoutMap.containsKey(NONEDITABLE_VIEW)) {
            JComponent newComponent = nonEditablePanel.get().getComponent();
            component.add(newComponent, NONEDITABLE_VIEW);
            layoutMap.put(NONEDITABLE_VIEW, newComponent);
        }         
        nonEditablePanel.get().setSharedFileList(currentList);
        layout.show(component, NONEDITABLE_VIEW);
    }
    
    public void updateFriends(List<String> friends) {
        List<String> currentFriends = Collections.unmodifiableList(currentList.getFriendIds());
        for(String id : currentFriends) {
            if(!friends.contains(id))
                currentList.removeFriend(id);
        }
        for(String id: friends) {
            if(!currentList.getFriendIds().contains(id))
                currentList.addFriend(id);
        }
    }
    
    public void setSharedFileList(SharedFileList currentFileList) {
        if(this.currentList == currentFileList)
            return;
        this.currentList = currentFileList;
//        showLoginView();
        showNonEditableView();
    }
    
    public JComponent getComponent() {
        return component;
    }
}
