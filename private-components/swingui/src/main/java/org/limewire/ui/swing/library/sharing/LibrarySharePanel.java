package org.limewire.ui.swing.library.sharing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.painter.ShapePainter;
import org.jdesktop.swingx.painter.AbstractLayoutPainter.HorizontalAlignment;
import org.jdesktop.swingx.painter.AbstractLayoutPainter.VerticalAlignment;
import org.limewire.core.api.library.BuddyFileList;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.ui.swing.RoundedBorder;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.SignoffEvent;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.gui.WritableTableFormat;
import ca.odell.glazedlists.swing.EventTableModel;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LibrarySharePanel extends JXPanel implements RegisteringEventListener<RosterEvent>{



    private static final int BUDDY_ROW_COUNT = 4;
    private static final int SHARED_ROW_COUNT = 20;
    private static final int BORDER_INSETS = 10;
    private static final int HGAP = 5;
    
    
    private  final SharingTarget GNUTELLA_SHARE = new SharingTarget(I18n.tr("Gnutella Network"));

    private JTextField inputField;

    private EventList<SharingTarget> noShareBuddyList;
    private EventList<SharingTarget> shareBuddyList;

    private JScrollPane shareScroll;
    private JScrollPane buddyScroll;

    private JXTable shareTable;
    private JXTable buddyTable;

    private JLabel buddyLabel;
    private JLabel shareLabel;
    
    private JXPanel mainPanel;
    
    private LibraryManager libraryManager;
    
    private LocalFileList gnutellaList;
    
    private ShapePainter shapePainter;
    
    private Map<SharingTarget, LocalFileList> buddyListMap;
    private LocalFileItem fileItem;
    
    private int ledgeWidth;
    private int ledgeHeight;
    
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
    
    
    @Inject
    public LibrarySharePanel(LibraryManager libraryManager) {
        GuiUtils.assignResources(this);
        
        
        this.libraryManager = libraryManager;
        buddyListMap = new ConcurrentHashMap<SharingTarget, LocalFileList>();   
        gnutellaList = libraryManager.getGnutellaList();
        
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
        
        shareBuddyList = GlazedLists.threadSafeList(new SortedList<SharingTarget>(new BasicEventList<SharingTarget>(), new SharingTargetComparator()));
       
        shareTable = new MouseableTable(new EventTableModel<SharingTarget>(shareBuddyList, new LibraryShareTableFormat(1)));
        shareTable.setTableHeader(null);
        final ShareRendererEditor removeEditor = new ShareRendererEditor(removeIcon, removeIconRollover, removeIconPressed);
        removeEditor.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
               unshareBuddy(removeEditor.getBuddy());
               removeEditor.cancelCellEditing();
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
  
        noShareBuddyList = GlazedLists.threadSafeList(new SortedList<SharingTarget>(new BasicEventList<SharingTarget>(), new SharingTargetComparator()));
        
        TextFilterator<SharingTarget> textFilter = new TextFilterator<SharingTarget>() {
            @Override
            public void getFilterStrings(List<String> baseList, SharingTarget element) {
                baseList.add(element.getName());
            }
        };
        EventList<SharingTarget> noShareSortedList = new FilterList<SharingTarget>(noShareBuddyList, 
                new TextComponentMatcherEditor<SharingTarget>(inputField, textFilter));
        
        buddyTable = new MouseableTable(new EventTableModel<SharingTarget>(noShareSortedList, new LibraryShareTableFormat(0)));
        //do nothing ColorHighlighter eliminates default striping
        buddyTable.setHighlighters(new ColorHighlighter());
        buddyTable.setTableHeader(null);
        buddyTable.setOpaque(false);
        buddyTable.setShowGrid(false);

        final ShareRendererEditor addEditor = new ShareRendererEditor(addIcon, addIconRollover, addIconPressed);
        addEditor.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
               shareBuddy(addEditor.getBuddy());
               addEditor.cancelCellEditing();
            }            
        });
        
        buddyTable.getColumnModel().getColumn(0).setCellEditor(addEditor);
        buddyTable.getColumnModel().getColumn(0).setPreferredWidth(addEditor.getPreferredSize().width); 
        buddyTable.getColumnModel().getColumn(0).setMaxWidth(addEditor.getPreferredSize().width);       
        buddyTable.getColumnModel().getColumn(0).setCellRenderer(new ShareRendererEditor(addIcon, addIconRollover, addIconPressed));

        buddyTable.setRowHeight(addEditor.getPreferredSize().height);
        buddyTable.setVisibleRowCount(BUDDY_ROW_COUNT);
        buddyScroll = new JScrollPane(buddyTable);      
        buddyScroll.setBorder(new RoundedBorder(5));
        
        shareScroll = new JScrollPane(shareTable);
        shareScroll.setBorder(new EmptyBorder(0, shareTableIndent, 0, 0));
        shareScroll.setOpaque(false);

        buddyLabel = new JLabel(I18n.tr("To share, type name below"));
        
        Dimension labelSize = buddyLabel.getPreferredSize().width > shareLabel.getPreferredSize().width ? 
                buddyLabel.getPreferredSize() : shareLabel.getPreferredSize();
        buddyLabel.setPreferredSize(labelSize);
        shareLabel.setPreferredSize(labelSize);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, HGAP, 0, HGAP);
        mainPanel.add(buddyLabel, gbc);
        
        gbc.gridy++;
        gbc.weighty = 0;
        mainPanel.add(inputField, gbc);
        gbc.gridy++;
        gbc.weighty = 1.0;
        mainPanel.add(buddyScroll, gbc);
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
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                inputField.requestFocus();
            }
        });        


        EventAnnotationProcessor.subscribe(this);   
        
    }
    
    //@Override
    public void show(Component c, int x, int y){
       // super.show(c, x, y);
        ledgeWidth = c.getWidth();
        ledgeHeight = c.getHeight();
        adjustSize();
        setBounds(x + c.getX() - mainPanel.getWidth() - HGAP * 2,y + c.getY(), getWidth(), getHeight());
        getParent().validate();
        setVisible(true);
    }

    //TODO: clean this up
    private void adjustPainter(){
        Area area = new Area(new RoundRectangle2D.Float(1, 1, mainPanel.getWidth() + HGAP * 2-1, getHeight()-2, BORDER_INSETS, BORDER_INSETS));
        Area area2 = new Area(new RoundRectangle2D.Float(mainPanel.getWidth() + HGAP * 2, 1, ledgeWidth-1, ledgeHeight, BORDER_INSETS, BORDER_INSETS));
        area.exclusiveOr(area2);
        Area area3 = new Area(new Rectangle2D.Float(mainPanel.getWidth() + HGAP, 1, BORDER_INSETS * 2, ledgeHeight));
        area.add(area3);
        shapePainter.setShape(area);
    }

    private void shareBuddy(SharingTarget buddy) {
        shareBuddyList.add(buddy);
        noShareBuddyList.remove(buddy);

        if (buddy == GNUTELLA_SHARE) {
            gnutellaList.addFile(fileItem.getFile());
        } else {
            buddyListMap.get(buddy).addFile(fileItem.getFile());
        }
        adjustSize();
    }
    
    
    private void unshareBuddy(SharingTarget buddy) {
        shareBuddyList.remove(buddy);
        noShareBuddyList.add(buddy);
        
        if (buddy == GNUTELLA_SHARE) {
            gnutellaList.removeFile(fileItem.getFile());
        } else {
            buddyListMap.get(buddy).removeFile(fileItem.getFile());
        }
        adjustSize();
    }
    
    //TODO: clean this up
    private void adjustSize(){
        int visibleRows = (noShareBuddyList.size() < BUDDY_ROW_COUNT) ? noShareBuddyList.size() : BUDDY_ROW_COUNT;
        buddyTable.setVisibleRowCount(visibleRows);
        buddyScroll.setVisible(noShareBuddyList.size() > 0);
        buddyLabel.setVisible(noShareBuddyList.size() > 0);
        inputField.setVisible(noShareBuddyList.size() > 1);
        
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
        setSize(mainPanel.getSize().width + ledgeWidth + 2 * HGAP, mainPanel.getSize().height + 2 * BORDER_INSETS);
        revalidate();       
        adjustPainter(); 
        repaint();
    }
    
    
    @Override
    public void handleEvent(final RosterEvent event) {
        if(event.getType().equals(User.EventType.USER_ADDED)) {              
            addBuddy(new SharingTarget(event.getSource().getId()));
        } else if(event.getType().equals(User.EventType.USER_REMOVED)) {
            removeBuddy(new SharingTarget(event.getSource().getId()));
        } else if(event.getType().equals(User.EventType.USER_UPDATED)) {
        }
    }   
    
    @EventSubscriber
    public void handleSignoff(SignoffEvent event) {
        shareBuddyList.clear();
        noShareBuddyList.clear();
        buddyListMap.clear();
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
    
    private void removeBuddy(SharingTarget buddy) {
        buddyListMap.remove(buddy);
        shareBuddyList.remove(buddy);
        noShareBuddyList.remove(buddy);
    }
    
    private void addBuddy(SharingTarget buddy) {
        if(!libraryManager.containsBuddy(buddy.getName())) {
            libraryManager.addBuddy(buddy.getName());
        }
        BuddyFileList fileList = (BuddyFileList) libraryManager.getBuddy(buddy.getName());
        buddyListMap.put(buddy, fileList);
        loadBuddy(buddy);
    }
    
    private void loadSharedBuddies() {
        shareBuddyList.clear();
        noShareBuddyList.clear();
        
        loadBuddy(GNUTELLA_SHARE);
        
        for (SharingTarget buddy : buddyListMap.keySet()) {
            loadBuddy(buddy);
        }
    }
    
    private void loadBuddy(SharingTarget buddy){
        if (isShared(fileItem, buddy)){
            shareBuddyList.add(buddy);
        } else {
            noShareBuddyList.add(buddy);
        }
    
    }
    
    private boolean isShared(FileItem fileItem, SharingTarget buddy){
        if(buddy == GNUTELLA_SHARE){
            return gnutellaList.getModel().contains(fileItem);
        }
        return buddyListMap.get(buddy).getModel().contains(fileItem);
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
            return baseObject.getName();
        }

        @Override
        public boolean isEditable(SharingTarget baseObject, int column) {
            if (column == editColumn) {
                return true;
            }

            return false;
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
            return o1.getName().compareTo(o2.getName());
        }
        
    }
 
}
