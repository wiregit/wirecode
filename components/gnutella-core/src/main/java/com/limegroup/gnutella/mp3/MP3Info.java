package com.limegroup.gnutella.mp3;

import java.io.*;
import com.limegroup.gnutella.ByteOrder;

/**
 *  MUCH MUCH inspiration from : <br>
 *    Gustav "Grim Reaper" Munkby <br>
 *    http://home.swipnet.se/grd/<br>
 *    grd@swipnet.se <br>
 *  The main motivation for this class was to find the bitrate of an mp3 file.
 *  This was accomplished in addition to setup of infrastructure for more
 *  advanced MP3Info querying.
 *  I also learned very much from http://www.dv.co.yu/mpgscript/mpeghdr.htm .
 */
public final class MP3Info {
    
    private final int MAX_KBS_TO_CHECK = 1024 * 200;  // 200 KBs
    private final int HEADER_SIZE_IN_BYTES = 4;
    private final int VBITRATE_SIZE_IN_BYTES = 12;
    private final boolean DEBUG = false;

    // the mp3 file info
    private String _fileName;
    private long _fileSize;

    // the representation of the header, where all info is ultimately
    // derived....
    private MP3Header _header;

    // the represenation of the Variable bit rate.  i don't know how useful this
    // is.
    private VBitRate _vBitRate;

    // just to know what kind of file it is.....
    private boolean _isVariableBitRate = false;

    // given a valid mp3 file, the constructor is not dumb.  it tries to load
    // the throwing an exception is valid.
    /** @exception java.io.IOException Thrown if mp3FileName is not a valid mp3
     *  file.
     */
    public MP3Info(String mp3FileName) throws IOException {
        
        // the one and only header i'll use....
        _header = new MP3Header();

        _fileName = mp3FileName;
        RandomAccessFile file = new RandomAccessFile(_fileName, "r");
        _fileSize = file.length();

        long pos = 0; // start at the beginning, duh....
        byte[] headerBytes = new byte[HEADER_SIZE_IN_BYTES];

        file.seek(pos); //begining of file
        while ( (pos < (MAX_KBS_TO_CHECK)) && // while reasonable...
                (pos < _fileSize) ) {         // & possible

            file.readFully(headerBytes); 

            _header.loadHeader(headerBytes);
            if (_header.isValidHeader()) 
                break; // if load was successful, STOP!
            
            pos++; // invalid header, increment the file pointer, try again
            file.seek(pos); 
        }

        debug("MP3Info(): found valid header at position " + pos);
        if (_header.isValidHeader() == false)
            throw new IOException("Invalid MP3 File!");

        //ASSERT: I have the valid header now.

        pos += 4; //increment the filePointer beyond the header bytes
        
        if ( _header.getVersionIndex()==3 ) {  // mpeg version 1
            
            if( _header.getModeIndex()==3 ) pos += 17; // Single Channel
            else                            pos += 32;
            
        } else {                             // mpeg version 2 or 2.5
            
            if( _header.getModeIndex()==3 ) pos +=  9; // Single Channel
            else                            pos += 17;
            
        }
        debug("MP3Info(): in attempt to find Variable bit rate, position = " + 
              pos);

        file.seek(pos);
        byte[] vBitRateBytes = new byte[VBITRATE_SIZE_IN_BYTES];
        file.readFully(vBitRateBytes); //read the vBitRate bits.
        _vBitRate = new VBitRate();
        _isVariableBitRate = _vBitRate.loadHeader(vBitRateBytes);

        debug("MP3Info(): is this a Variable bit rate? " + _isVariableBitRate);
        
        file.close();        
    }


