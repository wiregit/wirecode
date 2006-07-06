package com.limegroup.gnutella.metadata;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.util.IOUtils;
/**
 * Provide MP3 file info derived from the file header data
 *
 * @see #getLayer_*	  	-> mp3 layer (3 or "Layer III", etc)
 * @see #getMode      		-> mode type strings (stereo, dual channel, etc)
 * @see #getFrequency 		-> available frequencies (32000, 44100, etc) khz
 * @see #getVersion_* 		-> mp3 file version (2.0, or "MPEG Version 2.0")
 * @see #getHeaderBitRate 	-> constant bit rates(CBR) (128, 256, etc) kps
 * @see com.limegroup.gnutella.ByteOrder
 *
 * @author  cHANCE mOORE, ctmoore [at] gottapee [dot] com - 30 July 2002
 *			One of the Sindhis (both?), limewire team
 *			Gustav "Grim Reaper" Munkby, grd@swipnet.se
 *
 * TODO: add tests?
 */
//34567890123456789012345678901234567890123456789012345678901234567890123456789 
public final class MP3Info {

	/**
	 * the canonical localized mp3 file name
	 */	 
	private final String _file;
	
	/**
	 * 1st mp3 file's header; 4 bytes(combined) at beginning after any ID tags
	 * all the standard getters reference the header data
	 */	 
    private int _header;
    
    /**
     * represenation of the Variable bit rate header, if one exists
     * @see MP3Info$VBRHeader
     */
    private VBRHeader _vbrHeader; 

	/**
	 * Data holder for Xing variable bit rate headers
	 */
	final class VBRHeader {
		/**
		 * initially -1 as most fields are optional
		 * fields without getters are accessed through the MP3Info
		 */
		private int 	numFrames 	= -1;
	    private int 	numBytes 	= -1;
	    private int 	scale 		= -1;
	    private byte[] 	toc;
	    
	    /**
	     * @return int  suggested encoding quality, scaled 1 to 100
	     */
	    int getScale() {
		    return scale;
	    }

	    /**
	     * Table of Contents holds byte pos -> % of song complete, 1 to 100%
	     * @return byte[]  These bytes are raw java bytes, need to be "& 255"
	     */
	    byte[] getTableOfContents() {
		    return toc;
	    }

	    /**
	     * VBR header only returns a rate when frames and bytes are supplied
	     */
	    int getBitRate() {
		    if (numFrames != -1 && numBytes != -1) {
			    double tpf = 0;
			    switch (getLayerIndex()) {
				    case 1:
				    case 2:
				    tpf = 1152D;  
				    break;
				    case 3:
				    tpf = 384D;
			    } //new double[]{  -1, 1152, 1152, 384 }
			    tpf /= getFrequency();
				if( (getVersion_Numeric() == 2) || //MPEG_V_2
			    	(getVersion_Numeric() == 0) ) { //MPEG_V_25		    
			    	tpf /= 2;
				}
				return (int)( (numBytes * 8) / (tpf * numFrames * 1000) );
			}
		    
			return -1;		    
	    }

	    /**
	     *
	     */
	    int getLengthInSeconds() {
		    if (numFrames != -1) {
				double tpf = 0;
			    switch (getLayerIndex()) {
				    case 1:
				    case 2:
				    tpf = 1152D;  
				    break;
				    case 3:
				    tpf = 384D;
			    } //new double[]{  -1, 1152, 1152, 384 }
			    tpf /= getFrequency();
				if( (getVersion_Numeric() == 2) || //MPEG_V_2
			    	(getVersion_Numeric() == 0) ) { //MPEG_V_25		    
			    	tpf /= 2;
				}
				return (int)( tpf * numFrames );
			}
		    
			return -1;	
		}
	     
	}

