package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.util.Random;

public class FileManagerBenchmark {
    public static final String DIRECTORY="c:\\junk";
    public static final int QUERIES=1000;

    public static void main(String args[]) {
        SettingsManager settings=SettingsManager.instance();
        settings.setExtensions("txt");
        settings.setDirectories("");
        FileManager fm=FileManager.instance();
        
        System.out.println("Adding files...");
        long startTime=System.currentTimeMillis();
        fm.addDirectory(DIRECTORY);
        long stopTime=System.currentTimeMillis();
        long elapsed=(stopTime-startTime)/1000;
        System.out.println("Added "+fm.getNumFiles()+" in "+elapsed+" seconds\n");

        Random rand=new Random();
        System.out.println("Querying files...");
        startTime=System.currentTimeMillis();
        long hits=0;
        for (int i=0; i<QUERIES; i++) {
            int n=rand.nextInt() % 1000;
            String query="file"+String.valueOf(n);
            QueryRequest qr=new QueryRequest((byte)5, 0, query);
            Response[] responses=fm.query(qr);
            if (responses!=null)
                hits+=responses.length;
        }
        stopTime=System.currentTimeMillis();
        elapsed=(stopTime-startTime)/1000;
        System.out.println("Delivered "+hits+" query results in "+elapsed+" seconds\n"); 
    }
}