    public int getBitRate() {

        int retInt = 0;

        if (_isVariableBitRate) {
            // get average frame size by deviding fileSize 
            // by the number of frames
            double medFrameSize = 
            (double) _fileSize / (double) getNumberOfFrames();

            debug("MP3Info.getBitRate(): _fileSize = " + _fileSize);
            debug("MP3Info.getBitRate(): medFrameSize = " + medFrameSize);

            /* Now using the formula for FrameSizes which looks different,
               depending on which mpeg version we're using, for mpeg v1:
               
               FrameSize = 12 * BitRate / SampleRate + Padding (if there is 
               padding)
               
               for mpeg v2 the same thing is:
               
               FrameSize = 144 * BitRate / SampleRate + Padding (if there is 
               padding)
               
               remember that bitrate is in kbps and sample rate in Hz, so we 
               need to multiply our BitRate with 1000.
               
               For our purpose, just getting the average frame size, will 
               make the padding obsolete, so our formula looks like:
               
               FrameSize = (mpeg1?12:144) * 1000 * BitRate / SampleRate;
            */

            final double numerator = medFrameSize *
            (double)_header.getFrequency();
            double denominator = 1000.0;
            if (_header.getLayerIndex() == 3)
                denominator *= 12.0;
            else
                denominator *= 144.0;
            final double tempVal = numerator / denominator;


            debug("MP3Info.getBitRate(): tempVal = " + tempVal);
            retInt = (int)tempVal;
        }

        /* If the computed bit rate (above) is nonsensical, just get the 
         * bit rate from the header.  We aren't convinced how useful 
         * Variable Bit Rate is yet.
         */
        if (retInt < 1)
            retInt = _header.getBitRate();

        return retInt; 
    }


    /** This method is not useful, so it hasn't been made public.  We still need
     *  to figure out this whole variable bit rate mumbo jumbo.
     */
    private int getNumberOfFrames() {

        if (!_isVariableBitRate) {

            /* Now using the formula for FrameSizes which looks different,
               depending on which mpeg version we're using, for layer 1:
               
               FrameSize = 12 * BitRate / SampleRate + Padding (if there 
               is padding) for layer 2 & 3 the same thing is:
               
               FrameSize = 144 * BitRate / SampleRate + Padding (if there 
               is padding) remember that bitrate is in kbps and sample rate 
               in Hz, so we need to multiply our BitRate with 1000.
               
               For our purpose, just getting the average frame size, will 
               make the padding obsolete, so our formula looks like:
               
               FrameSize = (layer1?12:144) * 1000 * BitRate / SampleRate;
            */
            
            double medFrameSize = 
            (double)( 
                     ( (_header.getLayerIndex()==3) ? 12 : 144 ) *
                     (
                      (1000.0 * (double)_header.getBitRate() ) /
                      (double)_header.getFrequency()
                      )
                     );
            
            return (int)( ((double)_fileSize)/medFrameSize );
            
        }
        else return (int)_vBitRate.getNumberOfFrames();        
    }



    public void debug(String out) {
        if (DEBUG)
            System.out.println(out);
    }
        

    /*
       public static void main(String argv[]) throws Exception{
       for (int i = 0; i < argv.length; i++) {
       MP3Info mp3Info = new MP3Info(argv[i]);
       System.out.println("Bitrate for file " +
       argv[i] + " is " + mp3Info.getBitRate());
       System.out.println("-------------------------------------");
       }
       }
    */

        
    private class MP3Header {

        /** INTernal Header representation.  
         *  The last 4 bytes is all we care about.
         */
        private long _intHeader;
        private long getHeader() {
            return _intHeader;
        }

        private MP3Header() {
        }


        /* Loads the header from the given bytes.
         * @return true if the input bits represent a valid header format.
         */
        public boolean loadHeader(byte[] headerBits) {
            // this thing is quite interesting, it works like the following
            // c[0] = 00000011
            // c[1] = 00001100
            // c[2] = 00110000
            // c[3] = 11000000
            // the operator << means that we'll move the bits in that direction
            // 00000011 << 24 = 00000011000000000000000000000000
            // 00001100 << 16 =         000011000000000000000000
            // 00110000 << 24 =                 0011000000000000
            // 11000000       =                         11000000
            //                +_________________________________            
            //                  00000011000011000011000011000000
            
            /*            _intHeader = (long)(
                          ( (headerBits[0] & 255) << 24) |
                          ( (headerBits[1] & 255) << 16) |
                          ( (headerBits[2] & 255) <<  8) |
                          ( (headerBits[3] & 255)      )
                          ); 
            */

            long a, b, c, d, temp;
            temp = ByteOrder.ubyte2int(headerBits[3]);
            a = temp;
            temp = ByteOrder.ubyte2int(headerBits[2]);
            b = temp << 8;
            temp = ByteOrder.ubyte2int(headerBits[1]);
            c = temp << 16;
            temp = ByteOrder.ubyte2int(headerBits[0]);
            d = temp << 24;
            _intHeader = a | b | c | d;

            return isValidHeader();
        }