    /**
     * An MPEG audio file is built up from smaller parts called frames, which
     * are generally independent items. Each frame has its own header and audio
     * data that follows. There is NO MPEG file header; therefore, you can cut
     * any part of MPEG file and play it correctly (cut on frame boundaries!),
     * excluding MPEG 1 Layer III frames which are often dependent on another.
     *
     * To read info about an MPEG file, you can find the first frame, read its
     * header and assume that the other frames are the same. Exceptions to this
     * are VBR (variable bit rate) and ABR (average bit rate) files. The frame
     * header is constituted by the very first four bytes (32bits) in a frame.
     * The first 11 bits are always set on(1) and they're called "frame sync".
     * Frame CRC is optional and 16 bits long; it follows the frame header.
     * After the CRC comes the audio data.
     *
     * ::EXAMPLE:: MP3 file header format (4 byte length or 32 bits)
     *               byte[4] = { -1, -5, 80, 108 } 
	 *     -1 << 24  +  -5 << 16  +  80 << 08  +  108 << 0    {HdrCRC}
	 *     11111111     11101010     00110000     11000000     {0000}
	 *     AAAAAAAA     AAABBCCD     EEEEFFGH     IIJJKLMM     {ZZZZ}
	 *
	 * Label, Position(bits), Description	 
	 * A (31-21) Frame sync 
	 *           All bits set (1)
	 * B (20,19) MPEG Audio version ID
	 *           00 - MPEG Ver 2.5, 01 - reserved, 10 - Ver 2, 11 - Ver 1
	 *           Note: MPEG Ver 2.5 is not official; bit # 20 indicates 2.5
	 * C (18,17) Layer description
	 *           00 - reserved, 01 - Layer III, 10 - Layer II, 11 - Layer I
	 * D    (16) Protection bit
	 *           0 - None, 1 - Protected by CRC (16bit crc follows header)
	 * E (15,12) Bitrate index, version and layer
	 *           bits V1,L1 V1,L2 V1,L3 V2,L1 V2, L2 & L3
	 * F (11,10) 
	 * G     (9) Padding bit   
	 *           0 - frame not padded, 1 - frame padded with one extra slot
	 *           Note: Padding is used to fit the bit rates exactly.
	 * H     (8) Private bit 
	 *           0 - not private, 1 - private
	 *           Note: May be freely used for other needs of an application.
	 * I   (7,6) Channel Mode
	 *           00 - Stereo, 01 - Joint stereo, 10 - Dual (Stereo), 11 - Mono
     * J   (5,4) Mode extension (Only if Joint stereo)
     *           Used to join data; bits dynamically generated by an encoder.
     * K     (3) Copyright
     *           0 - Audio is not copyrighted, 1 - Audio is marked copyrighted
     * L     (2) Original
     *           0 - Copy of original media, 1 - Original media 
     * M   (1,0) Emphasis
     *           00 - none, 01 - 50/15 ms, 10 - reserved, 11 - CCIT J.17
     * Z (32-35) CRC  !!OPTIONAL!!
     *           Note: NOT part of header, just appended on end when needed
     *      
     * We read in bytes from the beginning of the mp3 file looking for
	 *  the 4 byte header; we can't assume it starts at byte 0 because
	 *  ID3 tags may be prepended before the first valid header.
	 * The loop below strolls through buffered chunks of the file
	 *  looking for the header. As an optimization, we check the first
	 *  10 bytes initially as it may contain the header; if it doesn't
	 *  we then check the first 10 bytes for an ID3v2 header and fetch
	 *  the tag's length, skipping those bytes leading us directly
	 *  to the header. If neither are found, it's a brute force search.
	 *  With each chunk, we step forward one byte at a time, and test
	 *  the current byte plus the next 3 bytes for a valid mp3 header.
     *
     * @exception java.io.IOException mp3 fileName had no valid header
     */
    public MP3Info(String file) throws IOException {
        
        _file = file;
             //TODO:use 1.4 BufferMaps
        int i = 0; 			//reusable loop variant
		int pos = 0; 		//position in file, start at the beginning, duh...
		int adjustedEOB = 0;//adjusted end depending on actual bytes read
		int c = 0; 			//number of actual bytes read from file
		InputStream fis = null;
		byte[] buf = new byte[2048];

		try {
			fis = new BufferedInputStream(new FileInputStream(_file));
			
			//initially check the first few bytes
			c = fis.read(buf, 0, buf.length);
			if( c < 4 )
			    throw new IOException("early EOF, tiny file?");

			//check for ID3 tag
			//officially ID3, some tags incorrectly contain lowercase
			if ( (buf[0] == 'i' || buf[0] == 'I')
			  && (buf[1] == 'd' || buf[1] == 'D')
			  && (buf[2] == '3')
			   ) {
				//length of tag format is specified in the ID3v2 standard
				//28 bits amongst four bytes, first bit of each byte is 0
				i = buf[6] << 7 | buf[7] << 7 | buf[8] << 7 | buf[9];

				if (i > 0) { //skip indicated tag length and read header
					i += 10;
				}
				else if (i < 0) { //clear bad data
					i = 0;
				}
			}			
                    	
			endheadersearch:
			do {				
				if (pos < buf.length - 3) { //is first time?
					adjustedEOB = c - 3;
				}
				else {
					i = 0; //reset i except first time
					adjustedEOB = c; //already offset
				}
				for ( ; i < adjustedEOB; i++ ) {
					///////
					//quicktest, first byte must be 256
				    //quickly skip more expensive tests below, if possible
				    if (buf[i] != -1 || (buf[i+1] & 255) < 224) {
					    continue;
				    }

				    //build a header to test
					_header
					= ( ByteOrder.ubyte2int(buf[i+3])       )
					| ( ByteOrder.ubyte2int(buf[i+2]) <<  8 )
					| ( ByteOrder.ubyte2int(buf[i+1]) << 16 )
					| ( ByteOrder.ubyte2int(buf[i  ]) << 24 )
					;			
			        
			        // detect if valid header or just four different chars
			        if ( //(getFrameSync()        ==2047) && //tested above
			                 (getVersionIndex()		!=   1) &&
			                 (getLayerIndex()		!=   0) && 
			                 (getBitrateIndex()		!=   0) &&  
			                 (getBitrateIndex()		!=  15) &&
			                 (getFrequencyIndex()	!=   3) &&
			                 (getEmphasisIndex()	!=   2)   ) {
						pos += i;
						break endheadersearch;
            		}
				}

				//save last 3 bytes to test with next chunk
				// check trailing end of last chunk with start of new chunk
				// ie. last 3 bytes with first   new byte
				//     last 2 bytes with first 2 new bytes
				//     last 1 byte  with first 3 new bytes
				if (adjustedEOB != -1) { //skip when EOF
					buf[0] = buf[c-3];
					buf[1] = buf[c-2];
					buf[2] = buf[c-1];
				}
				pos += c - 3;
				
				c = fis.read(buf, 3, buf.length-3); //read next chunk
				if( c < 6 ) //not enough to make a difference
				    throw new IOException("MP3 Header not found.");
			} while (c != -1 && pos < 100000); //c is # of bytes read; until EOF
			//stop checking after first 100k, could be corrupted/infected file

			
		if (c == -1 || pos >= 100000) { // what the $#*!
			_header = 0;
			throw new IOException("MP3 header not found.");
		}

		
	//  Looking for the VBR
		// @see loadVBRHeeader for VBR format specifics
		// advance to check where Xing header would be
		// make sure we have enough data to test/work with
        // 120 is total 'possible' VBR length
        // 36  max bytes to skip
        // 3   is to cover length of VBR header
        int need = buf.length   // total we have.
                   - i          // where we're currently at
                   - 3          // VBR header
                   - 120        // max possible VBR length
                   - 36;        // max bytes to skip
  		if (need < 0) { //special case, we need more data
	  		need = -need; // flip need to be positive.
	  		i -= need; // shift our offset down by the amount we'll be moving
	  		int j = 0;
			for (; need < buf.length; j++, need++ ) { // shift data
		  		buf[j] = buf[need];
	  		}
	  		// IMPORTANT:
	  		// j is NOT equal to i for the following reason:
	  		// i is where we last stopped reading data from the buffer.
	  		// j is where the last bit of valid information in the buffer is.
	  		// we must continue reading from the buffer using i, but we must
	  		// fill up the the rest of the buffer from j on.
	  		
	  		//read more, starting at where we last have valid data.
			c = fis.read(buf, j, buf.length-j);
		}
		
		
		if ( getVersionIndex() == 3 ) { // mpeg version 1            
            i += (getModeIndex()==3  ?  21  :  36);
        }
        else { // mpeg version 2 or 2.5            
            i += (getModeIndex()==3  ?  23  :  21);
        }
		
        // Doh!! not all VBR files will have correct tags, it's optional
        switch (buf[i+0]) {
	        case  88: //'X':
	        	if (((buf[i+1] == 'i' || buf[i+1] == 'I')
		  		  && (buf[i+2] == 'n' || buf[i+2] == 'N')
		  		  && (buf[i+3] == 'g' || buf[i+3] == 'G')))
			// The Xing VBR headers always begin with the four chars "Xing" 
				loadXingHeader(buf, i+4);
			break;
	        case 86: //'V':
        		if ((buf[i+1] == 'B'
		       	  && buf[i+2] == 'R'
		       	  && buf[i+3] == 'I' ))
			//"VBRI" is a rarely used method of tagging Fhg encoded VBRs
				loadFhgHeader(buf, i+4);
			break;
						
			//case 73: //'I':
			//	if( buf[i+1] == 'n'
		    //   	 && buf[i+2] == 'f'
		    //   	 && buf[i+3] == 'o' )
			// LAME uses "Info" to tag LAME CBR/ABR files
			// there is no VBR data, but may provide useful LAME & ABR data
			// 4 skips VBR header, 109 skips dead Xing tag to reach 'LAME' tag
	    	//	loadLAMETag(buf, i+4+109);
			//break;
			
			//default:
			//true VBR file may not have a proper tag, to find out for sure
			//read every header to calculate true variable rate, length, etc

		} 
		
	    } finally { //cleanup
            IOUtils.close(fis);
        }
    }

