package org.limewire.ui.swing.sharing.friends;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;

public class TestBuddyTable extends JPanel {

    BuddyNameTable table;
    EventList<BuddyItem> eventList;
    
    public TestBuddyTable() {
        
        setLayout(new BorderLayout());
        
        eventList = GlazedLists.threadSafeList(new BasicEventList<BuddyItem>());
        table = new BuddyNameTable(eventList, new BuddyTableFormat());
               
        createBuddy();
    
        add(new JScrollPane(table), BorderLayout.CENTER);
    }
    
    private void createBuddy() {
        BuddyItem item = new MockBuddyItem("Anthony", true, 122);     
        eventList.add(item);
        
        item = new MockBuddyItem("Mike", true, 78);     
        eventList.add(item);
        
        item = new MockBuddyItem("Jim", true, 58);     
        eventList.add(item);
        
        item = new MockBuddyItem("Lisa", true, 2);     
        eventList.add(item);
    
        item = new MockBuddyItem("Stephanie", true, 87);     
        eventList.add(item);
        
        item = new MockBuddyItem("George", true, 357);     
        eventList.add(item);
        
        item = new MockBuddyItem("John", true, 44);     
        eventList.add(item);
        
        item = new MockBuddyItem("Luke", true, 58);     
        eventList.add(item);
        
        item = new MockBuddyItem("Rob", true, 41);     
        eventList.add(item);
        
        item = new MockBuddyItem("Jen", true, 6516);     
        eventList.add(item);
        
        item = new MockBuddyItem("Julie", true, 516);     
        eventList.add(item);
        
        item = new MockBuddyItem("Terry", true, 84);     
        eventList.add(item);
        
        item = new MockBuddyItem("Zack", true, 6);     
        eventList.add(item);
        
        
        
        item = new MockBuddyItem("Jack", false, 0);     
        eventList.add(item);
        
        item = new MockBuddyItem("Liza", false, 0);     
        eventList.add(item);
        
        item = new MockBuddyItem("William", false, 0);     
        eventList.add(item);
        
    }
    
    public static void main(String args[]) {
        JFrame f = new JFrame();
        f.setSize(180,400);
        
        TestBuddyTable table = new TestBuddyTable();
        
        f.add(table);
        
        f.setDefaultCloseOperation(2);
        f.setVisible(true);
    }
}
