package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.gui.*;
import javax.swing.JOptionPane;

/**
 * Interactive test for the GUI's search capabilities.
 */
public class SearchViewTest {    
    public static void main(String args[]) {
        //Bring up application
        System.out.println("Bringing up GUI");
        com.limegroup.gnutella.gui.Main.main(new String[0]);
        
        //Do dummy search
        SearchView sv=MainFrame.instance().getSearchView();
        byte[] guid=sv.triggerSearch("testing");
        Assert.that(guid!=null, "Search didn't happen");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) { }

        //Add normal dummy result
        Response[] responses=new Response[1];
        responses[0]=new Response(1l, 10l, "testing small file.txt");
        QueryReply qr=new QueryReply(guid, (byte)5, 6346,
                                     new byte[4], Integer.MAX_VALUE,
                                     responses, new byte[16]);
        sv.handleQueryReply(qr);
        String msg="Do you see a query reply for 'testing small file.txt'?";
        int result = Utilities.showConfirmDialog(sv, msg, "Assertion Check",
                                                 JOptionPane.YES_NO_OPTION);
        Assert.that(result==JOptionPane.OK_OPTION);

        //Add mix of good and bad results
        responses=new Response[3];
        responses[0]=new Response(1l, 10l,
                                  "testing another different small file.txt");
        responses[1]=new Response(2l, 0xFFFFFFFFl,
                                  "testing a hugh monster file.txt");
        responses[2]=new Response(0xFFFFFFFFl, 100,
                                  "testing a hugh monster index.txt");
        qr=new QueryReply(guid, (byte)5, 6346,
                                     new byte[4], Integer.MAX_VALUE,
                                     responses, new byte[16]);
        sv.handleQueryReply(qr);
        msg="Do you see a query reply for another small file only?";
        result = Utilities.showConfirmDialog(sv, msg, "Assertion Check",
                                                 JOptionPane.YES_NO_OPTION);
        Assert.that(result==JOptionPane.OK_OPTION);


        msg="Testing passed if no exceptions in console.  Quit?";
        result = Utilities.showConfirmDialog(sv, msg, "Confirm",
                                                 JOptionPane.YES_NO_OPTION);
        if (result==JOptionPane.OK_OPTION)
            System.exit(0);        
    }
}