    public int getBitRate() {

        if (hasVariableBitRate()) {
	        int i = _vbrHeader.getBitRate();
	        if (i != -1) {
		        return i;
	        }
        }

        
        long size = getFileSize();
        double mediumFrameSize = 
          ( (getLayerIndex() == 3 ? 12000 : 144000) * getHeaderBitRate() )
             /
          ( (double)getFrequency() );
                        
		/* FrameSizes formula
            mpeg v1: FrameSize =  12 * BitRate / SampleRate + Padding
            mpeg v2: FrameSize = 144 * BitRate / SampleRate + Padding
            bitrate is kbps & sample rate in Hz, so multiply BitRate by 1000
         */
        // get average frame size by dividing size by the # of frames
        int retInt = (int)( (size / (size/mediumFrameSize) * getFrequency())
                        / 
                        (getLayerIndex() == 3  ?  12000  :  144000) );


  //???? If computed bitrate is nonsensical, just use header bitrate 
        if (retInt < 1) {
            return getHeaderBitRate();
        }
        else {
	        return retInt;
        }
    }
	private int getBitrateIndex() {
		
		return _header >> 12 & 15;
	}
	/**
	 * Mp3 Emphasis
	 * -> "none", "50/15 ms", null, "CCIT J.17"
	 *
	 * @see #getEmphasisIndex
	 * @return java.lang.String string reprensentation of emphasis
	 */
	public String getEmphasis() {

		switch (getEmphasisIndex()) {

			case 0:
			return "none";
			
			case 1:
			return "50/15 ms";
			
			case 2:
			return null;

			case 3:
			return "CCIT J.17";
			
			default: //not an official tag
			return "<unknown>";
		}
	}
	private int getEmphasisIndex() {
		
		return _header & 3;
	}
    /**
     * Bytes in the mp3 file
     *
     * @return long
     */
    public long getFileSize() {

	    if (hasVariableBitRate() && _vbrHeader.numBytes != -1) {
		    return _vbrHeader.numBytes;
	    }
	    
	    return new File(_file).length();        
    }
    
