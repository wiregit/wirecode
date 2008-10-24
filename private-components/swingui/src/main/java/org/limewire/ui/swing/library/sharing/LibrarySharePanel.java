package org.limewire.ui.swing.library.sharing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.painter.ShapePainter;
import org.jdesktop.swingx.painter.AbstractLayoutPainter.HorizontalAlignment;
import org.jdesktop.swingx.painter.AbstractLayoutPainter.VerticalAlignment;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.settings.SharingSettings;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.RoundedBorder;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.SignoffEvent;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.gui.WritableTableFormat;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.EventTableModel;

import com.google.inject.Inject;

public class LibrarySharePanel extends JXPanel implements RegisteringEventListener<RosterEvent>{



    private static final int FRIEND_ROW_COUNT = 4;
    private static final int SHARED_ROW_COUNT = 20;
    private static final int BORDER_INSETS = 10;
    private static final int HGAP = 5;
    
    
    private  final SharingTarget GNUTELLA_SHARE = new SharingTarget(new Gnutella());

    private JTextField inputField;

    /**
     * all unshared friends
     */    
    private EventList<SharingTarget> noShareFriendList;
    /**
     * filtered list of unshared friends
     */
    private FilterList<SharingTarget> noShareFilterList;
    private EventList<SharingTarget> shareFriendList;

    private JScrollPane shareScroll;
    private JScrollPane friendScroll;

    private JXTable shareTable;
    private MouseableTable friendTable;

    private JLabel friendLabel;
    private JLabel shareLabel;
    
    private JXPanel mainPanel;
    
    private ShareListManager libraryManager;
    
    private LocalFileList gnutellaList;
    
    private ShapePainter shapePainter;
    
    private Map<SharingTarget, LocalFileList> friendListMap;
    private LocalFileItem fileItem;
    
    private int ledgeWidth;
    private int ledgeHeight;
    private int ledgeY;
    
    @Resource
    private Icon removeIcon;
    @Resource
    private Icon removeIconRollover;
    @Resource
    private Icon removeIconPressed;
    @Resource
    private Icon addIcon;
    @Resource
    private Icon addIconRollover;
    @Resource
    private Icon addIconPressed;
    
    @Resource
    private int shareTableIndent = 15;
    
