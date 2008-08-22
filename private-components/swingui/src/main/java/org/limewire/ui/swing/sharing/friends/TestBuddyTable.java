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
        BuddyItem item = new MockBuddyItem("Anthony", 122);     
        eventList.add(item);
        
        item = new MockBuddyItem("Mike", 78);     
        eventList.add(item);
        
        item = new MockBuddyItem("Jim", 58);     
        eventList.add(item);
        
        item = new MockBuddyItem("Lisa", 2);     
        eventList.add(item);
    
        item = new MockBuddyItem("Stephanie", 87);     
        eventList.add(item);
        
        item = new MockBuddyItem("George", 357);     
        eventList.add(item);
        
        item = new MockBuddyItem("John", 44);     
        eventList.add(item);
        
        item = new MockBuddyItem("Luke", 58);     
        eventList.add(item);
        
        item = new MockBuddyItem("Rob", 41);     
        eventList.add(item);
        
        item = new MockBuddyItem("Jen", 6516);     
        eventList.add(item);
        
        item = new MockBuddyItem("Julie", 516);     
        eventList.add(item);
        
        item = new MockBuddyItem("Terry", 84);     
        eventList.add(item);
        
        item = new MockBuddyItem("Zack", 6);     
        eventList.add(item);
        
        
        
        item = new MockBuddyItem("Jack", 0);     
        eventList.add(item);
        
        item = new MockBuddyItem("Liza", 0);     
        eventList.add(item);
        
        item = new MockBuddyItem("William", 0);     
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