	/**
	 * The frequency is dependent on bitrate index and MPEG version
	 * -> MPEG 2.5 - 32000, 16000,  8000
	 * -> MPEG 2   - 22050, 24000, 16000
	 * -> MPEG 1   - 44100, 48000, 32000
	 *
	 * @see #getVersionIndex
	 * @see #getFrequencyIndex
	 * @return the current frequency [8000-48000 Hz]
	 */
	public int getFrequency() {
                                   
	    switch (getVersionIndex()) {

	 		case 0: //MPEG 2.5 - 32000, 16000,  8000
			switch(getFrequencyIndex()) {
				case 0:
				return 11025; //!!32000 isn't correct!!

				case 1:
				return 12000; //!!16000 isn't correct!!

				case 2:
				return 8000;

				default:
				return -1;//error
			}

	 		case 1: //reserved
	 		return 0;

	 		case 2: //MPEG 2 - 22050, 24000, 16000
			switch(getFrequencyIndex()) {
				case 0:
				return 22050;

				case 1:
				return 24000;

				case 2:
				return 16000;

				default:
				return -1;//error
			}

	 		case 3: //MPEG 1 - 44100, 48000, 32000
			switch(getFrequencyIndex()) {
				case 0:
				return 44100;

				case 1:
				return 48000;

				case 2:
				return 32000;

				default:
				return -1;//error
			}
			
			default: //error
				return -1;	
		
 		}	                
	}
	private int getFrequencyIndex() {
		 
		return _header >> 10 & 3;  
	}
	/**
	 * Based on the bitrate index found in the header
	 * The header bit rate is based off the BITRATE_TABLE values using indexes
	 *  whereas the other bit rate is calculated directly without the table
	 *  both rates should be equal, excluding possible VBR discrepencies
	 *
	 * @see getBitRate
     * @return int The bitrate in between 8 - 448 Kb/s .
     */
	public int getHeaderBitRate() {

		int ind = -1;

		switch (getVersionIndex()) {
	        
	        case 0: //2.0
	        case 2: //2.5
				if( getLayer_Numeric() == 1 ) { // mpeg layer 1
					ind = 3;
		 		}
				else {// mpeg layer 2 & 3 if( layer == 2 || layer == 3 ) {
					ind = 4;
		    	}
	        break;
	        
	        case 1: //error or nothing
	        default:
	        return 0;
	        	         
	        case 3:
	        	ind = getLayer_Numeric()-1; 
			    //if( layer == MPEG_L_1 ) ind = 0;
			    //else if( layer == MPEG_L_2 ) ind = 1;
			    //else if( layer == MPEG_L_3 ) ind = 2;
		}
	  
		//if( bitrateIndex >= 0 && bitrateIndex <= 15 ) {
		try {
			short[] BITRATE_TABLE = { 
			   0,   0,   0,   0,   0, 
			  32,  32,  32,  32,   8,
			  64,  48,  40,  48,  16,
			  96,  56,  48,  56,  24,
			 128,  64,  56,  64,  32,
			 160,  80,  64,  80,  40,
			 192,  96,  80,  96,  48,
			 224, 112,  96, 112,  56,
			 256, 128, 112, 128,  64,
			 288, 160, 128, 144,  80,
			 320, 192, 160, 160,  96,
			 352, 224, 192, 176, 112,
			 384, 256, 224, 192, 128,
			 416, 320, 256, 224, 144,
			 448, 384, 320, 256, 160};	
		    return BITRATE_TABLE[getBitrateIndex()*5+ind];
		} catch (ArrayIndexOutOfBoundsException aiob) {
			return -1;
		}
		          
    }
	/** 
	 * Layer formula:
	 *  4 - layerIndex
	 * -> 1, 2, 3
	 *
	 * @see #getLayerIndex
	 * @return int the Layer [1-3] in small int format
	 */
	public int getLayer_Numeric() {
            
		return 4 - getLayerIndex();
	}
	/** 
	 * Layer formula:
	 *  4 - layerIndex
	 * -> null, "Layer III", "Layer II", "Layer I"
	 *
	 * @see #getLayerIndex
	 * @return java.lang.String representation of Mp3 Layer
	 */
	public String getLayer_String() {
            
		switch (getLayerIndex()) {

			//for those not in the know...don't worry
			//the "Layer " string is reused internally bytecode
			case 1:
			return "Layer " + "III";

			case 2:
			return "Layer " + "II";

			case 3:
			return "Layer " + "I";

			default:
			return "Layer " + "?";
		}
	}
	private int getLayerIndex() {
		
		return _header >> 17 & 3;  
	}
    /** 
     * Length in seconds formula:
     *  -> fileSize / (bitrate * 100 / 8)
     *
     * @see #getFileSize
     * @see #getHeaderBitRate
     * @return long mp3 seconds
     */
    public long getLengthInSeconds() {

	    if (hasVariableBitRate()) {
		    int i = _vbrHeader.getLengthInSeconds();
		    if (i != -1) {
			    return i;
		    }
	    }
	    
        return getFileSize() / (getHeaderBitRate()*1000 / 8);
    }
	/**
	 * Output channel information
	 *  "Stereo", "Joint Stereo", "Dual Channel", "Single Channel"
	 *
	 * @see #getModeIndex
	 * @return java.lang.String Display representation of playing mode
	 */
	public String getMode() {
            
		switch(getModeIndex()) {
		
			case 0:
			return "Stereo";
			
			case 1:
			return "Joint Stereo";
			
			case 2:
			return "Dual Channel";
			
			case 3:
			return "Single Channel";

			default:
			return "<unknown>";
		}
	}

