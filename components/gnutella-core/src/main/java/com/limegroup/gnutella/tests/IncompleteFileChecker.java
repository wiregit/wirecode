package com.limegroup.gnutella.tests;

import java.io.*;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.Assert;
import com.sun.java.util.collections.*;

/**
 * Checks that files that an incomplete file is not corrupt.  That is, all
 * blocks mentioned in downloads.dat must be the same as the original file.
 * Syntax:
 * <pre>
 *     SYNTAX: IncompleteFileChecker  <incompleteDir> <completeDir>");
 * </pre> 
 */
public class IncompleteFileChecker {
    public static void main(String args[]) {
        //Parse arguments.
        if (args.length!=2) 
            syntaxError();
        File incompleteDir=new File(args[0]);
        File completeDir=new File(args[1]);
        if (! (incompleteDir.isDirectory() && completeDir.isDirectory()))
            syntaxError();

        //Open downloads.dat
        IncompleteFileManager ifm=null;
        try {
            ifm=readSnapshot(new File(incompleteDir, "downloads.dat"));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("ERRROR: could not read snapshot file from \""
                               +args[0]+"\".");
            syntaxError();
        }
        System.out.println("Incomplete files: "+ifm.toString()+"\n\n");
        
        //For each file in incomplete directory...
        String files[]=incompleteDir.list();
        for (int i=0; i<files.length; i++) {
            try {
                //Try to match it up with a complete file.
                File incomplete=new File(incompleteDir, files[i]);
                File complete=new File(
                    completeDir, 
                    stripIncompletePrefix(incomplete.getName()));
                if (! complete.exists())
                    continue;

                //And check consistency.
                check(incomplete, complete, ifm);
            } catch (IOException e) { 
            } catch (IllegalArgumentException e) { }
        }
    }

    /** 
     * Terminates with an error message. 
     */
    private static void syntaxError() {
        System.err.println("SYNTAX: IncompleteFileChecker "
                           +" <incompleteDir> <completeDir>");
        System.exit(1);
    }


    /**
     * Reads the IncompleteFileManager from the given downloads.dat file,
     * ignore the list of downloader.  
     *
     * @exception IOException the file couldn't be read for any reason
     */
    private static IncompleteFileManager readSnapshot(File file) 
            throws IOException {
        try {
            ObjectInputStream in=new ObjectInputStream(
                new FileInputStream(file));

            in.readObject();  //skip List of ManagedDownloader
            return (IncompleteFileManager)in.readObject();            
        } catch (ClassCastException e) {
            throw new IOException();
        } catch (ClassNotFoundException e) {
            throw new IOException();
        }
    }

    private static String stripIncompletePrefix(String name) 
            throws IllegalArgumentException {
        if (! name.startsWith("T-"))
            throw new IllegalArgumentException();
        
        try {
            int i=name.indexOf('-', 2);
            return name.substring(i+1);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException();
        }
    }
 
    /**
     * @exception IOException couldn't read incomplete or complete
     */
    public static void check(File incomplete, 
                             File complete, 
                             IncompleteFileManager ifm) throws IOException {
        //For each block of ifm...
        Iterator /* of Interval */ iter=ifm.getBlocks(incomplete);
        System.out.println("Testing \""+complete.getName()+"\":");
        while (iter.hasNext()) {
            Interval block=(Interval)iter.next();
            System.out.println("   block "+block+"...");

            //Skip to start of block...
            FileInputStream incompleteStream=new FileInputStream(incomplete);
            FileInputStream completeStream=new FileInputStream(complete);
            skipN(incompleteStream, block.low);
            skipN(completeStream, block.low);

            //...and compare one byte at a time.
            for (int i=block.low; i<block.high; i++) {
                int bI=incompleteStream.read();
                int bC=completeStream.read();

                Assert.that(bI==bC, "Byte "+i+" differs."
                    +"Incomplete file \""+incomplete+"\" got "+bI+", while"
                    +"ccomplete file \""+complete+"\" got "+bC+".  "
                    +"IFM block "+block);

                if (bI==-1 || bC==-1)
                    break;
            }

            incompleteStream.close();
            completeStream.close();
        }
    }

    /** Skips n bytes in "in". */
    private static void skipN(FileInputStream in, long n) throws IOException {
        long remaining=n;
        while (remaining>0)
            remaining-=in.skip(remaining);
    }
}
