package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.util.Random;
import java.io.*;

public class FileManagerBenchmark {
    /** Location of files. */
    public static final String DIRECTORY="c:\\Documents and Settings\\crohrs\\My Documents\\data\\filemanager\\simulated\\";
    /** The queries to try, in increasing order of nastiness */
    public static final String[] QUERIES={"beethoven",                   //no luck
                                          "gloria", "cannibal minds",    //some luck
                                          "the you", "mp3"};             //lots of luck
    /** The sample to try per query. */
    public static final int QUERY_TRIES=3000;

    public static void main(String args[]) {
        //Set up FileManager with no files.
        SettingsManager settings=SettingsManager.instance();
        settings.setExtensions(SettingsInterface.DEFAULT_EXTENSIONS);
        settings.setDirectories("");
        FileManager fm=FileManager.instance();
        if (! (new File(DIRECTORY).exists())) {
            System.out.println("Directory doesn't exist");
            return;
        }
        
        //Time directory scan.
        System.out.println("Adding files...");
        long startTime=System.currentTimeMillis();
        fm.addDirectory(DIRECTORY);
        long stopTime=System.currentTimeMillis();
        long elapsed=stopTime-startTime;
        System.out.println("Added "+fm.getNumFiles()+" in "+elapsed+" msecs\n");


        //Time query.
        for (int i=0; i<QUERIES.length; i++) {
            String query=QUERIES[i];
            QueryRequest qr=new QueryRequest((byte)5, 0, query);
            System.out.println("Querying files for \""+query+"\"...");

            startTime=System.currentTimeMillis();
            long hits=0;
            for (int tries=0; tries<QUERY_TRIES; tries++) {            
                Response[] responses=fm.query(qr);
                if (responses!=null)
                    hits+=responses.length;
            }
            stopTime=System.currentTimeMillis();

            elapsed=stopTime-startTime;
            System.out.println("Delivered "+hits+" query results in "+elapsed+" msecs\n"); 
        }
    }

    /*

This is the base line performance

Adding files...
Added 1190 in 4417 msecs

Querying files for "beethoven"...
Delivered 0 query results in 190 msecs

Querying files for "sinead"...
Delivered 4000 query results in 230 msecs

Querying files for "mccartney paul"...
Delivered 10000 query results in 501 msecs

Querying files for "and she"...
Delivered 16000 query results in 5318 msecs
****This is somewhat slow because "and" matches the directory name!

Querying files for "mp3"...
Delivered 1185000 query results in 33197 msecs
*****This is somwhat slow because 'mp3, '

==>Adding the special case optimization to FileManager.search and adding "," to
the delimeters doesn't really help.


--------------------------------------------

This is the old performance

Adding files...
Added 1190 in 3135 msecs

Querying files for "beethoven"...
Delivered 0 query results in 12999 msecs

Querying files for "sinead"...
Delivered 5000 query results in 14130 msecs

Querying files for "mccartney paul"...
Delivered 11000 query results in 13770 msecs

Querying files for "and she"...
Delivered 18000 query results in 16163 msecs

Querying files for "mp3"...
Delivered 1185000 query results in 32747 msecs

======================================== Small Test ====================================

Old algorithm

Querying files for "beethoven"...
Delivered 0 query results in 3054 msecs

Querying files for "gloria"...
Delivered 9000 query results in 3275 msecs

Querying files for "cannibal minds"...
Delivered 3000 query results in 3275 msecs

Querying files for "the you"...
Delivered 6000 query results in 4286 msecs

Querying files for "mp3"...
Delivered 324000 query results in 8171 msecs



New algo

Adding files...
Added 109 in 170 msecs

Querying files for "beethoven"...
Delivered 0 query results in 100 msecs

Querying files for "gloria"...
Delivered 9000 query results in 381 msecs

Querying files for "cannibal minds"...
Delivered 3000 query results in 420 msecs

Querying files for "the you"...
Delivered 3000 query results in 772 msecs

Querying files for "mp3"...
Delivered 324000 query results in 5558 msecs


     */
}