	/**
	 * Mode extension joins information not used for stereo effect, thus
	 * reducing needed resources. These bits are dynamically determined by an
	 * encoder in Joint stereo mode. Complete frequency range of MPEG files is
	 * divided in 32 subbands. For Layer I & II, bits determine frequency range
	 * where intensity stereo is applied. For Layer III, two bits determine
	 * whether intensity stereo or m/s stereo is used.
	 *
	 *     Layer I and II     |          Layer III
	 * -----------------------------------------------------
	 * value |  Layer I & II  | Intensity stereo | MS stereo
	 * -----------------------|-----------------------------
	 *   00  | bands  4 to 31 |             off  |      off
	 *   01  | bands  8 to 31 |              on  |      off
	 *   10  | bands 12 to 31 |             off  |       on
	 *   11  | bands 16 to 31 |              on  |       on
	 */
    @SuppressWarnings("unused")
	 private int getModeExtIndex() {
		
		return _header >> 4 & 3;  
	}
     
	private int getModeIndex() {
		
		return _header >> 6 & 3;  
	}


    /**
     * FrameSize formula
     *   mpeg v1: FrameSize = 12 * BitRate / SampleRate + Padding               
     *   mpeg v2: FrameSize = 144 * BitRate / SampleRate + Padding
     *  bitrate is kbps and sample rate in Hz, so multiply BitRate by 1000   
     * Number of Frames formula
     *  mp3 file length in bytes / frame size
     *  the VBR header usually has the number of frames stored internally
     *
     * !!Results may not be precise as frame calculation is not always exact.
     *   Programs like Winamp occasionaly return slightly different results.
     *   For example, we don't exclude added frames like ID3 tags.
     *
     * @!deprecated  Not used internally
     * @return int frames calculated from mp3 (possible vbr) header
     */
    public int getNumberOfFrames() {

        if (hasVariableBitRate() && _vbrHeader.numFrames != -1) { 
	    	return _vbrHeader.numFrames;        
        }
        //getHeaderBitRate()
        //we round the calculation using (int) which produces a result
        //similiar to Winamp stats, but this breaks other calcs elsewhere
		return (int)
		 ( getFileSize()
           /
           ( getLayerIndex() == 3 ? 12000 : 144000 * getBitRate()
			 /
			 getFrequency() + (isPadded() ? getLayerIndex() == 3 ? 32 : 8 : 0)
           )
		 );	
    }


