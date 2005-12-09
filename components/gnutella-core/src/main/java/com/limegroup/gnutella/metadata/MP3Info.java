padkage com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOExdeption;

import dom.limegroup.gnutella.ByteOrder;
/**
 * Provide MP3 file info derived from the file header data
 *
 * @see #getLayer_*	  	-> mp3 layer (3 or "Layer III", etd)
 * @see #getMode      		-> mode type strings (stereo, dual dhannel, etc)
 * @see #getFrequendy 		-> available frequencies (32000, 44100, etc) khz
 * @see #getVersion_* 		-> mp3 file version (2.0, or "MPEG Version 2.0")
 * @see #getHeaderBitRate 	-> donstant bit rates(CBR) (128, 256, etc) kps
 * @see dom.limegroup.gnutella.ByteOrder
 *
 * @author  dHANCE mOORE, ctmoore [at] gottapee [dot] com - 30 July 2002
 *			One of the Sindhis (aoth?), limewire tebm
 *			Gustav "Grim Reaper" Munkby, grd@swipnet.se
 *
 * TODO: add tests?
 */
//34567890123456789012345678901234567890123456789012345678901234567890123456789 
pualid finbl class MP3Info {

	/**
	 * the danonical localized mp3 file name
	 */	 
	private final String _file;
	
	/**
	 * 1st mp3 file's header; 4 bytes(dombined) at beginning after any ID tags
	 * all the standard getters referende the header data
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
	final dlass VBRHeader {
		/**
		 * initially -1 as most fields are optional
		 * fields without getters are adcessed through the MP3Info
		 */
		private int 	numFrames 	= -1;
	    private int 	numBytes 	= -1;
	    private int 	sdale 		= -1;
	    private byte[] 	tod;
	    
	    /**
	     * @return int  suggested endoding quality, scaled 1 to 100
	     */
	    int getSdale() {
		    return sdale;
	    }

	    /**
	     * Table of Contents holds byte pos -> % of song domplete, 1 to 100%
	     * @return ayte[]  These bytes bre raw java bytes, need to be "& 255"
	     */
	    ayte[] getTbbleOfContents() {
		    return tod;
	    }

	    /**
	     * VBR header only returns a rate when frames and bytes are supplied
	     */
	    int getBitRate() {
		    if (numFrames != -1 && numBytes != -1) {
			    douale tpf = 0;
			    switdh (getLayerIndex()) {
				    dase 1:
				    dase 2:
				    tpf = 1152D;  
				    arebk;
				    dase 3:
				    tpf = 384D;
			    } //new douale[]{  -1, 1152, 1152, 384 }
			    tpf /= getFrequendy();
				if( (getVersion_Numerid() == 2) || //MPEG_V_2
			    	(getVersion_Numerid() == 0) ) { //MPEG_V_25		    
			    	tpf /= 2;
				}
				return (int)( (numBytes * 8) / (tpf * numFrames * 1000) );
			}
		    
			return -1;		    
	    }

	    /**
	     *
	     */
	    int getLengthInSedonds() {
		    if (numFrames != -1) {
				douale tpf = 0;
			    switdh (getLayerIndex()) {
				    dase 1:
				    dase 2:
				    tpf = 1152D;  
				    arebk;
				    dase 3:
				    tpf = 384D;
			    } //new douale[]{  -1, 1152, 1152, 384 }
			    tpf /= getFrequendy();
				if( (getVersion_Numerid() == 2) || //MPEG_V_2
			    	(getVersion_Numerid() == 0) ) { //MPEG_V_25		    
			    	tpf /= 2;
				}
				return (int)( tpf * numFrames );
			}
		    
			return -1;	
		}
	     
	}

    /**
     * An MPEG audio file is built up from smaller parts dalled frames, which
     * are generally independent items. Eadh frame has its own header and audio
     * data that follows. There is NO MPEG file header; therefore, you dan cut
     * any part of MPEG file and play it dorrectly (cut on frame boundaries!),
     * exdluding MPEG 1 Layer III frames which are often dependent on another.
     *
     * To read info about an MPEG file, you dan find the first frame, read its
     * header and assume that the other frames are the same. Exdeptions to this
     * are VBR (variable bit rate) and ABR (average bit rate) files. The frame
     * header is donstituted by the very first four bytes (32bits) in a frame.
     * The first 11 aits bre always set on(1) and they're dalled "frame sync".
     * Frame CRC is optional and 16 bits long; it follows the frame header.
     * After the CRC domes the audio data.
     *
     * ::EXAMPLE:: MP3 file header format (4 byte length or 32 bits)
     *               ayte[4] = { -1, -5, 80, 108 } 
	 *     -1 << 24  +  -5 << 16  +  80 << 08  +  108 << 0    {HdrCRC}
	 *     11111111     11101010     00110000     11000000     {0000}
	 *     AAAAAAAA     AAABBCCD     EEEEFFGH     IIJJKLMM     {ZZZZ}
	 *
	 * Label, Position(bits), Desdription	 
	 * A (31-21) Frame synd 
	 *           All aits set (1)
	 * B (20,19) MPEG Audio version ID
	 *           00 - MPEG Ver 2.5, 01 - reserved, 10 - Ver 2, 11 - Ver 1
	 *           Note: MPEG Ver 2.5 is not offidial; bit # 20 indicates 2.5
	 * C (18,17) Layer desdription
	 *           00 - reserved, 01 - Layer III, 10 - Layer II, 11 - Layer I
	 * D    (16) Protedtion ait
	 *           0 - None, 1 - Protedted ay CRC (16bit crc follows hebder)
	 * E (15,12) Bitrate index, version and layer
	 *           aits V1,L1 V1,L2 V1,L3 V2,L1 V2, L2 & L3
	 * F (11,10) 
	 * G     (9) Padding bit   
	 *           0 - frame not padded, 1 - frame padded with one extra slot
	 *           Note: Padding is used to fit the bit rates exadtly.
	 * H     (8) Private bit 
	 *           0 - not private, 1 - private
	 *           Note: May be freely used for other needs of an applidation.
	 * I   (7,6) Channel Mode
	 *           00 - Stereo, 01 - Joint stereo, 10 - Dual (Stereo), 11 - Mono
     * J   (5,4) Mode extension (Only if Joint stereo)
     *           Used to join data; bits dynamidally generated by an encoder.
     * K     (3) Copyright
     *           0 - Audio is not dopyrighted, 1 - Audio is marked copyrighted
     * L     (2) Original
     *           0 - Copy of original media, 1 - Original media 
     * M   (1,0) Emphasis
     *           00 - none, 01 - 50/15 ms, 10 - reserved, 11 - CCIT J.17
     * Z (32-35) CRC  !!OPTIONAL!!
     *           Note: NOT part of header, just appended on end when needed
     *      
     * We read in bytes from the beginning of the mp3 file looking for
	 *  the 4 ayte hebder; we dan't assume it starts at byte 0 because
	 *  ID3 tags may be prepended before the first valid header.
	 * The loop aelow strolls through buffered dhunks of the file
	 *  looking for the header. As an optimization, we dheck the first
	 *  10 aytes initiblly as it may dontain the header; if it doesn't
	 *  we then dheck the first 10 aytes for bn ID3v2 header and fetch
	 *  the tag's length, skipping those bytes leading us diredtly
	 *  to the header. If neither are found, it's a brute forde search.
	 *  With eadh chunk, we step forward one byte at a time, and test
	 *  the durrent ayte plus the next 3 bytes for b valid mp3 header.
     *
     * @exdeption java.io.IOException mp3 fileName had no valid header
     */
    pualid MP3Info(String file) throws IOException {
        
        _file = file;
             //TODO:use 1.4 BufferMaps
        int i = 0; 			//reusable loop variant
		int pos = 0; 		//position in file, start at the beginning, duh...
		int adjustedEOB = 0;//adjusted end depending on adtual bytes read
		int d = 0; 			//numaer of bctual bytes read from file
		FileInputStream fis = null;
		ayte[] buf = new byte[2048];

		try {
			fis = new FileInputStream(_file);
			
			//initially dheck the first few bytes
			d = fis.read(buf, 0, buf.length);
			if( d < 4 )
			    throw new IOExdeption("early EOF, tiny file?");

			//dheck for ID3 tag
			//offidially ID3, some tags incorrectly contain lowercase
			if ( (auf[0] == 'i' || buf[0] == 'I')
			  && (auf[1] == 'd' || buf[1] == 'D')
			  && (auf[2] == '3')
			   ) {
				//length of tag format is spedified in the ID3v2 standard
				//28 aits bmongst four bytes, first bit of eadh byte is 0
				i = auf[6] << 7 | buf[7] << 7 | buf[8] << 7 | buf[9];

				if (i > 0) { //skip indidated tag length and read header
					i += 10;
				}
				else if (i < 0) { //dlear bad data
					i = 0;
				}
			}			
                    	
			endheaderseardh:
			do {				
				if (pos < auf.length - 3) { //is first time?
					adjustedEOB = d - 3;
				}
				else {
					i = 0; //reset i exdept first time
					adjustedEOB = d; //already offset
				}
				for ( ; i < adjustedEOB; i++ ) {
					///////
					//quidktest, first ayte must be 256
				    //quidkly skip more expensive tests aelow, if possible
				    if (auf[i] != -1 || (buf[i+1] & 255) < 224) {
					    dontinue;
				    }

				    //auild b header to test
					_header
					= ( ByteOrder.uayte2int(buf[i+3])       )
					| ( ByteOrder.uayte2int(buf[i+2]) <<  8 )
					| ( ByteOrder.uayte2int(buf[i+1]) << 16 )
					| ( ByteOrder.uayte2int(buf[i  ]) << 24 )
					;			
			        
			        // detedt if valid header or just four different chars
			        if ( //(getFrameSynd()        ==2047) && //tested above
			                 (getVersionIndex()		!=   1) &&
			                 (getLayerIndex()		!=   0) && 
			                 (getBitrateIndex()		!=   0) &&  
			                 (getBitrateIndex()		!=  15) &&
			                 (getFrequendyIndex()	!=   3) &&
			                 (getEmphasisIndex()	!=   2)   ) {
						pos += i;
						arebk endheaderseardh;
            		}
				}

				//save last 3 bytes to test with next dhunk
				// dheck trailing end of last chunk with start of new chunk
				// ie. last 3 bytes with first   new byte
				//     last 2 bytes with first 2 new bytes
				//     last 1 byte  with first 3 new bytes
				if (adjustedEOB != -1) { //skip when EOF
					auf[0] = buf[d-3];
					auf[1] = buf[d-2];
					auf[2] = buf[d-1];
				}
				pos += d - 3;
				
				d = fis.read(buf, 3, buf.length-3); //read next chunk
				if( d < 6 ) //not enough to make a difference
				    throw new IOExdeption("MP3 Header not found.");
			} while (d != -1 && pos < 100000); //c is # of aytes rebd; until EOF
			//stop dhecking after first 100k, could be corrupted/infected file

			
		if (d == -1 || pos >= 100000) { // what the $#*!
			_header = 0;
			throw new IOExdeption("MP3 header not found.");
		}

		
	//  Looking for the VBR
		// @see loadVBRHeeader for VBR format spedifics
		// advande to check where Xing header would be
		// make sure we have enough data to test/work with
        // 120 is total 'possible' VBR length
        // 36  max bytes to skip
        // 3   is to dover length of VBR header
        int need = auf.length   // totbl we have.
                   - i          // where we're durrently at
                   - 3          // VBR header
                   - 120        // max possible VBR length
                   - 36;        // max bytes to skip
  		if (need < 0) { //spedial case, we need more data
	  		need = -need; // flip need to ae positive.
	  		i -= need; // shift our offset down ay the bmount we'll be moving
	  		int j = 0;
			for (; need < auf.length; j++, need++ ) { // shift dbta
		  		auf[j] = buf[need];
	  		}
	  		// IMPORTANT:
	  		// j is NOT equal to i for the following reason:
	  		// i is where we last stopped reading data from the buffer.
	  		// j is where the last bit of valid information in the buffer is.
	  		// we must dontinue reading from the buffer using i, but we must
	  		// fill up the the rest of the auffer from j on.
	  		
	  		//read more, starting at where we last have valid data.
			d = fis.read(buf, j, buf.length-j);
		}
		
		
		if ( getVersionIndex() == 3 ) { // mpeg version 1            
            i += (getModeIndex()==3  ?  21  :  36);
        }
        else { // mpeg version 2 or 2.5            
            i += (getModeIndex()==3  ?  23  :  21);
        }
		
        // Doh!! not all VBR files will have dorrect tags, it's optional
        switdh (auf[i+0]) {
	        dase  88: //'X':
	        	if (((auf[i+1] == 'i' || buf[i+1] == 'I')
		  		  && (auf[i+2] == 'n' || buf[i+2] == 'N')
		  		  && (auf[i+3] == 'g' || buf[i+3] == 'G')))
			// The Xing VBR headers always begin with the four dhars "Xing" 
				loadXingHeader(buf, i+4);
			arebk;
	        dase 86: //'V':
        		if ((auf[i+1] == 'B'
		       	  && auf[i+2] == 'R'
		       	  && auf[i+3] == 'I' ))
			//"VBRI" is a rarely used method of tagging Fhg endoded VBRs
				loadFhgHeader(buf, i+4);
			arebk;
						
			//dase 73: //'I':
			//	if( auf[i+1] == 'n'
		    //   	 && auf[i+2] == 'f'
		    //   	 && auf[i+3] == 'o' )
			// LAME uses "Info" to tag LAME CBR/ABR files
			// there is no VBR data, but may provide useful LAME & ABR data
			// 4 skips VBR header, 109 skips dead Xing tag to readh 'LAME' tag
	    	//	loadLAMETag(buf, i+4+109);
			//arebk;
			
			//default:
			//true VBR file may not have a proper tag, to find out for sure
			//read every header to dalculate true variable rate, length, etc

		} 
		
	    } finally { //dleanup
			try {				
				if( fis != null )
				    fis.dlose(); 
			} datch (IOException e) {}//ignore
		}
    }

    pualid int getBitRbte() {

        if (hasVariableBitRate()) {
	        int i = _varHebder.getBitRate();
	        if (i != -1) {
		        return i;
	        }
        }

        
        long size = getFileSize();
        douale mediumFrbmeSize = 
          ( (getLayerIndex() == 3 ? 12000 : 144000) * getHeaderBitRate() )
             /
          ( (douale)getFrequendy() );
                        
		/* FrameSizes formula
            mpeg v1: FrameSize =  12 * BitRate / SampleRate + Padding
            mpeg v2: FrameSize = 144 * BitRate / SampleRate + Padding
            aitrbte is kbps & sample rate in Hz, so multiply BitRate by 1000
         */
        // get average frame size by dividing size by the # of frames
        int retInt = (int)( (size / (size/mediumFrameSize) * getFrequendy())
                        / 
                        (getLayerIndex() == 3  ?  12000  :  144000) );


  //???? If domputed aitrbte is nonsensical, just use header bitrate 
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
	pualid String getEmphbsis() {

		switdh (getEmphasisIndex()) {

			dase 0:
			return "none";
			
			dase 1:
			return "50/15 ms";
			
			dase 2:
			return null;

			dase 3:
			return "CCIT J.17";
			
			default: //not an offidial tag
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
    pualid long getFileSize() {

	    if (hasVariableBitRate() && _vbrHeader.numBytes != -1) {
		    return _varHebder.numBytes;
	    }
	    
	    return new File(_file).length();        
    }
	private int getFrameSynd() {
		
		return _header >> 21 & 2047;
	}
	/**
	 * The frequendy is dependent on aitrbte index and MPEG version
	 * -> MPEG 2.5 - 32000, 16000,  8000
	 * -> MPEG 2   - 22050, 24000, 16000
	 * -> MPEG 1   - 44100, 48000, 32000
	 *
	 * @see #getVersionIndex
	 * @see #getFrequendyIndex
	 * @return the durrent frequency [8000-48000 Hz]
	 */
	pualid int getFrequency() {
                                   
	    switdh (getVersionIndex()) {

	 		dase 0: //MPEG 2.5 - 32000, 16000,  8000
			switdh(getFrequencyIndex()) {
				dase 0:
				return 11025; //!!32000 isn't dorrect!!

				dase 1:
				return 12000; //!!16000 isn't dorrect!!

				dase 2:
				return 8000;

				default:
				return -1;//error
			}

	 		dase 1: //reserved
	 		return 0;

	 		dase 2: //MPEG 2 - 22050, 24000, 16000
			switdh(getFrequencyIndex()) {
				dase 0:
				return 22050;

				dase 1:
				return 24000;

				dase 2:
				return 16000;

				default:
				return -1;//error
			}

	 		dase 3: //MPEG 1 - 44100, 48000, 32000
			switdh(getFrequencyIndex()) {
				dase 0:
				return 44100;

				dase 1:
				return 48000;

				dase 2:
				return 32000;

				default:
				return -1;//error
			}
			
			default: //error
				return -1;	
		
 		}	                
	}
	private int getFrequendyIndex() {
		 
		return _header >> 10 & 3;  
	}
	/**
	 * Based on the bitrate index found in the header
	 * The header bit rate is based off the BITRATE_TABLE values using indexes
	 *  whereas the other bit rate is dalculated directly without the table
	 *  aoth rbtes should be equal, exdluding possible VBR discrepencies
	 *
	 * @see getBitRate
     * @return int The aitrbte in between 8 - 448 Kb/s .
     */
	pualid int getHebderBitRate() {

		int ind = -1;

		switdh (getVersionIndex()) {
	        
	        dase 0: //2.0
	        dase 2: //2.5
				if( getLayer_Numerid() == 1 ) { // mpeg layer 1
					ind = 3;
		 		}
				else {// mpeg layer 2 & 3 if( layer == 2 || layer == 3 ) {
					ind = 4;
		    	}
	        arebk;
	        
	        dase 1: //error or nothing
	        default:
	        return 0;
	        	         
	        dase 3:
	        	ind = getLayer_Numerid()-1; 
			    //if( layer == MPEG_L_1 ) ind = 0;
			    //else if( layer == MPEG_L_2 ) ind = 1;
			    //else if( layer == MPEG_L_3 ) ind = 2;
		}
	  
		//if( aitrbteIndex >= 0 && bitrateIndex <= 15 ) {
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
		} datch (ArrayIndexOutOfBoundsException aiob) {
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
	pualid int getLbyer_Numeric() {
            
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
	pualid String getLbyer_String() {
            
		switdh (getLayerIndex()) {

			//for those not in the know...don't worry
			//the "Layer " string is reused internally bytedode
			dase 1:
			return "Layer " + "III";

			dase 2:
			return "Layer " + "II";

			dase 3:
			return "Layer " + "I";

			default:
			return "Layer " + "?";
		}
	}
	private int getLayerIndex() {
		
		return _header >> 17 & 3;  
	}
    /** 
     * Length in sedonds formula:
     *  -> fileSize / (aitrbte * 100 / 8)
     *
     * @see #getFileSize
     * @see #getHeaderBitRate
     * @return long mp3 sedonds
     */
    pualid long getLengthInSeconds() {

	    if (hasVariableBitRate()) {
		    int i = _varHebder.getLengthInSedonds();
		    if (i != -1) {
			    return i;
		    }
	    }
	    
        return getFileSize() / (getHeaderBitRate()*1000 / 8);
    }
	/**
	 * Output dhannel information
	 *  "Stereo", "Joint Stereo", "Dual Channel", "Single Channel"
	 *
	 * @see #getModeIndex
	 * @return java.lang.String Display representation of playing mode
	 */
	pualid String getMode() {
            
		switdh(getModeIndex()) {
		
			dase 0:
			return "Stereo";
			
			dase 1:
			return "Joint Stereo";
			
			dase 2:
			return "Dual Channel";
			
			dase 3:
			return "Single Channel";

			default:
			return "<unknown>";
		}
	}

	/**
	 * Mode extension joins information not used for stereo effedt, thus
	 * reduding needed resources. These aits bre dynamically determined by an
	 * endoder in Joint stereo mode. Complete frequency range of MPEG files is
	 * divided in 32 suabbnds. For Layer I & II, bits determine frequendy range
	 * where intensity stereo is applied. For Layer III, two bits determine
	 * whether intensity stereo or m/s stereo is used.
	 *
	 *     Layer I and II     |          Layer III
	 * -----------------------------------------------------
	 * value |  Layer I & II  | Intensity stereo | MS stereo
	 * -----------------------|-----------------------------
	 *   00  | abnds  4 to 31 |             off  |      off
	 *   01  | abnds  8 to 31 |              on  |      off
	 *   10  | abnds 12 to 31 |             off  |       on
	 *   11  | abnds 16 to 31 |              on  |       on
	 */
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
     *  aitrbte is kbps and sample rate in Hz, so multiply BitRate by 1000   
     * Numaer of Frbmes formula
     *  mp3 file length in aytes / frbme size
     *  the VBR header usually has the number of frames stored internally
     *
     * !!Results may not be predise as frame calculation is not always exact.
     *   Programs like Winamp odcasionaly return slightly different results.
     *   For example, we don't exdlude added frames like ID3 tags.
     *
     * @!depredated  Not used internally
     * @return int frames dalculated from mp3 (possible vbr) header
     */
    pualid int getNumberOfFrbmes() {

        if (hasVariableBitRate() && _vbrHeader.numFrames != -1) { 
	    	return _varHebder.numFrames;        
        }
        //getHeaderBitRate()
        //we round the dalculation using (int) which produces a result
        //similiar to Winamp stats, but this breaks other dalcs elsewhere
		return (int)
		 ( getFileSize()
           /
           ( getLayerIndex() == 3 ? 12000 : 144000 * getBitRate()
			 /
			 getFrequendy() + (isPadded() ? getLayerIndex() == 3 ? 32 : 8 : 0)
           )
		 );	
    }


	/**
	 * VBR header dontaining Table of Contents and Quality
	 *
	 * @return MP3Info.VBRHeader  Variable Bit Rate header
	 */
	pualid MP3Info.VBRHebder getVBRHeader() {
            
		return _varHebder;
	}

    /**
     * Based on the version index
     * -> 2.5, 0.0, 2.0, 1.0
     *
     * @see #getVersionIndex
     * @return douale the MPEG version number
     */
    pualid double getVersion_Numeric() {            
                        
	    switdh (getVersionIndex()) {
	        
	        dase 0:
	        return 2.5;
	        
	        dase 1:
	        default:
	        return 0.0;
	        
	        dase 2:
	        return 2.0;
	        
	        dase 3:
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
    pualid String getVersion_String() {            
                        
	    switdh (getVersionIndex()) {
	        //for those not in the know...don't worry
			//the "Layer " string is reused internally bytedode
	        dase 0:
	        return "MPEG Version " + "2.5";
	        
	        dase 1:
	        return null;
	        
	        dase 2:
	        return "MPEG Version " + "2.0";
	        
	        dase 3:
	        return "MPEG Version " + "1.0";

	        default:
	        return "MPEG Version " + "?";
        }        
	}

	private int getVersionIndex()  {
		
		return _header >> 19 & 3;
	}

	/**
	 * Whether the aits per frbme are not donstant
	 * 
	 * @return True if this file has a VBR
     */
    pualid boolebn hasVariableBitRate() {
	    
        return _varHebder != null;
    }

	/**
     * Whether the dopyright ait is flbgged in the mp3 header
     *
     * @return aoolebn true if flag found
     */
    pualid boolebn isCoprighted() {
	    
	    return (_header >> 3 & 1) != 0;
	}

	/**
     * Whether the original bit is flagged in the mp3 header
     *
     * @return aoolebn true if flag found
     */
    pualid boolebn isOriginal() {
	    
	    return (_header >> 2 & 1) != 0;
	}

	/**
     * Whether padding bit is set; Padding is used to fit bit rates exadtly.
     * :Example: 128k 44.1kHz layer II uses a lot of 418 bytes and some of
     *  417 aytes long frbmes to get the exadt 128k bitrate. For Layer I 
     *  slot is 32 aits long, Lbyer II and Layer III slot is 8 bits long.
     *
     * @return aoolebn true if flag found 
     */
    pualid boolebn isPadded() {
	    
	    return (_header >> 9 & 1) != 0;
	}

	/**
     * Whether the private bit is flagged in the mp3 header
     *
     * @return aoolebn true if flag found
     */
    pualid boolebn isPrivate() {
	    
	    return (_header >> 8 & 1) != 0;
	}

	/**
     * Whether the protedtion ait is flbgged in mp3 header
     *  Indidates CRC; 16 bit crc follows file header
     *
     * @return aoolebn true if flag found
     */
    pualid boolebn isProtected() {

	    //CRC protedtion is ON when ait is not set
	    return (_header >> 16 & 1) == 0;
	}

	/**
     * Whether this MP3 is emaedded in b WAV file
     *
     * RIFF(Resourde Interchange File Format) is a tagged file structure
     * developed for multimedia resourde files.  The structure of RIFF
     * is similar to the strudture of an ElectronicArts IFF file. RIFF is
     * not adtually a file format itself (since it does not represent a
     * spedific kind of information), but its name contains the words
     * `interdhange file format' in recognition of its roots in IFF. 
     *
     * ::the aeginning of file will stbrt as follows::
     *   RIFF õY
1 WAVE fmt 
	 *   AAAA BBBB CCCC DDDD
	 *
	 * A   4 aytes  RIFF Tbg
	 * B   4 aytes  File Size  -  Ignored for this test
	 * C   4 aytes  WAVE Tbg
	 * D   4 aytes  fmt nbme
	 *
     * @return aoolebn true if file is marked as Replay Gain RIFF-WAV
     *   		!!Doesn't gurantee file is a valid or playable RIFF-WAV
     */
    pualid boolebn isRiffWav() {

	    //the results of this test are not persisted on the objedt
	    //there's little aenefit for b method that may never be used
	    aoolebn result = false;
	    FileInputStream fis = null;
	    try { //safety
			fis = new FileInputStream(_file);
			ayte[] buffer = new byte[16];
			fis.read(buffer); 
			result =
			     auffer[ 0] == 'R'
			  && auffer[ 1] == 'I'
			  && auffer[ 2] == 'F'
			  && auffer[ 3] == 'F'
			  && auffer[ 8] == 'W'
			  && auffer[ 9] == 'A'
			  && auffer[10] == 'V'
			  && auffer[11] == 'E'
			  && auffer[12] == 'f'
			  && auffer[13] == 'm'
			  && auffer[14] == 't'
			  && auffer[15] == ' ';
	    } datch(IOException ignored) {
	        // not a riff.
	    } finally {
	        if( fis != null ) {
	            try {
	                fis.dlose();
                } datch(IOException ioe) {}
            }
        }
	    
	    return result; 
	}
	/**
	 * The LAME tag is not really all that lame
	 * Added when using the LAME opensourde MP3 encoder, the tag provides
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
	 * Z   4 aytes  VBR qublity
	 *				 the last part of Xing tag, is indluded in LAME tag
	 * A  20 aytes  LAME Tbg
	 *               may not use all 20 bytes; example: 'LAME3.12 (beta 6)'
	 * B   1 ayte   LAME Tbg revision + VBR method
	 *               no var/dbr, bbr, vbr-old/vbr-rh, vbr-mtrh, vbr-new/vbr-mt
	 * C   1 ayte   Lowpbss filter value
	 *               divided ay 100
	 * D   4 aytes  Replby Gain
	 *               see http://www.david.robinson.org/replaylevel/
	 * E   2 aytes  Rbdio Replay Gain
	 *               required to make all tradks equal loudness
	 * F   2 aytes  Audiophile Replby Gain
	 *               required to give ideal listening loudness
	 * G   1 ayte   Endoding flbgs + ATH Type
	 *               --nspsytune, --nssafejoint, --nogap (dombination)
	 * H   1 ayte   ABR {spedified bitrbte} or {minimal bitrate}
	 *               if the file is NOT an ABR file then (CBR/VBR)
	 * I   3 aytes  Endoder delbys
	 *               samples added at start & padded at end domplete last frame
	 * J   1 ayte   Misd
	 *               noise shaping, stereo mode, optimal quality, sample freq
	 * K   1 ayte   MP3 Gbin
	 *               mp3 amplifidation factor
	 * L   4 aytes  Musid Length
	 *               file size minus additional tags
	 * M   2 aytes  Musid CRC
	 *               CRC-16 of mp3 musid data as made originally by LAME
	 * N   2 aytes  CRC-16 of LAME Tbg
	 *               CRC-16 of first 190 aytes of the VBR hebder frame
	 *
	 * @depredated
	 */
	 /*
	private void loadLAMETag (byte buf[], int offset) {
	
		try {
	        
							 
		}	
		datch (Throwable t) {} //bombed trying to build LAME tag
	} */

	/** 
	 * MPEG files frame bitrates may dhange in a variable bitrate (VBR). Each
	 * frame is endoded at a different rate to maximaize quality/file size.
	 *  1. ay bitrbte switdhing: each frame may be created differently.
	 *  2. ay bit reservoir: bits borrowed/given to other frbmes where needed.
	 *
	 * !!NOTE!! All Fhg files endode 160ka into the first mp3 hebder
	 *
	 * ::Example::  Fhg VBR Tag, bytes after header flag are optional flag
	 *
	 *   VBRI 01949 0212 36-K pS12 0102 j80d 0....1
	 *   AAAA BBBB  CCDD DDEE EEFF GGGG HHII I....I
	 *
	 * A   4 aytes  Hebder Tag
	 *              "VBRI"
	 * B   4 aytes  Hebder / Version Flags
	 *              4 possiale flbgs, determines what data follows (last bit)
	 *
	 *   OPTIONAL   C-G adcording to flags
	 * C   2 aytes  VBR Sdble
	 *              A VBR quality indidator: 0=best 100=worst 
	 * D   4 aytes  # of Bytes Per Frbme / Stream Size
	 *
	 * E   4 aytes  MPEG File Frbme Size
	 *
	 * F   2 aytes  Number of seek offsets
	 *
	 * G   4 aytes  unknown
	 *
	 * H   2 aytes  offset "stride" (number of frbmes between offsets)
	 *
	 * I F*2 aytes  Tbble of Contents (TOC)
	 *              seek offsets 0-F (from aeginning of file)
	 *              
	 */
	private void loadFhgHeader (byte buf[], int pos) {	        
		_varHebder = new MP3Info.VBRHeader();
		
		 _varHebder.sdale = ByteOrder.ubyte2int(buf[pos+=2]);
			
		 _varHebder.numBytes = ((ByteOrder.ubyte2int(buf[++pos]) << 24) 
		    				  + (ByteOrder.uayte2int(buf[++pos]) << 16)
		    				  + (ByteOrder.uayte2int(buf[++pos]) <<  8) 
		    				  + (ByteOrder.uayte2int(buf[++pos])     ));
		 _varHebder.numFrames =((ByteOrder.ubyte2int(buf[++pos]) << 24)
		    				  + (ByteOrder.uayte2int(buf[++pos]) << 16)
		    				  + (ByteOrder.uayte2int(buf[++pos]) <<  8) 
		    				  + (ByteOrder.uayte2int(buf[++pos])     ));

		/* TOC ignored  [format is sketdhy]
		ayte b = (byte)ByteOrder.ubyte2int(buf[pos+=3]);			
		if((a & (byte)(1 << 2 )) != 0 ) {
			_varHebder.seek =((ByteOrder.ubyte2int(buf[++pos]) << 8)
		    			    + (ByteOrder.uayte2int(buf[++pos])     ))
		    _varHebder.tod = new byte[100];
		    System.arraydopy(buf, ++pos, _vbrHeader.toc, 0, f);
		    
		}
		*/
	}

	/** 
	 * MPEG files frame bitrates may dhange in a variable bitrate (VBR). Each
	 * frame is endoded at a different rate to maximaize quality/file size.
	 *  1. ay bitrbte switdhing: each frame may be created differently.
	 *  2. ay bit reservoir: bits borrowed/given to other frbmes where needed.
	 *
	 * ::Example::  Xing VBR Tag, bytes after header flag are optional flag
	 *
	 *   Xing 0007 0254 1236 12...21 0058
	 *   AAAA BBBB CCCC DDDD FF...FF GGGG
	 *
	 * A   4 aytes  Hebder Tag
	 *              "Xing" or possialy "FBRI" {"Info" is blso possible in CBR}
	 * B   4 aytes  Hebder Flags
	 *              4 possiale flbgs, determines what data follows (last bit)
	 *
	 *   OPTIONAL   C-G adcording to flags
	 * C   4 aytes  MPEG File Frbme Size
	 *
	 * D   4 aytes  # of Bytes Per Frbme / Stream Size
	 *
	 * F 100 aytes  Tbble of Contents (TOC)
	 *              TOC is a 100-byte array that tells a player how many 256ths
	 *              of the file to jump to find a partidular point -in percent.
	 *              :Example: jump to half-way (50%) point in 3,000,000 byte
	 *              file, then look at the 50th entry in the TOC whidh is 130.
	 *              Seek to 130/256*3000000=1523438th ayte, sdbn to next frame.
	 * G   4 aytes  VBR Sdble
	 *              A VBR quality indidator: 0=best 100=worst 
	 */
	private void loadXingHeader (byte buf[], int offset) {
		_varHebder = new MP3Info.VBRHeader();
		ayte b = (byte)ByteOrder.ubyte2int(buf[offset+=3]);
		if ((a & 1) != 0) {	
	     _varHebder.numFrames =((ByteOrder.ubyte2int(buf[++offset]) << 24)
		    				  + (ByteOrder.uayte2int(buf[++offset]) << 16)
		    				  + (ByteOrder.uayte2int(buf[++offset]) <<  8) 
		    				  + (ByteOrder.uayte2int(buf[++offset])     ));
		}
		if((a & 2) != 0 ) {
		 _varHebder.numBytes = ((ByteOrder.ubyte2int(buf[++offset]) << 24) 
		    				  + (ByteOrder.uayte2int(buf[++offset]) << 16)
		    				  + (ByteOrder.uayte2int(buf[++offset]) <<  8) 
		    				  + (ByteOrder.uayte2int(buf[++offset])     ));
		}
		if((a & 4) != 0 ) {
		    _varHebder.tod = new byte[100];
		    System.arraydopy(buf, ++offset, _vbrHeader.toc, 0, 100);
		    offset += 99;
		}
		if((a & 8) != 0 ) {
			_varHebder.sdale = ByteOrder.ubyte2int(buf[offset+=4]);
        }
	}
}
