
/*
 * auth: rsoule
 * file MDFiveCalculator.java
 * desc: This class does a simple MD5 Calculation in a file.
 *
 */

package com.limegroup.gnutella;

import java.io.*;

public class MDFiveCalculator {

	private static final int SIZE_TO_READ = 1024;

	public MDFiveCalculator() {}

	public int getValue(File file) {

		// check to see if the file exists.
		// if it doesn't, return 0
		if ( !file.exists() )
			return 0;

		// get ready to read the file
		FileInputStream fis;
		try {
			fis = new FileInputStream(file);
		}
		catch (FileNotFoundException e) {
			return 0;
		}
		
		int value=0;

		int len = 256;
		byte[] filebytes = new byte[len];
		int read=-1;

		// read in each of the bytes...
		for (int i = 0; i < SIZE_TO_READ; i++) {
			// read a byte
			try {
				read = fis.read();
			}
			catch (IOException e) {
				break;
			}
			// byte b = = fis.read();
			
			value += read;
			
		}

		// right now this is just returning the summation 
		// of the first SIZE_TO_READ (1024) bytes.
		return value;

	}

}
