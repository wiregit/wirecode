package org.limewire.ui.swing.library.sharing;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ComboBoxEditor;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.ComboPopup;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.SignoffEvent;
import org.limewire.ui.swing.library.Disposable;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.WritableTableFormat;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.EventComboBoxModel;
import ca.odell.glazedlists.swing.EventTableModel;

public class LibrarySharePanel extends JXPanel implements PropertyChangeListener, Disposable {

    private static final int SHARED_ROW_COUNT = 20;
    private static final int BORDER_INSETS = 10;
    private static final int HGAP = 5;
    //TODO: move this to properties
    private static final int WIDTH = 300;
    
    private LibraryShareModel shareModel;
    
    private JTextField inputField;

    /**
     * all unshared friends
     */    
    private EventList<SharingTarget> noShareFriendList;
    /**
     * filtered list of unshared friends
     */
    private FilterList<SharingTarget> noShareFilterList;
    /**
     * list of shared friends
     */
    private EventList<SharingTarget> shareFriendList;
    /**
     * list of all friends
     */
    private List<SharingTarget> allFriends;

    private JScrollPane shareScroll;

    private JXTable shareTable;
    private JComboBox friendCombo;

    private JLabel friendLabel;
    private JLabel shareLabel;
    private JLabel bottomLabel;
    
    private JXPanel mainPanel;
    
   // private ShapePainter shapePainter;    
    
    private JDialog dialog;
    
    private ComboPopup comboPopup;
    
    @Resource
    private Icon removeIcon;
    @Resource
    private Icon removeIconRollover;
    @Resource
    private Icon removeIconPressed;   
    @Resource
    private int shareTableIndent = 15;
    
