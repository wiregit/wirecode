/**
 * auth: rsoule
 * file: HTTPManager.java
 * desc: This class handles the server side upload
 *       and download.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella;

import java.io.*;
import java.net.*;

public class HTTPManager {

    private Socket _socket;
    private String _filename;
    private int _index;
    private InputStream _istream;
    private ByteReader _br;
    private int _uploadBegin;
    private int _uploadEnd;
	private String userAgent    = null;
	//private String userAltruism = null;

    /**
     * @requires If isPush, "GIV " was just read from s.
     *           Otherwise, "GET " was just read from s.
     * @effects  Transfers the file over s <i>in the foreground</i>.
     *           Throws IOException if the handshake failed.
     */
    public HTTPManager(Socket s, MessageRouter router, Acceptor acceptor,
                       ActivityCallback callback, boolean isPush)
            throws IOException {
        _socket = s;

        String command=null;
        FileManager fm = FileManager.instance();

        try {
            //We set the timeout now so we don't block reading
            //connection strings.  If this is a GIV connection, we
            //reset the timeout immediately before downloading;
            //otherwise, it isn't touched again.
            _socket.setSoTimeout(SettingsManager.instance().getTimeout());
            //The try-catch below is a work-around for JDK bug 4091706.
            try {
                _istream = _socket.getInputStream();
            } catch (Exception e) {
                throw new IOException();
            }
            _br = new ByteReader(_istream);
            command = _br.readLine();   /* read in the first line */
            if (command==null)
            throw new IOException();
        }
        catch (IOException e) {          /* if there is a problem reading */
            throw e;                     /* must alert the appropriate */
        }                                /* person */


        //All IndexOutOfBoundsException and NumberFormatExceptions
        //are handled below. (They are converted to IOException.)
        try {
            if (!isPush) {
                //Expect "GET /get/0/sample.txt HTTP/1.0"
                                               /* I need to get the filename */
                String parse[] = HTTPUtil.stringSplit(command, '/');
                                               /* and the index, but i'm */
                                               /* upset this is way hackey */

                if (parse.length!=4)
                    throw new IOException();
                if (!parse[0].equals("get"))
                    throw new IOException();

                _filename = parse[2].substring(0, parse[2].lastIndexOf("HTTP")-1);
                _index = java.lang.Integer.parseInt(parse[1]);
                                                   /* is there a better way? */

                readRange();

                // Prevent excess uploads from starting
                //if ( callback.getNumUploads() >=
                while ( HTTPUploader.getUploadCount() >=
                        SettingsManager.instance().getMaxUploads() )
                {
					// If you can't blow away a "Gnutella" upload
					if ( ! HTTPUploader.checkForLowPriorityUpload(userAgent) )
					{
						// Report Limit Reached
                        HTTPUploader.doLimitReached(s);
                        return;
					}
                }

                HTTPUploader uploader;
                uploader = new HTTPUploader(s, _filename, _index,
                                            callback,
                                            _uploadBegin, _uploadEnd);
				uploader.setUserAgent(userAgent);
                Thread.currentThread().setName("HTTPUploader (normal)");
                uploader.run(); //Ok, since we've already spawned a thread.
            }

            else /* isPush */ {
                //Expect  "GIV 0:BC1F6870696111D4A74D0001031AE043/sample.txt\n\n"


                String next=_br.readLine();
                if (next==null || (! next.equals(""))) {
                    throw new IOException();
                }

                //1. Extract file index.  IndexOutOfBoundsException
                //   or NumberFormatExceptions will be thrown here if there's
                //   a problem.  They're caught below.
                int i=command.indexOf(":");
                _index=Integer.parseInt(command.substring(0,i));
                //2. Extract clientID.  This can throw
                //   IndexOutOfBoundsException or
                //   IllegalArgumentException, which is caught below.
                int j=command.indexOf("/", i);
                byte[] guid=GUID.fromHexString(command.substring(i+1,j));
                //3. Extract file name.  This can throw
                //   IndexOutOfBoundsException.
                _filename=command.substring(j+1);


                //Constructor to HTTPUploader checks that we can accept the
                //file.
                HTTPDownloader downloader;
                downloader = new HTTPDownloader(s, _filename, _index, guid,
                                                router, acceptor, callback);
                Thread.currentThread().setName("HTTPDownload (push)");
                downloader.run(); //Ok, since we've already spawned a thread.
            }
        } catch (IndexOutOfBoundsException e) {
            throw new IOException();
        } catch (NumberFormatException e) {
            throw new IOException();
        } catch (IllegalArgumentException e) {
            throw new IOException();
        } catch (IllegalAccessException e) {
            //We never requested the specified file!
            throw new IOException();
        }
    }

    public void readRange() throws IOException {
        String str = " ";

        _uploadBegin = 0;
        _uploadEnd = 0;

        while (true) {
            str = _br.readLine();

            if ( (str==null) || (str.equals("")) ){
                break;
            }

            if (str.indexOf("Range: bytes=") != -1) {
                String sub = str.substring(13);
                sub = sub.trim();   // remove the white space
                char c;
                c = sub.charAt(0);  // get the first character
                if (c == '-') {  // - n
                    String second = sub.substring(1);
                    second = second.trim();
                    _uploadEnd = java.lang.Integer.parseInt(second);
                }
                else {                // m - n or 0 -
                    int dash = sub.indexOf("-");

                    String first = sub.substring(0, dash);
                    first = first.trim();

                    _uploadBegin = java.lang.Integer.parseInt(first);

                    String second = sub.substring(dash+1);
                    second = second.trim();

                    if (!second.equals("")) {
                        _uploadEnd = java.lang.Integer.parseInt(second);
                    }
                }
            }


			// TODO2:  Implement some form of altruism
  			//if (str.indexOf("User-Altruism") != -1) {
  			//	userAltruism = str;
  			//}

			// check the User-Agent field of the header information
			if (str.indexOf("User-Agent:") != -1) {
				// check for netscape, internet explorer,
				// or other free riding downoaders
				if (SettingsManager.instance().getAllowBrowser() == false) {

					// if we are not supposed to read from them
					// throw an exception
					if( (str.indexOf("Mozilla") != -1) ||
						(str.indexOf("DA") != -1) ||
						(str.indexOf("Download") != -1) ||
						(str.indexOf("FlashGet") != -1) ||
						(str.indexOf("GetRight") != -1) ||
						(str.indexOf("Go!Zilla") != -1) ||
						(str.indexOf("Inet") != -1) ||
						(str.indexOf("MIIxpc") != -1) ||
						(str.indexOf("MSProxy") != -1) ||
						(str.indexOf("Mass") != -1) ||
						(str.indexOf("MyGetRight") != -1) ||
						(str.indexOf("NetAnts") != -1) ||
						(str.indexOf("NetZip") != -1) ||
						(str.indexOf("RealDownload") != -1) ||
						(str.indexOf("SmartDownload") != -1) ||
						(str.indexOf("Teleport") != -1) ||
						(str.indexOf("WebDownloader") != -1) ) {
							HTTPUploader.doFreeloaderResponse(_socket);
						    throw new IOException("Web Browser");
						}
					
				}
				userAgent = str.substring(11).trim();
			}
		}
	}

    public void shutdown() {
        try {
            _socket.close();
        }
        catch (IOException e) {
        }
    }
}