	/**
	 * VBR header containing Table of Contents and Quality
	 *
	 * @return MP3Info.VBRHeader  Variable Bit Rate header
	 */
	public MP3Info.VBRHeader getVBRHeader() {
            
		return _vbrHeader;
	}

    /**
     * Based on the version index
     * -> 2.5, 0.0, 2.0, 1.0
     *
     * @see #getVersionIndex
     * @return double the MPEG version number
     */
    public double getVersion_Numeric() {            
                        
	    switch (getVersionIndex()) {
	        
	        case 0:
	        return 2.5;
	        
	        case 1:
	        default:
	        return 0.0;
	        
	        case 2:
	        return 2.0;
	        
	        case 3:
	        return 1.0;
        }        
	}

    /**
     * Based on the version index
     * -> "MPEG Version 2.5", null, "MPEG Version 2.0", "MPEG Version 1.0"
     *
     * @see #getVersionIndex
     * @return java.lang.String representation of version
     */
    public String getVersion_String() {            
                        
	    switch (getVersionIndex()) {
	        //for those not in the know...don't worry
			//the "Layer " string is reused internally bytecode
	        case 0:
	        return "MPEG Version " + "2.5";
	        
	        case 1:
	        return null;
	        
	        case 2:
	        return "MPEG Version " + "2.0";
	        
	        case 3:
	        return "MPEG Version " + "1.0";

	        default:
	        return "MPEG Version " + "?";
        }        
	}

	private int getVersionIndex()  {
		
		return _header >> 19 & 3;
	}

	/**
	 * Whether the bits per frame are not constant
	 * 
	 * @return True if this file has a VBR
     */
    public boolean hasVariableBitRate() {
	    
        return _vbrHeader != null;
    }

	/**
     * Whether the copyright bit is flagged in the mp3 header
     *
     * @return boolean true if flag found
     */
    public boolean isCoprighted() {
	    
	    return (_header >> 3 & 1) != 0;
	}

	/**
     * Whether the original bit is flagged in the mp3 header
     *
     * @return boolean true if flag found
     */
    public boolean isOriginal() {
	    
	    return (_header >> 2 & 1) != 0;
	}

	/**
     * Whether padding bit is set; Padding is used to fit bit rates exactly.
     * :Example: 128k 44.1kHz layer II uses a lot of 418 bytes and some of
     *  417 bytes long frames to get the exact 128k bitrate. For Layer I 
     *  slot is 32 bits long, Layer II and Layer III slot is 8 bits long.
     *
     * @return boolean true if flag found 
     */
    public boolean isPadded() {
	    
	    return (_header >> 9 & 1) != 0;
	}

	/**
     * Whether the private bit is flagged in the mp3 header
     *
     * @return boolean true if flag found
     */
    public boolean isPrivate() {
	    
	    return (_header >> 8 & 1) != 0;
	}

	/**
     * Whether the protection bit is flagged in mp3 header
     *  Indicates CRC; 16 bit crc follows file header
     *
     * @return boolean true if flag found
     */
    public boolean isProtected() {

	    //CRC protection is ON when bit is not set
	    return (_header >> 16 & 1) == 0;
	}