    private Action up = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if(!friendCombo.isPopupVisible()){
                friendCombo.showPopup();
            } else {
                int selRow = friendCombo.getSelectedIndex();
                if (selRow > 0) {
                    selRow--;
                    friendCombo.setSelectedIndex(selRow);
                }
            }
        }
    };

    private Action down = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if(!friendCombo.isPopupVisible()){
                friendCombo.showPopup();
            } else {
                int selRow = friendCombo.getSelectedIndex();
                if (selRow < noShareFilterList.size() - 1) {
                    selRow++;
                    friendCombo.setSelectedIndex(selRow);
                }
            }
        }
    };
    private AWTEventListener eventListener;
    
    
    public LibrarySharePanel(List<SharingTarget> allFriends) {
        //TODO clean up constructor
        GuiUtils.assignResources(this);
        
        this.allFriends = allFriends;
        
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        
//        shapePainter = new ShapePainter();
//        shapePainter.setFillPaint(getBackground());        
//        shapePainter.setBorderPaint(Color.BLACK);
//        shapePainter.setBorderWidth(2);
//        shapePainter.setHorizontalAlignment(HorizontalAlignment.LEFT);
//        shapePainter.setVerticalAlignment(VerticalAlignment.TOP);
//        setBackgroundPainter(shapePainter);
        setBorder(new LineBorder(Color.BLACK));
        
        mainPanel = new JXPanel(new GridBagLayout());
        mainPanel.setOpaque(false);
      
        
        inputField = new JTextField(12);
        inputField.setBorder(null);
        
        shareLabel = new JLabel(I18n.tr("Currently sharing with"));
        
        shareFriendList = GlazedListsFactory.threadSafeList(GlazedListsFactory.sortedList(new BasicEventList<SharingTarget>(), new SharingTargetComparator()));
       
        shareTable = new ToolTipTable(new EventTableModel<SharingTarget>(shareFriendList, new LibraryShareTableFormat(1)));
        shareTable.setTableHeader(null);
        final ShareRendererEditor removeEditor = new ShareRendererEditor(removeIcon, removeIconRollover, removeIconPressed);
        removeEditor.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
               unshareFriend(removeEditor.getFriend());
               removeEditor.cancelCellEditing();
               inputField.requestFocusInWindow();
            }            
        });
        //do nothing ColorHighlighter eliminates default striping
        shareTable.setHighlighters(new ColorHighlighter());
        shareTable.setRowHeight(removeEditor.getPreferredSize().height);
        shareTable.getColumnModel().getColumn(1).setCellEditor(removeEditor);
        shareTable.getColumnModel().getColumn(1).setPreferredWidth(removeEditor.getPreferredSize().width);    
        shareTable.getColumnModel().getColumn(1).setMaxWidth(removeEditor.getPreferredSize().width);    
        shareTable.getColumnModel().getColumn(1).setCellRenderer(new ShareRendererEditor(removeIcon, removeIconRollover, removeIconPressed));      
        shareTable.setShowGrid(false);       
        shareTable.setOpaque(false);
        shareTable.setColumnSelectionAllowed(false);
        shareTable.setRowSelectionAllowed(false);
  
        
        TextFilterator<SharingTarget> textFilter = new TextFilterator<SharingTarget>() {
            @Override
            public void getFilterStrings(List<String> baseList, SharingTarget element) {
                baseList.add(element.getFriend().getName());
                baseList.add(element.getFriend().getId());
            }
        };
        
        noShareFriendList = GlazedLists.threadSafeList(GlazedListsFactory.sortedList(new BasicEventList<SharingTarget>(), new SharingTargetComparator()));
      //using TextComponentMatcherEditor would cause problems because it also uses DocumentListener so we 
        //have no guarantee about the order of sorting and selecting
        final TextMatcherEditor<SharingTarget>textMatcher = new TextMatcherEditor<SharingTarget>(textFilter);
        noShareFilterList = GlazedListsFactory.filterList(noShareFriendList, textMatcher);

        friendCombo = new JComboBox(new EventComboBoxModel<SharingTarget>(noShareFilterList));
        ShareComboBoxUI comboUI = new ShareComboBoxUI();
        
        friendCombo.setUI(comboUI);
        friendCombo.setEditor(new ShareComboBoxEditor());
        friendCombo.setEditable(true);
        
        comboPopup = comboUI.getPopup();
        

        
        // mouseClicked does not register on the combo box popup so we have to
        // use mousePressed and mouseReleased. (This is a workaround for
        // ActionEvents not being fired when the selected row is clicked)   
        MouseListener shareListener = new MouseAdapter() {
            private int pressedRow;
            @Override
            public void mousePressed(MouseEvent e) {
                pressedRow = comboPopup.getList().locationToIndex(e.getPoint());
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (pressedRow == comboPopup.getList().locationToIndex(e.getPoint())) {
                    shareSelectedFriend();
                }
            }
        };
        
        comboPopup.getList().addMouseListener(shareListener);

              
        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(noShareFilterList.size() == 0 || 
                        !friendCombo.isPopupVisible() && ("".equals(inputField.getText()) || inputField.getText() == null)){
                    dialog.setVisible(false);
                } else if (friendCombo.getSelectedIndex() >= 0) {
                    shareSelectedFriend();
                    friendCombo.hidePopup();
                }
            }
        });
        
        
        inputField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
               update();
            }
            
            private void update() {
                textMatcher.setFilterText(inputField.getText().split(" "));
                if (noShareFilterList.size() > 0) {
                    friendCombo.setSelectedIndex(0);
                    if (isVisible()) {
                        friendCombo.showPopup();
                    }
                }
            }
            
        });

        
        shareScroll = new JScrollPane(shareTable);
        shareScroll.setBorder(new EmptyBorder(0, shareTableIndent, 0, 0));
        shareScroll.setOpaque(false);

        friendLabel = new JLabel(I18n.tr("Start typing a friend's name"));
        shareFriendList.addListEventListener(new ListEventListener<SharingTarget>() {
            @Override
            public void listChanged(ListEvent<SharingTarget> listChanges) {
                adjustFriendLabelVisibility();
            }
        });
        
        Dimension labelSize = friendLabel.getPreferredSize().width > shareLabel.getPreferredSize().width ? 
                friendLabel.getPreferredSize() : shareLabel.getPreferredSize();
        friendLabel.setPreferredSize(labelSize);
        shareLabel.setPreferredSize(labelSize);
        
        bottomLabel = new MultiLineLabel("", WIDTH);
        
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, HGAP, 0, HGAP);
        
        gbc.gridy = 0;
        gbc.weighty = 0;
        mainPanel.add(shareLabel, gbc);
        
        gbc.gridy++;
        gbc.weighty = 1.0;
        mainPanel.add(shareScroll, gbc);
       
        gbc.gridy++;
        mainPanel.add(friendLabel, gbc);
        
        gbc.gridy++;
        gbc.weighty = 1.0;
        mainPanel.add(friendCombo, gbc);

        gbc.gridy++;
        mainPanel.add(bottomLabel, gbc);
        
        
                
        add(mainPanel);

        EventAnnotationProcessor.subscribe(this);   
        
        setKeyStrokes(this);
        setKeyStrokes(mainPanel);
        setKeyStrokes(inputField);
  
     
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (dialog.isVisible()) {
                    dialog.pack();
                }
            }

            @Override
            public void componentShown(ComponentEvent e) {
                inputField.requestFocusInWindow();
            }
        });
        
        // make sharePanel disappear when the user clicks elsewhere
        eventListener = new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (dialog.isVisible()) {
                    
                    if ((event.getID() == MouseEvent.MOUSE_PRESSED)) {
                        MouseEvent e = (MouseEvent) event;
                        
                        if (LibrarySharePanel.this != e.getComponent()
                                && !contains(e.getComponent())) {
                            dialog.setVisible(false);
                        }
                        
                    } else if (event.getID() == KeyEvent.KEY_TYPED) {
                        KeyEvent e = (KeyEvent) event;
                        
                        if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
                            dialog.setVisible(false);
                        }
                    }
                    
                }
            }
        }; 

    }
    
    public void setBottomLabel(String text){
        bottomLabel.setText(text);
    }
    
    private void createDialog(JComponent component){
        dialog = new JDialog((Window)component.getTopLevelAncestor());
        dialog.setUndecorated(true);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                addCloseListener();
            }
        });
        
        dialog.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                removeCloseListener();
            }
        });
        
        dialog.setLayout(new BorderLayout());
        dialog.add(this);
    }

    private void adjustFriendLabelVisibility() {
        friendLabel.setVisible(shareFriendList.size() == 0);
    }
    
    private void shareSelectedFriend() {
        int index = friendCombo.getSelectedIndex();
        if (index > -1) {
            shareFriend(noShareFilterList.get(index));
            if (noShareFilterList.size() == 0) {
                inputField.setText(null);
            }
            resetRowSelection(index);
        }
    }

    public void setShareModel(LibraryShareModel shareModel){
        this.shareModel = shareModel;
        shareModel.addPropertyChangeListener(this);
    }
    
    public LibraryShareModel getShareModel(){
        return shareModel;
    }

    
    private void setKeyStrokes(JComponent component) {
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up");
        component.getActionMap().put("up", up);
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down");
        component.getActionMap().put("down", down);       
    }

    
    public void show(JComponent c) {
        if(dialog == null){
            createDialog(c);
        }
        inputField.setText(null);
        if (noShareFilterList.size() > 0) {
            friendCombo.setSelectedIndex(0);
        }
        reloadSharedBuddies();
        adjustSize();
        dialog.setLocationRelativeTo(c);
        dialog.setVisible(true);
        dialog.toFront();
    }
    
 
