package org.limewire.ui.swing.advanced;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import net.miginfocom.swing.MigLayout;

import org.limewire.concurrent.ManagedThread;
import org.limewire.inspection.InspectionException;
import org.limewire.inspection.InspectionTool;
import org.limewire.inspection.Inspector;
import org.limewire.ui.swing.components.TextFieldClipboardControl;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Panel for debugging inspection points.  Allows inspection point quieries
 *  and dumping all the inspection points.
 *
 * 
 * <p>NOTE: Not translated because only for developement.
 */
public class InspectionsPanel extends TabPanel  {

    private final Inspector inspector;
    private final Injector injector;
    
    private final JTextArea inspectionListField;
    private final JTextArea outputTextField;
    private final JTable outputTable;
    private final JTextField filterTextField;

    private TableRowSorter<AbstractTableModel> sorter;
    
    @Inject
    public InspectionsPanel(Inspector inspector, Injector injector) {
        this.inspector = inspector;
        this.injector = injector;
        
        this.setLayout(new MigLayout("nogrid, fill"));
        
        Action refreshAction = new AbstractAction("Refresh") {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        };
        
        JLabel queryLabel = new JLabel("Query:");
        final JTextField queryField = new JTextField(100);
        JButton submitButton = new JButton();
        final JTextField resultField = new JTextField(100);
        
        JTabbedPane tabPane = new JTabbedPane();
        JPanel listPanel = new JPanel(new BorderLayout());
        JPanel textPanel = new JPanel(new BorderLayout());
        JPanel setupPanel = new JPanel(new BorderLayout());
        
        outputTable = new JTable();
        outputTable.setRowSelectionAllowed(true);
        JScrollPane outputTableScrollPane = new JScrollPane(outputTable);
        outputTextField = new JTextArea();
        JScrollPane outputTextScrollPane = new JScrollPane(outputTextField);
        outputTextField.setEditable(false);
        inspectionListField = new JTextArea();
        JScrollPane inspectionListScrollPane = new JScrollPane(inspectionListField);
        
        resultField.setEditable(false);
        TextFieldClipboardControl.install(queryField);
        TextFieldClipboardControl.install(resultField);
        TextFieldClipboardControl.install(outputTextField);
        TextFieldClipboardControl.install(inspectionListField);
        
        listPanel.setOpaque(false);
        textPanel.setOpaque(false);
        setupPanel.setOpaque(false);
        listPanel.setName("List Dump");
        textPanel.setName("Text Dump");
        setupPanel.setName("Setup");
        
        submitButton.setAction(new AbstractAction("Submit!") {
            @Override
            public void actionPerformed(ActionEvent e) {
                String queryString = queryField.getText().trim();
                
                if (queryString.length() < 1) {
                    return;
                }
                
                try {
                    Object result = InspectionsPanel.this.inspector.inspect(queryString, true);
                    resultField.setText(result.toString());
                } catch (InspectionException e1) {
                    resultField.setText(e1.getMessage());
                }
                
            }
        });      
        
        add(queryLabel);
        add(queryField);
        add(submitButton);
        add(resultField, "wrap");
        
        add(new JSeparator(JSeparator.HORIZONTAL), "growx, wrap");
        
        filterTextField = new JTextField(15);
        filterTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                resetFilter();
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                resetFilter();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                resetFilter();
            }
        });
        JPanel listBottomPanel = new JPanel(new MigLayout("fill, nogrid"));
        listBottomPanel.setOpaque(false);
        listBottomPanel.add(new JLabel("Filter:"));
        listBottomPanel.add(filterTextField);
        listPanel.add(outputTableScrollPane, BorderLayout.CENTER);
        listPanel.add(listBottomPanel, BorderLayout.SOUTH);
        
        
        textPanel.add(outputTextScrollPane, BorderLayout.CENTER);
        
        setupPanel.add(inspectionListScrollPane, BorderLayout.CENTER);
        JPanel setupBottomPanel = new JPanel(new MigLayout("fill, nogrid"));
        setupBottomPanel.setOpaque(false);
        JButton importButton = new JButton(new AbstractAction("Retrieve Inspection Points") {
            @Override
            public void actionPerformed(ActionEvent e) {
                populate();
            }
        });
        JButton loadButton = new JButton(refreshAction);
        setupBottomPanel.add(importButton);
        setupBottomPanel.add(loadButton, "gapbefore push");
        setupPanel.add(setupBottomPanel, BorderLayout.SOUTH);
            
        tabPane.add(listPanel);
        tabPane.add(textPanel);
        tabPane.add(setupPanel);
        
        final JPopupMenu refreshMenu = new JPopupMenu();
        refreshMenu.add(refreshAction);
        tabPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    refreshMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        
        add(tabPane, "dock center");
        
    }
    
    private void populate() {
        
        new ManagedThread("Debug Inspection Finder Thread") {
            @Override
            public void run() {
                Map<String, String> mappings = InspectionTool.generateMappings(CommonUtils.getCurrentDirectory(), injector, new String[] {"tests"});
            
                final StringBuffer resultsBuffer = new StringBuffer();
            
                for ( Entry<String,String> entry : mappings.entrySet() ) {
                    resultsBuffer.append(entry.getKey());
                    resultsBuffer.append('=');
                    resultsBuffer.append(entry.getValue());
                    resultsBuffer.append('\n');
                }
                
                SwingUtils.invokeNowOrLater(new Runnable() {
                    @Override
                    public void run() {
                        inspectionListField.setText(resultsBuffer.toString());
                    }
                });
            }
        }.start();
    }
    
    private void refresh() {

        final String[] lines = inspectionListField.getText().split("\n");
        
        final List<String> nameItems = new ArrayList<String>(lines.length);
        final List<String> resultItems = new ArrayList<String>(lines.length);
        
        final AbstractTableModel model = new AbstractTableModel() {
            @Override
            public String getColumnName(int columnIndex) {
                if (columnIndex == 0) {
                    return "Name";
                } else {
                    return "Result";
                }
            }
            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == 0) {
                    return nameItems.get(rowIndex);
                } else {
                    return resultItems.get(rowIndex);
                }
            }
            @Override
            public int getRowCount() {
                return nameItems.size();
            }
            @Override
            public int getColumnCount() {
                return 2;
            }
        };
        
        final StringBuffer resultsBuffer = new StringBuffer();
        
        for ( String line : lines ) {
            String key = line.trim();
            int equalsLocation = key.indexOf('=');
            if (equalsLocation > -1) {
                key = key.substring(equalsLocation+1).trim();
            }
            if (key.length() > 0) {
                resultsBuffer.append(key);
                nameItems.add(key);
                
                resultsBuffer.append('=');
                    
                String value;
                try {
                    Object result = inspector.inspect(key, true);
                    
                    if (result != null) {
                        value = result.toString();
                    } else {
                        value = "[NULL]";
                    }
                } catch (InspectionException e) {
                    value = '[' + e.getMessage() + ']';
                }
                
                resultsBuffer.append(value);
                resultItems.add(value);
                
                resultsBuffer.append('\n');
            }
        }
                
        outputTextField.setText(resultsBuffer.toString());
        outputTable.setModel(model);
                       
        sorter = new TableRowSorter<AbstractTableModel>(model);
        resetFilter();
        outputTable.setRowSorter(sorter);
    }
    
    private void resetFilter() {
        if (sorter == null) {
            return;
        }
        
        try {
            sorter.setRowFilter(RowFilter.regexFilter(filterTextField.getText(), 0));
        } catch (PatternSyntaxException e) {
        }
    }

    @Override
    public void initData() {
        // Empty
    }
    
    @Override
    public boolean isTabEnabled() {
        return true;
    }

    @Override
    public void dispose() {
        // Empty
    }

}
