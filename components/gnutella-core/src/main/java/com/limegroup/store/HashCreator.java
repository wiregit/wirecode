package com.limegroup.store;

import java.io.File;
import java.io.IOException;
import java.net.URL;


import com.limegroup.gnutella.URN;

public class HashCreator {

    URN urn;
    URL url;
    long length;
    File name;
    
    public HashCreator(String filePath ){
        
    }
    
    
    public static String usage = "java HashCreator [filenames]";
    
    public static void main( String args[] ) throws InterruptedException{
        if( args.length == 0 ) {
            System.out.println(usage);
            return;
        }
        
        System.out.println("Creating URNs");
        for( int i = 0; i < args.length; i++ ){
            String fileName = args[i];
            try {
                URN urn = URN.createSHA1Urn(new File(fileName));
                System.out.println("URN for:" + fileName + "  " + urn.toString());
                System.out.println( new File(fileName).length());
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }
    }
}
