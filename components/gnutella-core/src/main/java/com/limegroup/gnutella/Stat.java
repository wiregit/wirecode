package com.limegroup.gnutella;

import java.net.*;
import java.io.*;
import java.util.*;


/**
 *The puppose of this class is to keep some stastics of the system.
 *It will run on a thread that sleeps for about half and hour
 * and then gathers stsastics and dumps them into a file with the 
 * current time and date.
 */
public class Stat implements Runnable{

    GregorianCalendar cal;
    PrintWriter pr;
    Date d;
    ConnectionManager man;
    
    public Stat(ConnectionManager cm){
	cal = new GregorianCalendar();	    
	man = cm;
	try{
	    pr = new PrintWriter(new BufferedWriter(new FileWriter("stats.log",true)));
	}
	catch(Exception e){
	    ActivityCallback callback=cm.getCallback();
	    if (callback!=null)
		callback.error("Could not create output file for statistics logging.");
	}
    }
   
    public void run(){
	while(true){
	    try{
		Thread.sleep(108000000); //sleep for 30 minutes
		//System.out.println("Sumeet: thread waking up");
	    }
	    catch (Exception e){
	    }

	    Date d = cal.getTime();
	    
	    try{
		//System.out.println("Sumeet : Writing to file");
		pr.println(d);//write the time
		pr.println("Total messages seen : " + man.total);
		pr.println("Ping requests : " + man.PReqCount + " (" + ((man.PReqCount*100)/man.total) +"%)" );
		pr.println("Ping replies : " + man.PRepCount + " (" + ((man.PRepCount*100)/man.total) +"%)" );
		pr.println("Query requests : " + man.QReqCount + " (" + ((man.QReqCount*100)/man.total) +"%)" );
		pr.println("Query replies : " + man.QRepCount + " (" + ((man.QRepCount*100)/man.total) +"%)" );
		pr.println("Push requests : " + man.pushCount + " (" + ((man.pushCount*100)/man.total) +"%)" );
		//pr.println("Total files : " + man.totalFiles );
		//pr.println("Total size : " + man.totalSize );
		pr.println("-------------------------------------------------" );
		pr.flush();
		//reset all values
		man.PReqCount=0;
		man.PRepCount=0;
		man.QRepCount=0;
		man.QReqCount=0;
		man.total =0;
	    }
	    catch (Exception e) {
	    ActivityCallback callback=man.getCallback();
	    if (callback!=null)
		callback.error("Could not write statistics data to log.");		
	    }
	}
    }
}


