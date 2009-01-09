package org.limewire.ui.swing.util;

import java.text.Format;

import javax.swing.JComboBox;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.AutoCompleteSupport;
import ca.odell.glazedlists.swing.EventComboBoxModel;
import ca.odell.glazedlists.swing.EventListModel;
import ca.odell.glazedlists.swing.EventSelectionModel;
import ca.odell.glazedlists.swing.EventTableModel;

public class GlazedListsSwingFactory {
    
    public static <E> EventSelectionModel<E> eventSelectionModel(EventList<E> source) {
        return new EventSelectionModel<E>(source, false);
    }
    
    public static <E> AutoCompleteSupport<E> install(JComboBox comboBox, EventList<E> items) {
        return AutoCompleteSupport.install(comboBox, items, false);
    }
    
    public static <E> AutoCompleteSupport<E> install(JComboBox comboBox, EventList<E> items, TextFilterator<E> filterator) {
        return AutoCompleteSupport.install(comboBox, items, filterator, false);
    }
    
    public static <E> AutoCompleteSupport<E> install(JComboBox comboBox, EventList<E> items, TextFilterator<E> filterator, Format format) {
        return AutoCompleteSupport.install(comboBox, items, filterator, format, false);
    }
    
    public static <E> EventComboBoxModel eventComboBoxModel(EventList<E> source) {
        return new EventComboBoxModel<E>(source, false);
    }
    
    public static <E> EventListModel eventListModel(EventList<E> source) {
        return new EventListModel<E>(source, false);
    }
    
    public static <E> EventTableModel eventTableModel(EventList<E> source, TableFormat<E> tableFormat) {
        return new EventTableModel<E>(source, tableFormat, false);
    }
    
    

}
