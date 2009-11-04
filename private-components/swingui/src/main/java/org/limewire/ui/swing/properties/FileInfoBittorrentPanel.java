package org.limewire.ui.swing.properties;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentEvent;
import org.limewire.bittorrent.TorrentEventType;
import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.listener.EventListener;
import org.limewire.ui.swing.table.AbstractTableFormat;
import org.limewire.ui.swing.table.DefaultLimeTableCellRenderer;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

public class FileInfoBittorrentPanel implements FileInfoPanel, EventListener<TorrentEvent> {

    public static final String TORRENT_FILE_ENTRY_SELECTED = "torrentFileEntrySelected";
    
    private static final int DONT_DOWNLOAD = 0;

    private static final int LOWEST_PRIORITY = 1;

    private static final int NORMAL_PRIORITY = 2;

    private static final int HIGHEST_PRIORITY = 3;

    private final Torrent torrent;

    private final JPanel component;

    private BitTorrentTable table;

    /**
     * Items in the eventList are expected to be in the order that they are
     * returned from the Torrent instance. This is so we can pull items out by
     * the matching index in the TorrentFileEntry.
     */
    private EventList<TorrentFileEntryWrapper> eventList;

    private PropertyChangeSupport support = new PropertyChangeSupport(this);
    
    public FileInfoBittorrentPanel(Torrent torrent) {
        this.torrent = torrent;

        component = new JPanel(new MigLayout("fill"));

        init();
    }

    @Override
    public JComponent getComponent() {
        return component;
    }

    @Override
    public void updatePropertiableFile(PropertiableFile file) {
        // do nothing
    }

    private void init() {
        component.setOpaque(false);

        ObservableElementList.Connector<TorrentFileEntryWrapper> torrentFileEntryConnector = GlazedLists
                .beanConnector(TorrentFileEntryWrapper.class);
        eventList = GlazedListsFactory.observableElementList(
                new BasicEventList<TorrentFileEntryWrapper>(), torrentFileEntryConnector);

        List<TorrentFileEntry> fileEntries = torrent.getTorrentFileEntries();
        for (TorrentFileEntry entry : fileEntries) {
            eventList.add(new TorrentFileEntryWrapper(entry));
        }
        
        table = new BitTorrentTable(new DefaultEventTableModel<TorrentFileEntryWrapper>(eventList,
                new BitTorrentTableFormat()));

        component.add(new JScrollPane(table), "grow");

        torrent.addListener(this);
    }

    @Override
    public boolean hasChanged() {
        boolean hasChanged = false;
        for (TorrentFileEntryWrapper wrapper : eventList) {
            if (wrapper.hasChanged()) {
                hasChanged = true;
                break;
            }
        }
        return hasChanged;
    }

    @Override
    public void save() {
        if (hasChanged() && !torrent.isFinished()) {
            for (TorrentFileEntryWrapper wrapper : eventList) {
                torrent.setTorrenFileEntryPriority(wrapper.getTorrentFileEntry(), wrapper
                        .getPriority());
            }
        }
    }

    @Override
    public void dispose() {
        torrent.removeListener(this);
    }

    private class BitTorrentTable extends MouseableTable {
        public BitTorrentTable(final DefaultEventTableModel<TorrentFileEntryWrapper> model) {
            super(model);
            setShowHorizontalLines(false);
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            setColumnSelectionAllowed(false);
            final CheckBoxRendererEditor checkBoxEditor = new CheckBoxRendererEditor();
            checkBoxEditor.addActionListener(new ActionListener() {
                boolean torrentPartSelected = true;
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (checkBoxEditor.getCellEditorValue() != null) {
                        checkBoxEditor.getCellEditorValue().setPriority(
                                checkBoxEditor.isSelected() ? 1 : 0);
                        checkBoxEditor.cancelCellEditing();
                    }
                    
                    if ( isAnyTorrentPartSelected() != torrentPartSelected )
                    {
                        torrentPartSelected = !torrentPartSelected;
                        support.firePropertyChange(TORRENT_FILE_ENTRY_SELECTED, !torrentPartSelected, torrentPartSelected);
                    }
                    
                    BitTorrentTable.this.repaint();
                }
                
                private boolean isAnyTorrentPartSelected()
                {
                    for (int counter = 0; counter < model.getRowCount(); counter++)
                    {
                        TorrentFileEntryWrapper torrentFile = model.getElementAt(counter);
                        if (torrentFile.getPriority() != 0)
                            return true;
                    }
                    
                    return false;
                }
            });
            getColumn(BitTorrentTableFormat.DOWNLOAD_INDEX).setCellRenderer(
                    new CheckBoxRendererEditor());
            getColumn(BitTorrentTableFormat.DOWNLOAD_INDEX).setCellEditor(checkBoxEditor);