	/**
     * Whether this MP3 is embedded in a WAV file
     *
     * RIFF(Resource Interchange File Format) is a tagged file structure
     * developed for multimedia resource files.  The structure of RIFF
     * is similar to the structure of an ElectronicArts IFF file. RIFF is
     * not actually a file format itself (since it does not represent a
     * specific kind of information), but its name contains the words
     * `interchange file format' in recognition of its roots in IFF. 
     *
     * ::the beginning of file will start as follows::
     *   RIFF õY
1 WAVE fmt 
	 *   AAAA BBBB CCCC DDDD
	 *
	 * A   4 bytes  RIFF Tag
	 * B   4 bytes  File Size  -  Ignored for this test
	 * C   4 bytes  WAVE Tag
	 * D   4 bytes  fmt name
	 *
     * @return boolean true if file is marked as Replay Gain RIFF-WAV
     *   		!!Doesn't gurantee file is a valid or playable RIFF-WAV
     */
    public boolean isRiffWav() {

	    //the results of this test are not persisted on the object
	    //there's little benefit for a method that may never be used
	    boolean result = false;
	    FileInputStream fis = null;
	    try { //safety
			fis = new FileInputStream(_file);
			byte[] buffer = new byte[16];
			fis.read(buffer); 
			result =
			     buffer[ 0] == 'R'
			  && buffer[ 1] == 'I'
			  && buffer[ 2] == 'F'
			  && buffer[ 3] == 'F'
			  && buffer[ 8] == 'W'
			  && buffer[ 9] == 'A'
			  && buffer[10] == 'V'
			  && buffer[11] == 'E'
			  && buffer[12] == 'f'
			  && buffer[13] == 'm'
			  && buffer[14] == 't'
			  && buffer[15] == ' ';
	    } catch(IOException ignored) {
	        // not a riff.
	    } finally {
	        if( fis != null ) {
	            try {
	                fis.close();
                } catch(IOException ioe) {}
            }
        }
	    
	    return result; 
	}
	/**
	 * The LAME tag is not really all that lame
	 * Added when using the LAME opensource MP3 encoder, the tag provides
	 * song details, most importatnly for us would be any bit rate info.
	 * 
	 * ::Example::  LAME Tag
	 *
	 *   0005 LAME3.90. õY
a kY
1 õY
q 7Y
3 dY
2 pY
i 0
	 *   ZZZZ AA.....AA BCDD DDEE FFGH IIIJ KLLL LMMN N
	 *
	 * Z   4 bytes  VBR quality
	 *				 the last part of Xing tag, is included in LAME tag
	 * A  20 bytes  LAME Tag
	 *               may not use all 20 bytes; example: 'LAME3.12 (beta 6)'
	 * B   1 byte   LAME Tag revision + VBR method
	 *               no vbr/cbr, abr, vbr-old/vbr-rh, vbr-mtrh, vbr-new/vbr-mt
	 * C   1 byte   Lowpass filter value
	 *               divided by 100
	 * D   4 bytes  Replay Gain
	 *               see http://www.david.robinson.org/replaylevel/
	 * E   2 bytes  Radio Replay Gain
	 *               required to make all tracks equal loudness
	 * F   2 bytes  Audiophile Replay Gain
	 *               required to give ideal listening loudness
	 * G   1 byte   Encoding flags + ATH Type
	 *               --nspsytune, --nssafejoint, --nogap (combination)
	 * H   1 byte   ABR {specified bitrate} or {minimal bitrate}
	 *               if the file is NOT an ABR file then (CBR/VBR)
	 * I   3 bytes  Encoder delays
	 *               samples added at start & padded at end complete last frame
	 * J   1 byte   Misc
	 *               noise shaping, stereo mode, optimal quality, sample freq
	 * K   1 byte   MP3 Gain
	 *               mp3 amplification factor
	 * L   4 bytes  Music Length
	 *               file size minus additional tags
	 * M   2 bytes  Music CRC
	 *               CRC-16 of mp3 music data as made originally by LAME
	 * N   2 bytes  CRC-16 of LAME Tag
	 *               CRC-16 of first 190 bytes of the VBR header frame
	 *
	 * @deprecated
	 */
	 /*
	private void loadLAMETag (byte buf[], int offset) {
	
		try {
	        
							 
		}	
		catch (Throwable t) {} //bombed trying to build LAME tag
	} */