//    private void adjustPainter(){
//        Area area = new Area(new RoundRectangle2D.Float(1, 1, mainPanel.getWidth() + HGAP * 2-1, getHeight()-2, BORDER_INSETS, BORDER_INSETS));
//        shapePainter.setShape(area);
//    }


    private void resetRowSelection(int oldSelRow) {
        if(noShareFilterList.size() == 0){
            return;
        }
        
        if(oldSelRow == -1){
            friendCombo.setSelectedIndex(0);
        } else if(noShareFilterList.size() > oldSelRow){
            friendCombo.setSelectedIndex(oldSelRow);
        } else if ((oldSelRow - 1) >= 0 && (oldSelRow - 1) < noShareFilterList.size()) {
            friendCombo.setSelectedIndex(oldSelRow - 1);
        }
    }
    
    
    private void shareFriend(SharingTarget friend) {
        shareFriendList.add(friend);
        noShareFriendList.remove(friend);

        shareModel.shareFriend(friend);
        
        adjustSize();
    }
    
    
    private void unshareFriend(SharingTarget friend) {
        shareFriendList.remove(friend);
        noShareFriendList.add(friend);
        
        shareModel.unshareFriend(friend);
        
        adjustSize();
    }
    
    public void adjustSize(){
        adjustFriendLabelVisibility();
        inputField.setVisible(noShareFriendList.size() > 1);
        
        int visibleRows = (shareTable.getRowCount() < SHARED_ROW_COUNT) ? shareTable.getRowCount() : SHARED_ROW_COUNT;
        shareTable.setVisibleRowCount(visibleRows);
        shareScroll.setVisible(visibleRows > 0);
        shareLabel.setVisible(visibleRows > 0);
        
        int height = 0;
        int prefWidth = 0;
        for(Component c : getComponents()){
            if (c.isVisible()) {
                height += c.getPreferredSize().height;
            }
            prefWidth = Math.max(prefWidth, c.getPreferredSize().width);
        }
        
         mainPanel.setSize(WIDTH, height);
         setSize(mainPanel.getSize().width + 2 * HGAP, mainPanel.getSize().height + BORDER_INSETS);
       // adjustPainter();
    }
    
     
    
    @EventSubscriber
    public void handleSignoff(SignoffEvent event) {
        shareFriendList.clear();
        noShareFriendList.clear();
        allFriends.clear();
    }
    
    
    public boolean contains(Component c) {
        for (; c != null; c = c.getParent()) {
            if (c == this || c == comboPopup) {
                return true;
            }
        }
        return false;
    }
    
    public void dispose() {
        if (noShareFriendList instanceof TransformedList) {
            ((TransformedList) noShareFriendList).dispose();
        }       
        
        ((EventTableModel)shareTable.getModel()).dispose();
    }
    
    
    private void reloadSharedBuddies() {
        shareFriendList.clear();
        noShareFriendList.clear();

        if (shareModel.isGnutellaNetworkSharable()) {
            loadFriend(SharingTarget.GNUTELLA_SHARE);
        }
        
        for (SharingTarget friend : allFriends) {
            loadFriend(friend);
        }
    }
    
    private void loadFriend(SharingTarget friend) {
        if (isShared(friend)) {
            shareFriendList.add(friend);
        } else {
            noShareFriendList.add(friend);
        }
    }
    
    private boolean isShared(SharingTarget friend){
        return shareModel.isShared(friend);
    }   
    
    private void addCloseListener() {
        Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, AWTEvent.MOUSE_EVENT_MASK);
        Toolkit.getDefaultToolkit().addAWTEventListener(eventListener, AWTEvent.KEY_EVENT_MASK);
    }

    private void removeCloseListener() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(eventListener);
    }
            
    private static class LibraryShareTableFormat implements WritableTableFormat<SharingTarget> {
        private int editColumn;
        
        public LibraryShareTableFormat(int editedColumn){
            this.editColumn = editedColumn;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return null;
        }

        @Override
        public Object getColumnValue(SharingTarget baseObject, int column) {
            if(column == editColumn){
                return baseObject;
            }
            return baseObject.getFriend().getRenderName();
        }

        @Override
        public boolean isEditable(SharingTarget baseObject, int column) {
           // if (column == editColumn) {
                return true;
//            }
//
//            return false;
        }

        @Override
        public SharingTarget setColumnValue(SharingTarget baseObject, Object editedValue, int column) {
            return baseObject;
        }

    }
    
    private class SharingTargetComparator implements Comparator<SharingTarget>{

        @Override
        public int compare(SharingTarget o1, SharingTarget o2) {
            if (o1 == o2){
                return 0;
            }
            if(o1.isGnutellaNetwork()){
                return -1;
            }
            if(o2.isGnutellaNetwork()){
                return 1;
            }
            return o1.getFriend().getRenderName().compareTo(o2.getFriend().getRenderName());
        }
        
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // the share model file or category has changed - reload friends.
        reloadSharedBuddies();
    }
    
    
    private class ShareComboBoxEditor implements ComboBoxEditor{
        @Override
        public Component getEditorComponent() {
            return inputField;
        }

        @Override
        public void addActionListener(ActionListener l) {
            //Do nothing
        }       

        @Override
        public Object getItem() {
            //Do nothing
            return null;
        }

        @Override
        public void removeActionListener(ActionListener l) {
            //Do nothing
        }

        @Override
        public void selectAll() {
            //Do nothing
        }

        @Override
        public void setItem(Object anObject) {
            //Do nothing
        }
        
    }
    
    private static class ShareComboBoxUI extends BasicComboBoxUI {
        public ComboPopup getPopup(){
            return popup;
        }
    }

    
}
