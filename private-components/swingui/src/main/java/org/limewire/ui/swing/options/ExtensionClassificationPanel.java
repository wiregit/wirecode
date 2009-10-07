package org.limewire.ui.swing.options;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.TableDecorator;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;

/**
 * A panel that simply displays the current file to category associations being used.
 */
public class ExtensionClassificationPanel extends JPanel {

    private final CategoryManager categoryManager;
    private final IconManager iconManager;
    private final JXTable table;
    private final ButtonDecorator buttonDecorator;
    
    private Category[] currentCategories = null;
    
    private static final Category[] ALL_CATEGORIES = Category.values();
    private final JPanel switchPanel;
    
    @Inject
    public ExtensionClassificationPanel(CategoryManager categoryManager, IconManager iconManager,
            TableDecorator tableDecorator, ButtonDecorator buttonDecorator) {

        super(new BorderLayout());
        
        this.categoryManager = categoryManager;
        this.iconManager = iconManager;
        this.buttonDecorator = buttonDecorator;
                
        setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        setOpaque(false);
        
        add(new JLabel("<html>"+I18n.tr("Below are the file extensions LimeWire knows about and how they are classified throughout the program")+"</html>"),
            BorderLayout.NORTH);
        
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        
        switchPanel = new JPanel(new MigLayout("insets 5, gap 5"));
        
        centerPanel.add(switchPanel, BorderLayout.NORTH);
        
        table = new JXTable(); 
        tableDecorator.decorate(table);        
        
        table.setShowGrid(false, false);
        table.setColumnSelectionAllowed(false);
        table.setSelectionMode(0);
        
        JScrollPane scrollPane = new JScrollPane(table, 
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setOpaque(false);
        
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new MigLayout("insets 4 0 0 0, gap 4, fill"));
        buttonPanel.setOpaque(false);
        buttonPanel.add(new JButton(new OKDialogAction()), "tag ok");
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Builds a table model containing the extension association information for a given
     *  set of categories. 
     */
    private TableModel createTableModel(Category... categories) {
        Collection<String> extensions = new HashSet<String>();
        for ( Category category : categories ) {
            extensions.addAll(categoryManager.getExtensionsForCategory(category));
        }
        
        List<String> sortedList = new ArrayList<String>(extensions);
        Collections.sort(sortedList);
        extensions = sortedList;
        
        List<String> headingList = new ArrayList<String>();
        headingList.add(""); // Icon
        headingList.add(I18n.tr("Extension"));
        if (categories.length > 1) {
            headingList.add(I18n.tr("Category"));
        }
        if (OSUtils.isWindows()) {
            headingList.add(I18n.tr("Type"));
        }
        
        Object[][] data = new Object[extensions.size()][headingList.size()];
        
        int y = 0;
        for ( String ext : extensions ) {
            int x = 0;
            
            Icon icon = iconManager.getIconForExtension(ext);
            
            data[y][x++] = icon;
            data[y][x++] = ext;
            
            if (categories.length > 1) {
                data[y][x++] = I18n.tr(categoryManager.getCategoryForExtension(ext).getSingularName());
            }
            if (OSUtils.isWindows() && icon != null) {
                data[y][x++] = icon.toString();
            }
               
            y++;
        }
        
        return new DefaultTableModel(data, headingList.toArray()) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0) {
                    return Icon.class;
                }
                else {
                    return String.class;
                }
            }
        };
    }
    
    private class CategorySwitchAction extends AbstractAction {
        
        private final Category[] categoriesForSwitch;
        
        public CategorySwitchAction(String name, Category... categoriesForSwitch) {
            super(name);
            this.categoriesForSwitch = categoriesForSwitch;
        }
        
        public CategorySwitchAction(Category... categoriesForSwitch) {
            super(I18n.tr(categoriesForSwitch[0].getPluralName()));
            this.categoriesForSwitch = categoriesForSwitch;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            switchCategory(categoriesForSwitch);
            updateSelection((JButton)e.getSource());
        }
    }
    
    private void switchCategory(Category... categories) {
        
        if (categories == currentCategories) {
            return;
        }
        
        currentCategories = categories;
        TableModel model = createTableModel(currentCategories);

        table.setModel(model);
        TableColumn iconColumn = table.getColumn(model.getColumnName(0));
        iconColumn.setResizable(false);
        iconColumn.setMinWidth(16);
        iconColumn.setMaxWidth(16);
        iconColumn.setWidth(16);
    }
    
    private void updateSelection(JButton button) {
        button.setSelected(true);
        for ( Component comp : switchPanel.getComponents() ) {
            if (comp instanceof JButton && button != comp) {
                ((JButton)comp).setSelected(false);
            }
        }
    }
    
    private JButton createSelectionButton(Action action) {
        JXButton button = new JXButton(action);
        button.setModel(new JToggleButton.ToggleButtonModel());
        buttonDecorator.decorateLinkButton(button);
        return button;
    }
    
    /**
     * Loads the panel for use.  Builds the table and lays out the component.
     */
    public void init() {
        switchCategory(ALL_CATEGORIES);
        
        switchPanel.removeAll();
        switchPanel.invalidate();
        
        switchPanel.add(new JLabel(I18n.tr("Show:")));
        
        JButton allButton = createSelectionButton(new CategorySwitchAction(I18n.tr("All"), ALL_CATEGORIES));
        allButton.setSelected(true);
        switchPanel.add(allButton);
        
        for ( Category category : ALL_CATEGORIES ) {
            if (categoryManager.getExtensionsForCategory(category).size() > 0) {
                switchPanel.add(createSelectionButton(new CategorySwitchAction(category)));
            }
        }
    }
}
