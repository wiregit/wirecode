package org.limewire.ui.swing.options;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.util.I18n;

/**
 * Really Advanced Option View
 */
public class ReallyAdvancedOptionPanel extends OptionPanel {

    public static final int MULTI_LINE_LABEL_WIDTH = 440;
    
    private static final String FIREWALL = I18n.tr("Firewall");
    private static final String PROXY = I18n.tr("Proxy");
    private static final String NETWORK_INTERFACE = I18n.tr("Network Interface");
    private static final String PERFORMANCE = I18n.tr("Performance");
    private static final String BITTORRENT = I18n.tr("BitTorrent");
    private static final String FILTERING = I18n.tr("Filtering");
    private static final String SPAM = I18n.tr("Spam");
    
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private JList list;
    
    private Map<String, OptionPanel> panels = new HashMap<String, OptionPanel>();
    
    public ReallyAdvancedOptionPanel() {
        setLayout(new MigLayout("insets 15 15 15 15, fillx, gapy 10", "fill", "fill"));
        
        list = new JList();
        list.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setPreferredSize(new Dimension(150,500));
        list.addListSelectionListener(new ListSelectionListener(){
            @Override
            public void valueChanged(ListSelectionEvent e) {
                String name = (String) list.getModel().getElementAt(list.getSelectedIndex());
                cardLayout.show(cardPanel, name);
            }
        });
        
        cardLayout = new CardLayout();
        cardPanel = new JPanel();

        cardPanel.setLayout(cardLayout);
        createPanels();
        createList();
        
        add(new JLabel(I18n.tr("We recommend you don't touch these unless you really know what you're doing")), "span 2, wrap");
        add(list, "growy");
        add(cardPanel, "grow");
        
        list.setSelectedIndex(0);
    }
    
    private void createPanels() {      
        panels.put(FIREWALL, new FirewallOptionPanel());
        panels.put(PROXY, new ProxyOptionPanel());
        panels.put(NETWORK_INTERFACE, new NetworkInterfaceOptionPanel());
        panels.put(PERFORMANCE, new PerformanceOptionPanel());
        panels.put(BITTORRENT, new BitTorrentOptionPanel());
        panels.put(FILTERING, new FilteringOptionPanel());
        panels.put(SPAM, new SpamOptionPanel());
        
        
        cardPanel.add(panels.get(FIREWALL), FIREWALL);
        cardPanel.add(panels.get(PROXY), PROXY);
        cardPanel.add(panels.get(NETWORK_INTERFACE), NETWORK_INTERFACE);
        cardPanel.add(panels.get(PERFORMANCE), PERFORMANCE);
        cardPanel.add(panels.get(BITTORRENT), BITTORRENT);
        cardPanel.add(panels.get(FILTERING), FILTERING);
        cardPanel.add(panels.get(SPAM), SPAM);
        
        for(OptionPanel panel : panels.values()) 
            panel.setOpaque(false);
    }
    
    private void createList() {
        DefaultListModel model = new DefaultListModel();
        model.addElement(FIREWALL);
        model.addElement(PROXY);
        model.addElement(NETWORK_INTERFACE);
        model.addElement(PERFORMANCE);
        model.addElement(BITTORRENT);
        model.addElement(FILTERING);
        model.addElement(SPAM);
        
        list.setModel(model);
    }
    
    @Override
    void applyOptions() {
        for(OptionPanel panel : panels.values()) {
            panel.applyOptions();
        }
    }

    @Override
    boolean hasChanged() {
        for(OptionPanel panel : panels.values()) {
            if(panel.hasChanged())
                return true;
        }
        return false;
    }

    @Override
    void initOptions() {
        for(OptionPanel panel : panels.values()) {
            panel.initOptions();
        }
    }
}