        // This function is a supplement to the loadHeader
        // function, the only purpose is to detect if the
        // header loaded by loadHeader is a valid header
        // or just four different chars
        public boolean isValidHeader() {
            
            return ( ((getFrameSync()      & 2047)==2047) &&
                     ((getVersionIndex()   &    3)!=   1) &&
                     ((getLayerIndex()     &    3)!=   0) && 
                     ((getBitrateIndex()   &   15)!=   0) &&  
                     // due to lack of support of the .mp3 format
                     // no "public" .mp3's should contain information
                     // like this anyway... :)
                     ((getBitrateIndex()   &   15)!=  15) &&
                     ((getFrequencyIndex() &    3)!=   3) &&
                     ((getEmphasisIndex()  &    3)!=   2)    );

        }

        /** @return the MPEG version [1.0-2.5]
         */
        public double getVersion() {            
            // a table to convert the indexes into
            // something informative...
            double table[] = {
                2.5, 0.0, 2.0, 1.0
            };
            
            // return modified value
            return table[getVersionIndex()];            
        }

        /** @return the Layer [1-3]
         */
        public int getLayer() {
            // when speaking of layers there is a 
            // cute coincidence, the Layer always
            // equals 4 - layerIndex, so that's what
            // we will return
            return ( 4 - getLayerIndex() );            
        }

        /** @return the current frequency [8000-48000 Hz]
         */
        public int getFrequency() {
            
            // a table to convert the indexes into
            // something informative...
            int table[][] = {
                {32000, 16000,  8000}, //MPEG 2.5
                {    0,     0,     0}, //reserved
                {22050, 24000, 16000}, //MPEG 2
                {44100, 48000, 32000}  //MPEG 1
            };
            
            // the frequency is not only dependent of the bitrate index,
            // the bitrate also varies with the MPEG version
            debug("MP3Header.getFrequency():" + 
                  table[getVersionIndex()][getFrequencyIndex()]);
            return table[getVersionIndex()][getFrequencyIndex()];            
        }

        // the purpose of getMode is to get information about
        // the current playing mode, such as:
        // "Joint Stereo"
        public String getMode() {
            String retString = null;

            // here you could use a array of strings instead
            // but I think this method is nicer, at least
            // when not dealing with that many variations
            switch(getModeIndex()) {
            default:
                retString = "Stereo";
                break;

            case 1:
                retString = "Joint Stereo";
                break;
                
            case 2:
                retString = "Dual Channel";
                break;
                
            case 3:
                retString = "Single Channel";
                break;
            }
            
            return retString;
        }

        
        public int getFrameSync()     { return (int)((getHeader()>>21) & 2047);};
        public int getVersionIndex()  { return (int)((getHeader()>>19) & 3);  };
        public int getLayerIndex()    { return (int)((getHeader()>>17) & 3);  };
        public int getProtectionBit() { return (int)((getHeader()>>16) & 1);  };
        public int getBitrateIndex()  { return (int)((getHeader()>>12) & 15); };
        public int getFrequencyIndex(){ return (int)((getHeader()>>10) & 3);  };
        public int getPaddingBit()    { return (int)((getHeader()>> 9) & 1);  };
        public int getPrivateBit()    { return (int)((getHeader()>> 8) & 1);  };
        public int getModeIndex()     { return (int)((getHeader()>> 6) & 3);  };
        public int getModeExtIndex()  { return (int)((getHeader()>> 4) & 3);  };
        public int getCoprightBit()   { return (int)((getHeader()>> 3) & 1);  };
        public int getOrginalBit()    { return (int)((getHeader()>> 2) & 1);  };
        public int getEmphasisIndex() { return (int)((getHeader()    ) & 3);  };


        
        /** @return The bitrate in between 8 - 448 Kb/s .
         *  @exception Exception Thrown when bitrate determination fails.
         *  @param file The Name of the mp3 file (implicit) you want the 
         *  bitrate of.
         */
        public int getBitRate() {
            
            int retInt = 0;
            
            int table[][][] = {
                {       //MPEG 2 & 2.5
                    //Layer 3
                    {0,8,16,24,32,40,48,56,64,80,96,112,128,144,160,0},
                    //Layer 2
                    {0,8,16,24,32,40,48,56,64,80,96,112,128,144,160,0},
                    //Layer I
                    {0,32,48,56,64,80,96,112,128,144,160,176,192,224,256,0}
                },
                {       //MPEG 1
                    //Layer III
                    {0,32,40,48,56,64,80,96,112,128,160,192,224,256,320,0},
                    //Layer II
                    {0,32,48,56,64,80,96,112,128,160,192,224,256,320,384,0},
                    //Layer I
                    {0,32,64,96,128,160,192,224,256,288,320,352,384,416,448,0}
                }
            };
        
            retInt = 
            table[(getVersionIndex()&1)][(getLayerIndex()-1)][getBitrateIndex()];
            
            return retInt;
        }
    }
    

