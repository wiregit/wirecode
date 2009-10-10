package org.limewire.ui.swing.options;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.core.settings.FilterSettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.table.AbstractTableFormat;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;

import ca.odell.glazedlists.swing.DefaultEventTableModel;

import com.google.inject.Inject;

/**
 * Creates a table to manage which file extensions will not show up in search results.
 */
public class FilterFileExtensionsOptionPanel extends AbstractFilterOptionPanel {
    
    private final SpamManager spamManager;
    private final IconManager iconManager;
    
    private JButton defaultButton;
    private JButton okButton;
    private JTextField keywordTextField;
    private JButton addKeywordButton;
    private JXTable filterTable;
        
    @Inject
    public FilterFileExtensionsOptionPanel(IconManager iconManager, SpamManager spamManager) {
        
        this.iconManager = iconManager;
        this.spamManager = spamManager;
        
        setLayout(new MigLayout("gapy 10, nogrid"));
        
        keywordTextField = new JTextField(30);
        addKeywordButton = new JButton(I18n.tr("Add Extension"));
        
     
                
        filterTable = new MouseableTable(new DefaultEventTableModel<String>(eventList, new FileFilterTableFormat()));
        
        filterTable.setShowGrid(false, false);
        filterTable.setColumnSelectionAllowed(false);
        filterTable.setSelectionMode(0);
        TableColumn column = filterTable.getColumn(0); 
        column.setCellRenderer(new IconRenderer());
        column.setWidth(16);
        column.setMaxWidth(16);
        column.setMinWidth(16);
        filterTable.getColumn(2).setCellRenderer(new RemoveButtonRenderer(filterTable));
        filterTable.getColumn(2).setCellEditor(new RemoveButtonRenderer(filterTable));
        
        okButton = new JButton(new OKDialogAction());
        addKeywordButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = keywordTextField.getText();
                if(text == null || text.trim().length() == 0)
                    return;
                if(!eventList.contains(text)) {
                    if(text.charAt(0) != '.')
                        text = "." + text;
                    eventList.add(text);
                }
                keywordTextField.setText("");
            }
        });
        
        defaultButton = new JButton(new DefaultAction());
        
        add(new MultiLineLabel(I18n.tr("LimeWire will not show files with the following extensions in your search results"), 300), "span, wrap");
        add(keywordTextField, "gapright 10");
        add(addKeywordButton,"wrap");
        add(new JScrollPane(filterTable), "span 2, grow, wrap");
        
        add(defaultButton, "alignx left");
        add(okButton, "tag ok, alignx right");
    }
    
    @Override
    boolean applyOptions() {
        String[] values = eventList.toArray(new String[eventList.size()]);
        FilterSettings.BANNED_EXTENSIONS.set(values);
        spamManager.adjustSpamFilters();
        return false;
    }

    @Override
    boolean hasChanged() {
        List model = Arrays.asList(FilterSettings.BANNED_EXTENSIONS.get());
        String[] values = eventList.toArray(new String[eventList.size()]);
        
        return model.equals(new ArrayList<String>(Arrays.asList(values)));
    }

    @Override
    public void initOptions() {
        eventList.clear();
        String[] bannedWords = FilterSettings.BANNED_EXTENSIONS.get();
        eventList.addAll(new ArrayList<String>(Arrays.asList(bannedWords)));
    }
    
    /**
     * Reverts the extensions not shown in search results to the default setting.
     */
    private class DefaultAction extends AbstractAction {
        public DefaultAction() {
            putValue(Action.NAME, I18n.tr("Use Default"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Revert to default settings"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            FilterSettings.BANNED_EXTENSIONS.revertToDefault();
            String[] bannedWords = FilterSettings.BANNED_EXTENSIONS.get();
            eventList.clear();
            eventList.addAll(new ArrayList<String>(Arrays.asList(bannedWords)));
        }
    }
    
    private class FileFilterTableFormat extends AbstractTableFormat<String> {

        private static final int ICON_INDEX = 0;
        private static final int NAME_INDEX = 1;
        private static final int BUTTON_INDEX = 2;
        
        public FileFilterTableFormat() {
            super("", I18n.tr("Extensions"), "");
        }

        @Override
        public Object getColumnValue(String baseObject, int column) {
            switch(column) {
                case ICON_INDEX: 
                    return iconManager.getIconForExtension(baseObject.substring(1));
                case NAME_INDEX: return baseObject;
                case BUTTON_INDEX: return baseObject;
            }
                
            throw new IllegalStateException("Unknown column:" + column);
        }
    }
    
    private static class IconRenderer implements TableCellRenderer {

        private final JLabel component = new JLabel(); 
        
        public IconRenderer() {
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null) {
                return null;
            }
            component.setIcon((Icon)value);
            
            return component;
        }
    }
}
