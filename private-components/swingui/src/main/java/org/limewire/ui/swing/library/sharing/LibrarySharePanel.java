package org.limewire.ui.swing.library.sharing;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
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
public class LibrarySharePanel extends JPopupMenu implements RegisteringEventListener<RosterEvent>{



    private static final int BUDDY_ROW_COUNT = 4;
    private static final int SHARED_ROW_COUNT = 20;
    private static final int BORDER_INSETS = 10;

    private JTextField inputField;

    private EventList<String> noShareBuddyList;
    private EventList<String> shareBuddyList;

    private JScrollPane shareScroll;
    private JScrollPane buddyScroll;

    private JXTable shareTable;
    private JXTable buddyTable;

    private JLabel buddyLabel;
    private JLabel shareLabel;
    
    private LibraryManager libraryManager;
    
    private Map<String, LocalFileList> buddyListMap;
    private LocalFileItem fileItem;
    
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
    
    
    @Inject
    public LibrarySharePanel(LibraryManager libraryManager) {
        EventAnnotationProcessor.subscribe(this);   
        GuiUtils.assignResources(this);

        setLayout(new GridBagLayout());
        
        setBorder(new RoundedBorder(BORDER_INSETS));
        this.libraryManager = libraryManager;
        buddyListMap = new HashMap<String,LocalFileList>();        
        
        inputField = new JTextField(12);
        
        shareLabel = new JLabel(I18n.tr("Currently sharing with"));
        
        shareBuddyList = GlazedLists.threadSafeList(new SortedList<String>(new BasicEventList<String>()));
       
        shareTable = new MouseableTable(new EventTableModel<String>(shareBuddyList, new LibraryShareTableFormat(1)));
        shareTable.setOpaque(false);
        shareTable.setTableHeader(null);
        final ShareRendererEditor removeEditor = new ShareRendererEditor(removeIcon, removeIconRollover, removeIconPressed);
        removeEditor.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
               unshareBuddy(removeEditor.getBuddy());
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
        shareTable.setBorder(null);
        shareTable.setOpaque(false);
  
        noShareBuddyList = GlazedLists.threadSafeList(new SortedList<String>(new FilterList<String>(new BasicEventList<String>(),
                new TextComponentMatcherEditor<String>(inputField,
                        new TextFilterator<String>() {
                            @Override
                            public void getFilterStrings(List<String> baseList, String element) {
                                baseList.add(element);
                            }
                        }
                ))));
        
        buddyTable = new MouseableTable(new EventTableModel<String>(noShareBuddyList, new LibraryShareTableFormat(0)));
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
            }            
        });
        
        buddyTable.getColumnModel().getColumn(0).setCellEditor(addEditor);
        buddyTable.getColumnModel().getColumn(0).setPreferredWidth(addEditor.getPreferredSize().width); 
        buddyTable.getColumnModel().getColumn(0).setMaxWidth(addEditor.getPreferredSize().width);       
        buddyTable.getColumnModel().getColumn(0).setCellRenderer(new ShareRendererEditor(addIcon, addIconRollover, addIconPressed));

        buddyTable.setRowHeight(addEditor.getPreferredSize().height);
        buddyTable.setVisibleRowCount(BUDDY_ROW_COUNT);
        buddyScroll = new JScrollPane(buddyTable);        
        
        shareScroll = new JScrollPane(shareTable);
        shareScroll.setBorder(null);
        shareScroll.setOpaque(false);
        
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;
        buddyLabel = new JLabel(I18n.tr("To share, type name below"));
        add(buddyLabel, gbc);
        
        gbc.gridy++;
        gbc.weighty = 0;
        add(inputField, gbc);
        gbc.gridy++;
        gbc.weighty = 1.0;
        add(buddyScroll, gbc);
        gbc.gridy++;
        gbc.weighty = 0;
        add(new JSeparator(), gbc);   
        gbc.gridy++;
        gbc.weighty = 0;
        add(shareLabel, gbc);
        gbc.gridy++;
        gbc.weighty = 1.0;
        add(shareScroll, gbc);

        adjustSize();
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                inputField.requestFocus();
            }
        });        

    }
    
    @Override
    public void show(Component c, int x, int y){
        super.show(c, x, y);
        adjustSize();
    }

    private void shareBuddy(String buddy){
        shareBuddyList.add(buddy);
        noShareBuddyList.remove(buddy);
        buddyListMap.get(buddy).addFile(fileItem.getFile());
        adjustSize();
    }
    
    
    private void unshareBuddy(String buddy){
        shareBuddyList.remove(buddy); 
        noShareBuddyList.add(buddy);
        buddyListMap.get(buddy).removeFile(fileItem.getFile());
        adjustSize();
    }
    
    private void adjustSize(){
        int visibleRows = (buddyTable.getRowCount() < BUDDY_ROW_COUNT) ? buddyTable.getRowCount() : BUDDY_ROW_COUNT;
        buddyTable.setVisibleRowCount(visibleRows);
        buddyScroll.setVisible(visibleRows > 0);
        buddyLabel.setVisible(visibleRows > 0);
        inputField.setVisible(visibleRows > 0);
        
        visibleRows = (shareTable.getRowCount() < SHARED_ROW_COUNT) ? shareTable.getRowCount() : SHARED_ROW_COUNT;
        shareTable.setVisibleRowCount(visibleRows);
        shareScroll.setVisible(visibleRows > 0);
        shareLabel.setVisible(visibleRows > 0);
        
        int height = 0;
        for(Component c : getComponents()){
            if (c.isVisible()) {
                height += c.getPreferredSize().height;
            }
        }
        setPopupSize(190, height + 2 * BORDER_INSETS);
        revalidate();
    }
    
    
    @Override
    public void handleEvent(final RosterEvent event) {
        if(event.getType().equals(User.EventType.USER_ADDED)) {              
                    addBuddy(event.getSource().getId());
        } else if(event.getType().equals(User.EventType.USER_REMOVED)) {
            removeBuddy(event.getSource().getId());
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
    
    private void removeBuddy(String name) {
        buddyListMap.remove(name);
        shareBuddyList.remove(name);
        noShareBuddyList.remove(name);
        loadBuddy(name);
    }
    
    private void addBuddy(String name) {
        BuddyFileList fileList = (BuddyFileList) libraryManager.getBuddy(name);
        buddyListMap.put(name, fileList);
        loadBuddy(name);
    }
    
    private void loadSharedBuddies() {
        shareBuddyList.clear();
        noShareBuddyList.clear();
        for (String name : buddyListMap.keySet()) {
            loadBuddy(name);
        }
    }
    
    private void loadBuddy(String name){
        if (isShared(fileItem, name)){
            shareBuddyList.add(name);
        } else {
            noShareBuddyList.add(name);
        }
    
    }
    
    private boolean isShared(FileItem fileItem, String buddy){
        return buddyListMap.get(buddy).getModel().contains(fileItem);
    }   
    
    private static class LibraryShareTableFormat implements WritableTableFormat<String> {
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
        public Object getColumnValue(String baseObject, int column) {
            return baseObject;
        }

        @Override
        public boolean isEditable(String baseObject, int column) {
            if (column == editColumn) {
                return true;
            }

            return false;
        }

        @Override
        public String setColumnValue(String baseObject, Object editedValue, int column) {
            return baseObject;
        }

    }
 
}