            getColumn(BitTorrentTableFormat.SIZE_INDEX).setCellRenderer(new FileSizeRenderer());

            getColumn(BitTorrentTableFormat.PERCENT_INDEX).setCellRenderer(new PercentRenderer());
            getColumn(BitTorrentTableFormat.NAME_INDEX).setCellRenderer(
                    new DefaultLimeTableCellRenderer());

            final PriorityRendererEditor editor = new PriorityRendererEditor();
            editor.getButton().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int oldPriority = editor.getCellEditorValue().getPriority();
                    if (oldPriority != DONT_DOWNLOAD) {
                        editor.getCellEditorValue().setPriority(
                                oldPriority + 1 > HIGHEST_PRIORITY ? LOWEST_PRIORITY
                                        : (oldPriority + 1));
                        editor.cancelCellEditing();
                        BitTorrentTable.this.repaint();
                    }
                }

            });
            getColumn(BitTorrentTableFormat.PRIORITY_INDEX).setCellRenderer(
                    new PriorityRendererEditor());
            getColumn(BitTorrentTableFormat.PRIORITY_INDEX).setCellEditor(editor);

            getColumnExt(BitTorrentTableFormat.DOWNLOAD_INDEX).setMaxWidth(30);
            getColumnExt(BitTorrentTableFormat.DOWNLOAD_INDEX).setMinWidth(30);

            getColumnExt(BitTorrentTableFormat.PERCENT_INDEX).setMaxWidth(50);
            getColumnExt(BitTorrentTableFormat.PERCENT_INDEX).setMinWidth(50);

            getColumnExt(BitTorrentTableFormat.PRIORITY_INDEX).setMaxWidth(60);
            getColumnExt(BitTorrentTableFormat.PRIORITY_INDEX).setMinWidth(60);
        }
    }

    private class BitTorrentTableFormat extends AbstractTableFormat<TorrentFileEntryWrapper> {

        private static final int DOWNLOAD_INDEX = 0;

        private static final int NAME_INDEX = 1;

        private static final int SIZE_INDEX = 2;

        private static final int PERCENT_INDEX = 3;

        private static final int PRIORITY_INDEX = 4;

        public BitTorrentTableFormat() {
            super(I18n.tr("DL"), I18n.tr("Name"), I18n.tr("Size"), I18n.tr("%"), I18n
                    .tr("Priority"));
        }

        @Override
        public Object getColumnValue(TorrentFileEntryWrapper baseObject, int column) {
            switch (column) {
            case DOWNLOAD_INDEX:
                return baseObject;
            case NAME_INDEX:
                return baseObject.getPath();
            case SIZE_INDEX:
                return baseObject.getSize();
            case PERCENT_INDEX:
                return baseObject;
            case PRIORITY_INDEX:
                return baseObject;
            }
            throw new IllegalStateException("Unknown column:" + column);
        }

    }

    private class CheckBoxRendererEditor extends JCheckBox implements TableCellRenderer,
            TableCellEditor {

        /** List of cell editor listeners. */
        private final List<CellEditorListener> listenerList = new ArrayList<CellEditorListener>();

        private TorrentFileEntryWrapper currentWrapper;

        public CheckBoxRendererEditor() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            return getTableCellComponent(table, value, isSelected, column, column);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            return getTableCellComponent(table, value, isSelected, column, column);
        }

        private Component getTableCellComponent(JTable table, Object value, boolean isSelected,
                int row, int column) {
            if (value instanceof TorrentFileEntryWrapper) {
                currentWrapper = (TorrentFileEntryWrapper) value;

                if (torrent.isFinished()) {
                    setEnabled(false);
                    setSelected(currentWrapper.getProgress() == 1.0f
                            && currentWrapper.getPriority() > DONT_DOWNLOAD);
                } else if (currentWrapper.getProgress() == 1.0f) {
                    setSelected(true);
                    setEnabled(false);
                } else {
                    setSelected(((TorrentFileEntryWrapper) value).getPriority() != DONT_DOWNLOAD);
                    setEnabled(!torrent.isFinished());
                }
            } else {
                currentWrapper = null;
                setSelected(false);
            }

            return this;
        }

        @Override
        public void addCellEditorListener(CellEditorListener l) {
            if (!listenerList.contains(l)) {
                listenerList.add(l);
            }
        }

        @Override
        public void cancelCellEditing() {
            ChangeEvent event = new ChangeEvent(this);
            for (int i = 0, size = listenerList.size(); i < size; i++) {
                listenerList.get(i).editingCanceled(event);
            }
        }

        @Override
        public TorrentFileEntryWrapper getCellEditorValue() {
            return currentWrapper;
        }

        @Override
        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }

        @Override
        public void removeCellEditorListener(CellEditorListener l) {
            listenerList.remove(l);
        }

        @Override
        public boolean shouldSelectCell(EventObject anEvent) {
            return true;
        }

        @Override
        public boolean stopCellEditing() {
            cancelCellEditing();
            return true;
        }
    }

    private class PercentRenderer extends DefaultLimeTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);

            if (value instanceof TorrentFileEntryWrapper) {
                float percent = ((TorrentFileEntryWrapper) value).getProgress();
                if (torrent.isFinished()) {
                    if (percent == 1.0f
                            && ((TorrentFileEntryWrapper) value).getPriority() > DONT_DOWNLOAD) {
                        setText(I18n.tr("Done"));
                    } else {
                        setText("");
                    }
                } else if (((TorrentFileEntryWrapper) value).getPriority() == DONT_DOWNLOAD) {
                    setText("");
                } else {
                    setText((int) (percent * 100) + "%");
                }
            } else {
                setText("");
            }

            return this;
        }
    }

    private class PriorityRendererEditor extends JPanel implements TableCellRenderer,
            TableCellEditor {

        /** List of cell editor listeners. */
        private final List<CellEditorListener> listenerList = new ArrayList<CellEditorListener>();

        @Resource
        private Icon lowestPriorityIcon;

        @Resource
        private Icon normalPriorityIcon;

        @Resource
        private Icon highestPriorityIcon;

        @Resource
        private Font textFont;

        @Resource
        private Color fontColor;

        private final JButton button;

        private TorrentFileEntryWrapper currentWrapper;

        public PriorityRendererEditor() {
            super(new MigLayout("align 50%"));
            GuiUtils.assignResources(this);

            button = new JButton();
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setFont(textFont);
            button.setFocusPainted(false);
            button.setForeground(fontColor);
            FontUtils.underline(button);

            add(button);
        }

        public JButton getButton() {
            return button;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            return getTableCellComponent(table, value, isSelected, row, column);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            return getTableCellComponent(table, value, isSelected, row, column);
        }

        private Component getTableCellComponent(JTable table, Object value, boolean isSelected,
                int row, int column) {
            if (value instanceof TorrentFileEntryWrapper) {
                currentWrapper = (TorrentFileEntryWrapper) value;
                int priority = ((TorrentFileEntryWrapper) value).getPriority();
                if (((TorrentFileEntryWrapper) value).getProgress() == 1.0f) {
                    button.setIcon(null);
                    button.setText("");
                    // button.setText(I18n.tr("delete"));
                } else if (priority == DONT_DOWNLOAD) {
                    button.setIcon(null);
                    button.setText("");
                } else if (priority == LOWEST_PRIORITY) {
                    button.setIcon(lowestPriorityIcon);
                    button.setText("");
                } else if (priority == NORMAL_PRIORITY) {
                    button.setIcon(normalPriorityIcon);
                    button.setText("");
                } else if (priority == HIGHEST_PRIORITY) {
                    button.setIcon(highestPriorityIcon);
                    button.setText("");
                }
            } else {
                currentWrapper = null;
                button.setIcon(null);
                button.setText("");
            }
            return this;
        }

        @Override
        public void addCellEditorListener(CellEditorListener l) {
            if (!listenerList.contains(l)) {
                listenerList.add(l);
            }
        }

        @Override
        public void cancelCellEditing() {
            ChangeEvent event = new ChangeEvent(this);
            for (int i = 0, size = listenerList.size(); i < size; i++) {
                listenerList.get(i).editingCanceled(event);
            }
        }

        @Override
        public TorrentFileEntryWrapper getCellEditorValue() {
            return currentWrapper;
        }

        @Override
        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }

        @Override
        public void removeCellEditorListener(CellEditorListener l) {
            listenerList.remove(l);
        }

        @Override
        public boolean shouldSelectCell(EventObject anEvent) {
            return true;
        }

        @Override
        public boolean stopCellEditing() {
            cancelCellEditing();
            return true;
        }
    }

    public void addPropertyChangeListener( PropertyChangeListener listener )
    {
        support.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener( PropertyChangeListener listener )
    {
        support.removePropertyChangeListener(listener);
    }
    
    @Override
    public void handleEvent(TorrentEvent event) {
        if (event.getType() == TorrentEventType.STATUS_CHANGED || event.getType() == TorrentEventType.COMPLETED) {
            SwingUtils.invokeNowOrLater(new Runnable() {
                @Override
                public void run() {
                    List<TorrentFileEntry> fileEntries = torrent.getTorrentFileEntries();
                    for (TorrentFileEntry newEntry : fileEntries) {
                        TorrentFileEntryWrapper wrapper = eventList.get(newEntry.getIndex());
                        wrapper.setTorrentFileEntry(newEntry);
                    }
                }
            });
        }
    }
}