    private class VBitRate {
        
        public final int FRAMES_FLAG    = 0x0001;
        public final int BYTES_FLAG     = 0x0002;
        public final int TOC_FLAG       = 0x0004;
        public final int VBR_SCALE_FLAG = 0x0008;
           
        private long _frames;
        public long getNumberOfFrames() {
            debug("VBitRate.getNumberOfFrames(): returning " + _frames);
            return _frames;
        }


        public VBitRate() {
        }


        /** I do not think this method works correctly.  Fortunately, I don't
         *  think it is ever *REALLY* needed, as MP3Header.getBitRate works fine
         *  and accurately.
         */
        public boolean loadHeader (byte inputHeader[]) {
            
            boolean retBool = false;

            // The Xing VBR headers always begin with the four
            // chars "Xing" so this tests wether we have a VBR
            // header or not
            if ((new String(inputHeader, 0, 4)).equals("Xing")) {                
                _frames = -1;
                retBool = false;                
            }
            else {
    
                // now we will get the flags and number of frames,
                // this is done in the same way as the FrameHeader
                // is generated in the CFrameHeader class
                // if you're curious about how it works, go and look
                // there
                
                // here we get the flags from the next four bytes
                /*                long flags = (long)(
                                  ( (inputHeader[4] & 255) << 24) |
                                  ( (inputHeader[5] & 255) << 16) |
                                  ( (inputHeader[6] & 255) <<  8) |
                                  ( (inputHeader[7] & 255)      )
                                  ); 
                */
                long flags;
                {
                    long a, b, c, d, temp;
                    temp = ByteOrder.ubyte2int(inputHeader[7]);
                    a = temp;
                    temp = ByteOrder.ubyte2int(inputHeader[6]);
                    b = temp << 8;
                    temp = ByteOrder.ubyte2int(inputHeader[5]);
                    c = temp << 16;
                    temp = ByteOrder.ubyte2int(inputHeader[4]);
                    d = temp << 24;
                    flags = a | b | c | d;
                }


                // if this tag contains the number of frames, load
                // that number into storage, if not something will
                // be wrong when calculating the bitrate and length
                // of the music
                if ((flags & FRAMES_FLAG) != 0) {
                    /*
                      _frames = (
                      ( (inputHeader[ 8] & 255) << 24) |
                      ( (inputHeader[ 9] & 255) << 16) |
                      ( (inputHeader[10] & 255) <<  8) |
                      ( (inputHeader[11] & 255)      )
                      ); 
                    */
                    long a, b, c, d, temp;
                    temp = ByteOrder.ubyte2int(inputHeader[11]);
                    a = temp;
                    temp = ByteOrder.ubyte2int(inputHeader[10]);
                    b = temp << 8;
                    temp = ByteOrder.ubyte2int(inputHeader[9]);
                    c = temp << 16;
                    temp = ByteOrder.ubyte2int(inputHeader[8]);
                    d = temp << 24;
                    _frames = a | b | c | d;

                    // if it gets this far, everything went according
                    // to plans, so we should return true!
                    retBool = true;
                } 
                else {
                    
                    // returning -1 so an error would be obvious
                    // not many people would believe in a bitrate
                    // -21 kbps :)
                    _frames = -1;
                    
                    // this function was returning false before
                    // as there is an error occuring, but in that
                    // case the bitrate wouldn't be unbelievable
                    // so that's why I changed my mind and let it
                    // return true instead
                    retBool = true;
                }
            }

            return retBool;
        }       
    }
    
    
}
