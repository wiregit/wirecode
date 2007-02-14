package org.limewire.rudp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ManagedThread;

/**
 * A standalone program for testing UDPConnections across machines.
 */
public class UStandalone {
	
    private static final Log LOG = LogFactory.getLog(UStandalone.class);


	/** A boolean that tracks whether the read thread was successful */
    private static boolean readSuccess = false;

	public static boolean echoClient(UDPConnection usock, int numBytes) 
	  throws IOException {
		OutputStream ostream = usock.getOutputStream();
		InputStream  istream = usock.getInputStream();

		readSuccess = false;
		ClientReader reader = new ClientReader(istream, numBytes);
		reader.start();

		for (int i = 0; i < numBytes; i++) {
			ostream.write(i % 256);
			if ( (i % 1000) == 0 ) 
				LOG.debug("Write status: "+i);
		}
		LOG.trace("Done write");
		
		try { reader.join(); } catch (InterruptedException ie){}
        LOG.debug("Done echoClient test");

		return readSuccess;
	}

	static class ClientReader extends ManagedThread {
		InputStream istream;
		int         numBytes;

		public ClientReader(InputStream istream, int numBytes) {
		    super ("ClientReader");
			this.istream = istream;
			this.numBytes = numBytes;
		}

		public void run() {
			int rval;
			LOG.debug("Begin read");

			try {
				for (int i = 0; i < numBytes; i++) {
					rval = istream.read();
					if ( rval != (i % 256) ) {
						LOG.debug("Error on read expected: "+i
						  +" received: "+rval);
						break;
					} else
						LOG.trace("Properly recieved: "+i);
					if ( (i % 1000) == 0 ) 
						LOG.debug("Read status: "+i);
				}
				readSuccess = true;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			LOG.debug("Done read");
		}
	}

	public static boolean echoServer(UDPConnection usock, int numBytes) 
	  throws IOException {
		OutputStream ostream = usock.getOutputStream();
		InputStream  istream = usock.getInputStream();

		boolean success = false;

		int rval;
		for (int i = 0; i < numBytes; i++) {
			rval = istream.read();
			if ( rval != (i % 256) ) {
				LOG.debug("Error on read expected: "+i
				  +" received: "+rval);
				return false;
			} 
			if ( (i % 1000) == 0 ) 
				LOG.debug("Echo status: "+i);
			ostream.write(rval);
		}
		success = true;
		LOG.trace("Done echo");
		try { Thread.sleep(1*1000); } catch (InterruptedException ie){}
        LOG.debug("Done echoServer test");
		return success;
	}

	public static boolean echoClientBlock(UDPConnection usock, int numBlocks) 
	  throws IOException {
		OutputStream ostream = usock.getOutputStream();
		InputStream  istream = usock.getInputStream();

		readSuccess = false;
		ClientBlockReader reader = new ClientBlockReader(istream, numBlocks);
		reader.start();
	
		// setup transfer data
		byte bdata[] = new byte[512];
		for (int i = 0; i < 512; i++)
			bdata[i] = (byte) (i % 256);

		for (int i = 0; i < numBlocks; i++) {
			ostream.write(bdata, 0, 512);
			if ( (i % 8) == 0 ) 
				LOG.debug("Write status: "+i*512+
                  " time:"+System.currentTimeMillis());
		}
		LOG.trace("Done write");
		
		try { reader.join(); } catch (InterruptedException ie){}
        LOG.debug("Done echoClientBlock test");
		return readSuccess;
	}

	static class ClientBlockReader extends ManagedThread {
		InputStream istream;
		int         numBlocks;

		public ClientBlockReader(InputStream istream, int numBlocks) {
			this.istream   = istream;
			this.numBlocks = numBlocks;
		}

		public void run() {
			LOG.debug("Begin read");

			byte bdata[] = new byte[512];

            int btest;
			int len;
            int printTarget = 0;
			try {
				for (int i = 0; i < 512 * numBlocks; i += len) {
					len = istream.read(bdata);

                    if ( len != 512 ) 
                        LOG.debug("Abnormal data size: "+len+" loc: "+i);

					for (int j = 0; j < len; j++) {
                        btest = bdata[j] & 0xff;
						if ( btest != ((i+j) % 256) ) {
							LOG.debug("Error on read expected: "+(i+j)
							  +" received: "+bdata[j]);
							return;
						} 
						if ( (i+j) > printTarget ) { 
							LOG.debug("Read status: "+i+
                              " time:"+System.currentTimeMillis());
                            printTarget = i+j+1024;
                        }
					}
				}
				readSuccess = true;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			LOG.debug("Done read");
		}
	}

	public static boolean echoServerBlock(UDPConnection usock, int numBlocks) 
	  throws IOException {
		OutputStream ostream = usock.getOutputStream();
		InputStream  istream = usock.getInputStream();

		byte bdata[] = new byte[512];

		boolean success = false;

        int btest;
		int len = 0;
		for (int i = 0; i < 512 * numBlocks; i += len) {
			len = istream.read(bdata);

            if ( len != 512 ) 
                LOG.debug("Abnormal data size: "+len+" loc: "+i);

			for (int j = 0; j < len; j++) {
                btest = bdata[j] & 0xff;
				if ( btest != ((i+j) % 256) ) {
					LOG.debug("Error on echo expected: "+(i+j)
					  +" received: "+bdata[j]);
					return false;
				} 
				if ( ((i+j) % 1024) == 0 ) 
					LOG.debug("Echo status: "+i+
                      " time:"+System.currentTimeMillis());
			}
            ostream.write(bdata, 0, len);
		}
		success = true;
		LOG.trace("Done echoBlock");
		try { Thread.sleep(1*1000); } catch (InterruptedException ie){}
        LOG.debug("Done echoServerBlock test");
		return success;
	}

    public static boolean unidirectionalClient(UDPConnection usock, 
	  int numBytes) throws IOException {
        OutputStream ostream = usock.getOutputStream();

		boolean success = false;

        int i = 0;
        for (i = 0; i < numBytes; i++) {
            ostream.write(i % 256);
            if ( (i % 1000) == 0 ) 
                LOG.debug("Write status: "+i);
        }
		success = true;
        LOG.debug("Write reached: "+i);
        
        try { Thread.sleep(1*1000); } catch (InterruptedException ie){}
        LOG.debug("Done unidirectionalClient test");
		return success;
    }

    public static boolean unidirectionalServer(UDPConnection usock, 
	  int numBytes) throws IOException {
        InputStream  istream = usock.getInputStream();

		boolean success = false;
        int rval;
        int i = 0;
        for (i = 0; i < numBytes; i++) {
            rval = istream.read();
            if ( rval != (i % 256) ) {
                LOG.debug("Error on read expected: "+i
                  +" received: "+rval);
                break;
            } else {
                if ( (i % 1000) == 0 ) 
                    LOG.debug("Read Properly received: "+i);
            }
        }
		success = true;
        LOG.debug("Read reached: "+i);
        
        try { Thread.sleep(1*1000); } catch (InterruptedException ie){}
        LOG.debug("Done unidirectionalServer test");
		return success;
    }

}