    private Action up = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            int selRow = friendTable.getSelectedRow();
            if (selRow > 0) {
                selRow--;
                friendTable.setRowSelectionInterval(selRow, selRow);
            }
        }
    };

    private Action down = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            int selRow = friendTable.getSelectedRow();
            if (selRow < friendTable.getRowCount() - 1) {
                selRow++;
                friendTable.setRowSelectionInterval(selRow, selRow);
            }
        }
    };
    
    
    @Inject
    public LibrarySharePanel(ShareListManager libraryManager) {
        GuiUtils.assignResources(this);
        
        
        this.libraryManager = libraryManager;
        friendListMap = new ConcurrentHashMap<SharingTarget, LocalFileList>();   
        gnutellaList = libraryManager.getGnutellaShareList();
        
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        
        shapePainter = new ShapePainter();
        shapePainter.setFillPaint(getBackground());        
        shapePainter.setBorderPaint(Color.BLACK);
        shapePainter.setBorderWidth(2);
        shapePainter.setHorizontalAlignment(HorizontalAlignment.LEFT);
        shapePainter.setVerticalAlignment(VerticalAlignment.TOP);
        setBackgroundPainter(shapePainter);
        
        mainPanel = new JXPanel(new GridBagLayout());
        mainPanel.setOpaque(false);
      
        
        inputField = new JTextField(12);
        inputField.setBorder(new RoundedBorder(5));
        
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
        
        //using TextComponentMatcherEditor would cause problems because it also uses DocumentListener so we 
        //have no guarantee about the order of sorting and selecting
        final TextMatcherEditor<SharingTarget>textMatcher = new TextMatcherEditor<SharingTarget>(textFilter);
        noShareFriendList = GlazedLists.threadSafeList(GlazedListsFactory.sortedList(new BasicEventList<SharingTarget>(), new SharingTargetComparator()));
        noShareFilterList = GlazedListsFactory.filterList(noShareFriendList, textMatcher);
        
        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if("".equals(inputField.getText()) || inputField.getText() == null || friendTable.getRowCount() == 0){
                    setVisible(false);
                } else if (friendTable.getSelectedRow() >= 0) {
                    shareFriend(noShareFilterList.get(friendTable.getSelectedRow()));
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
            
            private void update(){
                textMatcher.setFilterText(inputField.getText().split(" "));
                if (friendTable.getRowCount() > 0){
                    friendTable.setRowSelectionInterval(0, 0);
                }
            }
        });
        
        friendTable = new ToolTipTable(new EventTableModel<SharingTarget>(noShareFilterList, new LibraryShareTableFormat(0)));
        //do nothing ColorHighlighter eliminates default striping
        friendTable.setHighlighters(new ColorHighlighter(HighlightPredicate.ALWAYS, getBackground(),
                Color.BLACK, friendTable.getSelectionBackground(), friendTable.getSelectionForeground()));
        friendTable.setTableHeader(null);
        friendTable.setOpaque(false);
        friendTable.setShowGrid(false, false);
        friendTable.setColumnSelectionAllowed(false);
        friendTable.setRowSelectionAllowed(true);
        friendTable.setToolTipText("filler to enable tooltips");

        final ShareRendererEditor addEditor = new ShareRendererEditor(addIcon, addIconRollover, addIconPressed);
        addEditor.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
               int row = friendTable.getSelectedRow();
               shareFriend(addEditor.getFriend());
               addEditor.cancelCellEditing();
               inputField.requestFocusInWindow();
               resetRowSelection(friendTable, row);
            }            
        });
        
        friendTable.setDoubleClickHandler(new TableDoubleClickHandler() {
            @Override
            public void handleDoubleClick(int row) {
                addSelectedFriend();
            }
        });

        friendTable.getColumnModel().getColumn(0).setCellEditor(addEditor);
        friendTable.getColumnModel().getColumn(0).setPreferredWidth(addEditor.getPreferredSize().width); 
        friendTable.getColumnModel().getColumn(0).setMaxWidth(addEditor.getPreferredSize().width);       
        friendTable.getColumnModel().getColumn(0).setCellRenderer(new ShareRendererEditor(addIcon, addIconRollover, addIconPressed));

        friendTable.setRowHeight(addEditor.getPreferredSize().height);
        friendTable.setVisibleRowCount(FRIEND_ROW_COUNT);
        friendScroll = new JScrollPane(friendTable);      
        friendScroll.setBorder(new RoundedBorder(5));
        
        shareScroll = new JScrollPane(shareTable);
        shareScroll.setBorder(new EmptyBorder(0, shareTableIndent, 0, 0));
        shareScroll.setOpaque(false);

        friendLabel = new JLabel(I18n.tr("To share, type name below"));
        
        Dimension labelSize = friendLabel.getPreferredSize().width > shareLabel.getPreferredSize().width ? 
                friendLabel.getPreferredSize() : shareLabel.getPreferredSize();
        friendLabel.setPreferredSize(labelSize);
        shareLabel.setPreferredSize(labelSize);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, HGAP, 0, HGAP);
        mainPanel.add(friendLabel, gbc);
        
        gbc.gridy++;
        gbc.weighty = 0;
        mainPanel.add(inputField, gbc);
        gbc.gridy++;
        gbc.weighty = 1.0;
        mainPanel.add(friendScroll, gbc);
        gbc.gridy++;
        gbc.weighty = 0;
        mainPanel.add(new JSeparator(), gbc);   
        gbc.gridy++;
        gbc.weighty = 0;
        mainPanel.add(shareLabel, gbc);
        gbc.gridy++;
        gbc.weighty = 1.0;
        mainPanel.add(shareScroll, gbc);
                
        add(mainPanel);

        adjustSize();

        EventAnnotationProcessor.subscribe(this);   
        
        setKeyStrokes(this);
        setKeyStrokes(mainPanel);
        setKeyStrokes(inputField);
        
        
         Action enterAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addSelectedFriend();
            }
        };
        
        friendTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0),enterAction.toString() );
        friendTable.getActionMap().put(enterAction.toString(), enterAction);        
        
        Action closeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };

        

        this.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),closeAction.toString() );
        this.getActionMap().put(closeAction.toString(), closeAction);  

        mainPanel.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),closeAction.toString() );
        mainPanel.getActionMap().put(closeAction.toString(), closeAction);  
        
        friendTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),closeAction.toString() );
        friendTable.getActionMap().put(closeAction.toString(), closeAction);  
        
        shareTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),closeAction.toString() );
        shareTable.getActionMap().put(closeAction.toString(), closeAction); 
        
        inputField.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),closeAction.toString() );
        inputField.getActionMap().put(closeAction.toString(), closeAction);   
     
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                inputField.requestFocusInWindow();
            }
        });
    }
    

    private void addSelectedFriend() {
        if (friendTable.getSelectedRow() >= 0) {
            int row = friendTable.getSelectedRow();
            shareFriend(noShareFilterList.get(row));
            resetRowSelection(friendTable, row);
        }
    
    }
    
    private void setKeyStrokes(JComponent component) {
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up");
        component.getActionMap().put("up", up);
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down");
        component.getActionMap().put("down", down);       
    }
    
    //@Override
    public void show(Component c, Rectangle visibleRect){
        inputField.setText(null);
        
        ledgeWidth = c.getWidth();
        ledgeHeight = c.getHeight();
        
        adjustSize();
        
        //favor ledge on top
        boolean ledgeFitsOnBottom = c.getY() + c.getHeight() - getHeight() >= visibleRect.getY();
        boolean ledgeFitsOnTop = c.getY() + getHeight() <= visibleRect.getHeight();
 
        int y = 0; //y position for widget bounds
        if (ledgeFitsOnTop) {
            y = c.getY();
            ledgeY = 1;
        } else if (ledgeFitsOnBottom) {
            y = c.getY() + c.getHeight() - getHeight();
            ledgeY = getHeight() - c.getHeight() - 1;
        } else {
            y = (int) visibleRect.getY();
            ledgeY = c.getY() - y;
            if (ledgeY <= 0) {
                ledgeY = 1;
            }
        }
        
        adjustPainter();
        
        setBounds(c.getX() - mainPanel.getWidth() - HGAP * 2,y, getWidth(), getHeight());
        getParent().validate();
        setVisible(true);
    }

    //TODO: clean this up
    private void adjustPainter(){
        Area area = new Area(new RoundRectangle2D.Float(1, 1, mainPanel.getWidth() + HGAP * 2-1, getHeight()-2, BORDER_INSETS, BORDER_INSETS));
        Area area2 = new Area(new RoundRectangle2D.Float(mainPanel.getWidth() + HGAP * 2, ledgeY, ledgeWidth-1, ledgeHeight, BORDER_INSETS, BORDER_INSETS));
        area.exclusiveOr(area2);
        Area area3 = new Area(new Rectangle2D.Float(mainPanel.getWidth() + HGAP, ledgeY, BORDER_INSETS * 2, ledgeHeight));
        area.add(area3);
        shapePainter.setShape(area);
    }


    private void resetRowSelection(JTable table, int oldSelRow) {
        if(table.getRowCount() == 0){
            return;
        }
        
        if(oldSelRow == -1){
            table.setRowSelectionInterval(0, 0);
        } else if(table.getRowCount() > oldSelRow){
            table.setRowSelectionInterval(oldSelRow, oldSelRow);
        } else if ((oldSelRow - 1) >= 0 && (oldSelRow - 1) < table.getRowCount()) {
            table.setRowSelectionInterval(oldSelRow - 1, oldSelRow - 1);
        }
    }
    
    
    private void shareFriend(SharingTarget friend) {
        shareFriendList.add(friend);
        noShareFriendList.remove(friend);

        if (friend == GNUTELLA_SHARE) {
            gnutellaList.addFile(fileItem.getFile());
        } else {
            friendListMap.get(friend).addFile(fileItem.getFile());
        }
        adjustSize();
    }
    
    
    private void unshareFriend(SharingTarget friend) {
        shareFriendList.remove(friend);
        noShareFriendList.add(friend);
        
        if (friend == GNUTELLA_SHARE) {
            gnutellaList.removeFile(fileItem.getFile());
        } else {
            friendListMap.get(friend).removeFile(fileItem.getFile());
        }
        adjustSize();
    }
    
    //TODO: clean this up
    private void adjustSize(){
        int visibleRows = (noShareFriendList.size() < FRIEND_ROW_COUNT) ? noShareFriendList.size() : FRIEND_ROW_COUNT;
        friendTable.setVisibleRowCount(visibleRows);
        friendScroll.setVisible(noShareFriendList.size() > 0);
        friendLabel.setVisible(noShareFriendList.size() > 0);
        inputField.setVisible(noShareFriendList.size() > 1);
        
        visibleRows = (shareTable.getRowCount() < SHARED_ROW_COUNT) ? shareTable.getRowCount() : SHARED_ROW_COUNT;
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
        
        mainPanel.setSize(prefWidth, height);
        setSize(mainPanel.getSize().width + ledgeWidth + 2 * HGAP, mainPanel.getSize().height + BORDER_INSETS);
        revalidate();       
        adjustPainter(); 
        repaint();
    }
    
    
    @Override
    @SwingEDTEvent
    public void handleEvent(final RosterEvent event) {
        if(event.getType().equals(User.EventType.USER_ADDED)) {              
            addFriend(new SharingTarget(event.getSource()));
        } else if(event.getType().equals(User.EventType.USER_REMOVED)) {
            removeFriend(new SharingTarget(event.getSource()));
        }
    }   
    
    @EventSubscriber
    public void handleSignoff(SignoffEvent event) {
        shareFriendList.clear();
        noShareFriendList.clear();
        friendListMap.clear();
    }
    


    @Override
    @Inject
    public void register(ListenerSupport<RosterEvent> rosterEventListenerSupport) {
        rosterEventListenerSupport.addListener(this);
    }
    
    /**
     * @param fileItem  The LocalFileItem whose sharing info will be displayed
     */
    public void setFileItem(LocalFileItem fileItem){
        this.fileItem = fileItem;
        loadSharedBuddies();
    }
    
    //true if it is inside our funky shape
    @Override
    public boolean contains(int x, int y){
        return shapePainter.getShape().contains(x, y);
    }
    
    public boolean contains(Component c) {
        for (; c != null; c = c.getParent()) {
            if (c == this) {
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
        ((EventTableModel)friendTable.getModel()).dispose();
    }
    
    private void removeFriend(SharingTarget friend) {
        friendListMap.remove(friend);
        shareFriendList.remove(friend);
        noShareFriendList.remove(friend);
    }
    
    private void addFriend(SharingTarget friend) {
        LocalFileList fileList = libraryManager.getOrCreateFriendShareList(friend.getFriend());
        friendListMap.put(friend, fileList);
        loadFriend(friend);
    }
    
    private void loadSharedBuddies() {
        shareFriendList.clear();
        noShareFriendList.clear();

        if (fileItem.getCategory() != Category.DOCUMENT || SharingSettings.DOCUMENT_SHARING_ENABLED.getValue()) {
            loadFriend(GNUTELLA_SHARE);
        }
        
        for (SharingTarget friend : friendListMap.keySet()) {
            loadFriend(friend);
        }
    }
    
    private void loadFriend(SharingTarget friend) {
        if (isShared(fileItem, friend)) {
            shareFriendList.add(friend);
        } else {
            noShareFriendList.add(friend);
        }
    }
    
    private boolean isShared(FileItem fileItem, SharingTarget friend){
        if(friend == GNUTELLA_SHARE){
            return gnutellaList.getSwingModel().contains(fileItem);
        }
        return friendListMap.get(friend).getSwingModel().contains(fileItem);
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
            if(o1 == GNUTELLA_SHARE){
                return -1;
            }
            if(o2 == GNUTELLA_SHARE){
                return 1;
            }
            return o1.getFriend().getRenderName().compareTo(o2.getFriend().getRenderName());
        }
        
    }
    
    private static class Gnutella implements Friend {
        @Override
        public boolean isAnonymous() {
            return false;
        }
        
        @Override
        public String getId() {
            return "_@_internal_@_";
        }

        @Override
        public String getName() {
            return I18n.tr("LimeWire Network");
        }
        
        @Override
        public String getRenderName() {
            return getName();
        }

        public void setName(String name) {
            
        }

        public Network getNetwork() {
            return null;
        }
    }
 
}