	/** 
	 * MPEG files frame bitrates may change in a variable bitrate (VBR). Each
	 * frame is encoded at a different rate to maximaize quality/file size.
	 *  1. by bitrate switching: each frame may be created differently.
	 *  2. by bit reservoir: bits borrowed/given to other frames where needed.
	 *
	 * !!NOTE!! All Fhg files encode 160kb into the first mp3 header
	 *
	 * ::Example::  Fhg VBR Tag, bytes after header flag are optional flag
	 *
	 *   VBRI 01949 0212 36-K pS12 0102 j80d 0....1
	 *   AAAA BBBB  CCDD DDEE EEFF GGGG HHII I....I
	 *
	 * A   4 bytes  Header Tag
	 *              "VBRI"
	 * B   4 bytes  Header / Version Flags
	 *              4 possible flags, determines what data follows (last bit)
	 *
	 *   OPTIONAL   C-G according to flags
	 * C   2 bytes  VBR Scale
	 *              A VBR quality indicator: 0=best 100=worst 
	 * D   4 bytes  # of Bytes Per Frame / Stream Size
	 *
	 * E   4 bytes  MPEG File Frame Size
	 *
	 * F   2 bytes  Number of seek offsets
	 *
	 * G   4 bytes  unknown
	 *
	 * H   2 bytes  offset "stride" (number of frames between offsets)
	 *
	 * I F*2 bytes  Table of Contents (TOC)
	 *              seek offsets 0-F (from beginning of file)
	 *              
	 */
	private void loadFhgHeader (byte buf[], int pos) {	        
		_vbrHeader = new MP3Info.VBRHeader();
		
		 _vbrHeader.scale = ByteOrder.ubyte2int(buf[pos+=2]);
			
		 _vbrHeader.numBytes = ((ByteOrder.ubyte2int(buf[++pos]) << 24) 
		    				  + (ByteOrder.ubyte2int(buf[++pos]) << 16)
		    				  + (ByteOrder.ubyte2int(buf[++pos]) <<  8) 
		    				  + (ByteOrder.ubyte2int(buf[++pos])     ));
		 _vbrHeader.numFrames =((ByteOrder.ubyte2int(buf[++pos]) << 24)
		    				  + (ByteOrder.ubyte2int(buf[++pos]) << 16)
		    				  + (ByteOrder.ubyte2int(buf[++pos]) <<  8) 
		    				  + (ByteOrder.ubyte2int(buf[++pos])     ));

		/* TOC ignored  [format is sketchy]
		byte b = (byte)ByteOrder.ubyte2int(buf[pos+=3]);			
		if((b & (byte)(1 << 2 )) != 0 ) {
			_vbrHeader.seek =((ByteOrder.ubyte2int(buf[++pos]) << 8)
		    			    + (ByteOrder.ubyte2int(buf[++pos])     ))
		    _vbrHeader.toc = new byte[100];
		    System.arraycopy(buf, ++pos, _vbrHeader.toc, 0, f);
		    
		}
		*/
	}

	/** 
	 * MPEG files frame bitrates may change in a variable bitrate (VBR). Each
	 * frame is encoded at a different rate to maximaize quality/file size.
	 *  1. by bitrate switching: each frame may be created differently.
	 *  2. by bit reservoir: bits borrowed/given to other frames where needed.
	 *
	 * ::Example::  Xing VBR Tag, bytes after header flag are optional flag
	 *
	 *   Xing 0007 0254 1236 12...21 0058
	 *   AAAA BBBB CCCC DDDD FF...FF GGGG
	 *
	 * A   4 bytes  Header Tag
	 *              "Xing" or possibly "FBRI" {"Info" is also possible in CBR}
	 * B   4 bytes  Header Flags
	 *              4 possible flags, determines what data follows (last bit)
	 *
	 *   OPTIONAL   C-G according to flags
	 * C   4 bytes  MPEG File Frame Size
	 *
	 * D   4 bytes  # of Bytes Per Frame / Stream Size
	 *
	 * F 100 bytes  Table of Contents (TOC)
	 *              TOC is a 100-byte array that tells a player how many 256ths
	 *              of the file to jump to find a particular point -in percent.
	 *              :Example: jump to half-way (50%) point in 3,000,000 byte
	 *              file, then look at the 50th entry in the TOC which is 130.
	 *              Seek to 130/256*3000000=1523438th byte, scan to next frame.
	 * G   4 bytes  VBR Scale
	 *              A VBR quality indicator: 0=best 100=worst 
	 */
	private void loadXingHeader (byte buf[], int offset) {
		_vbrHeader = new MP3Info.VBRHeader();
		byte b = (byte)ByteOrder.ubyte2int(buf[offset+=3]);
		if ((b & 1) != 0) {	
	     _vbrHeader.numFrames =((ByteOrder.ubyte2int(buf[++offset]) << 24)
		    				  + (ByteOrder.ubyte2int(buf[++offset]) << 16)
		    				  + (ByteOrder.ubyte2int(buf[++offset]) <<  8) 
		    				  + (ByteOrder.ubyte2int(buf[++offset])     ));
		}
		if((b & 2) != 0 ) {
		 _vbrHeader.numBytes = ((ByteOrder.ubyte2int(buf[++offset]) << 24) 
		    				  + (ByteOrder.ubyte2int(buf[++offset]) << 16)
		    				  + (ByteOrder.ubyte2int(buf[++offset]) <<  8) 
		    				  + (ByteOrder.ubyte2int(buf[++offset])     ));
		}
		if((b & 4) != 0 ) {
		    _vbrHeader.toc = new byte[100];
		    System.arraycopy(buf, ++offset, _vbrHeader.toc, 0, 100);
		    offset += 99;
		}
		if((b & 8) != 0 ) {
			_vbrHeader.scale = ByteOrder.ubyte2int(buf[offset+=4]);
        }
	}
}
