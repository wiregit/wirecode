package com.limegroup.gnutella.mp3;

import junit.framework.*;
import com.limegroup.gnutella.mp3.*;
import com.sun.java.util.collections.*;
/**
 * Test MP3Info class
 *
 *
 * @author  cHANCE mOORE, ctmoore@gottapee.com - 30 July 2002
 */
//34567890123456789012345678901234567890123456789012345678901234567890123456789
public class MP3Test extends TestCase {
	
	private static final int runs = 900;
	
	private static final String d = "./"; // current directory
	
	private static final String file[] = new String[] {
	d+"mpg1layIII_96k-RIFFWAV_441000hz_joint_Xing.wav",
	d+"mpg1layI_0h_448k_frame14_48000hz_dual_fl5Orig.mp3",
	d+"mpg1layI_43h_32k_f256_44100hz_dual_50-15emph_CRCcopyOrig_sectest.mp3",
	d+"mpg1layII_0h_384k_frame24_48000hz_stereo_CRCOrig_Gogo1sectest.mp3",
	d+"mpg1layIII_212k-VBRq0_f1655_441hz_stereo_FhgTAGID3v1_waterson.mp3",
	d+"mpg1layIII_0h_58k-VBRq30_frame1211_44100hz_joint_XingTAG_sample.mp3",
	d+"mpg1layIII_217k-VBRq0NOTAG_f5815_44100hz_joint_ID3v1&2Orig_Knapp.mp3",
	d+"mpg1layIII_138k-VBRq44_f2895_44100hz_joint_XingTAG_ID3v2_Lame.mp3",
	d+"mpg1layIII_170k-VBRq66_f1721_441hz_stereo_XingTAGcopyID3v1_mutter.mp3",	
	d+"mpg1layII_0h_192k_frame44_44100hz_joint_CRCfl11.mp2",
	d+"mpg2.5layIII_0h_16k_f2573or2594_11050hz_joint_CRCcopy_starwars.mp3",
	d+"mpg2.5layIII_8k-VBRq95_f149_8000khz_mono_ID3v2Orig_XingTAG_Lame.mp3",
	d+"mpg2layI_0h_128k_frame54_22050hz_joint_CRCOrig_test33.mp3",
	d+"mpg2layII_1504h_16k_frame56_24000hz_joint_CRCOrigID3v1&2_test27.mp3",
	d+"mpg2layIII_0h_40k_764or744frame_22050hz_mono_Orig_Fhgl3Frank.mp3",
	d+"mpg2layIII_12kABRq55_f1202_16hz_stereo_XingLameTAG_CRCOrigwaterson.mp3",
	d+"mpgPlus_210k-VBR_f1149_441hz_joint_copy__NOTmp1lay_448k_f540_5015.mpc",
	d+"mpgPRO2layIII_0h_64k_frame2036or2015_22050hz_joint_thomson.mp3",
	d+"mpg4_golem160x90first120.avi",
	d+"corruptFileWithBadHeaders.mp3"};

    public MP3Test(String name) {
        super(name);
    }

    /**
     * 
     * @param args java.lang.String[]
     */
    public static void main(String[] args) {
        
        junit.textui.TestRunner.run(suite());
        
        
    }
    protected void setUp() {
        
    }
    public static Test suite() {
        return new TestSuite(MP3Test.class);
    }
    protected void tearDown() {
        
    }

    
    /**
     *
     * All values (where possible) have been validated by
     *  Winamp	winamp.com
     *  EncSpot	guerillasoft.com
     *  LAME
     *  MusicMatch musicmatch.com
     *  		
     */
    public void testMP3()
        throws Exception {
        
        MP3Info info = null;
        
        /*	mpg1layIII_217k-VBRq0NOTAG_f5815_441hz_joint_ID3v1&2_OrigKnapp.mp3
            com.limegroup.gnutella.mp3.MP3Info info = null;
            info  = new MP3Info(file[6]);
            MP3Info2 info2 = new MP3Info2(file[6]);
            assertEquals(info.getBitRate() , info2.getBitRate()); // 32 
            //if (info.isCoprighted() != info2._header.getCoprightBit());
            assertEquals(info.getFrequency() , info2._header.getFrequency());	
            assertEquals(info.getHeaderBitRate() , info2._header.getBitRate());	//32  weird data, same as winamp
            assertEquals(info.getLayer_Numeric() , info2._header.getLayer()); //3		
            assertEquals(info.getLengthInSeconds() , info2.getLengthInSeconds()); //1030	
            assertEquals(info.getMode() , info2._header.getMode());		
            assertEquals(info.getVersion_Numeric() , info2._header.getVersion(), 0D); //1.0d
            //old way is incorrect at 39440 frames	
            //assertTrue(info.getNumberOfFrames() == info2.getNumberOfFrames()) //39626us - 39624 winamp
            //no similiar method match
            //assertTrue(!info.getEmphasis().equals(info2._header.getEmphasisIndex())) //"none"	
            assertTrue(info.hasVariableBitRate() == info2.hasVariableBitRate()); 
            //////////////////////////////////////
            */
        /*
          mpg1layIII_96k-RIFFWAV_441000hz_joint_Xing.wav
          An MP3 file embedded in a wav file format 
        */
        info  = new MP3Info(file[0] );
        assertEquals(info.getFileSize(), 743933L);
        assertEquals(info.getBitRate() , 96);		
        assertTrue(!info.isCoprighted());		
        assertTrue(info.isOriginal());	
        assertTrue(!info.isPadded());	
        assertTrue(!info.isPrivate());	
        assertTrue(!info.isProtected());
        assertTrue(info.isRiffWav());	
        assertEquals(info.getFrequency() , 44100);	
        assertEquals(info.getHeaderBitRate() , 96);
        assertEquals(info.getLayer_Numeric() , 3);		
        assertEquals(info.getLengthInSeconds() , 61);	
        assertEquals(info.getMode() , "Joint Stereo");	
        assertEquals(info.getVersion_Numeric() , 1.0d, 0D);		
        assertEquals(info.getNumberOfFrames() , 2376);
        assertEquals(info.getEmphasis() , "none");	
        assertTrue(!info.hasVariableBitRate());
        
        /*
          mpg1layI_0h_448k_frame14_48000hz_dual_fl5Orig.mp3
        */
        info  = new MP3Info(file[1]);
        assertEquals(info.getFileSize(), 21952L);
        assertEquals(info.getBitRate() , 448);		
        assertTrue(!info.isCoprighted());		
        assertTrue(info.isOriginal());	
        assertTrue(!info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 48000);	
        assertEquals(info.getHeaderBitRate() , 448);
        assertEquals(info.getLayer_Numeric() , 1); 		
        assertEquals(info.getLengthInSeconds() , 0); 
        assertEquals(info.getMode() , "Dual Channel");	
        assertEquals(info.getVersion_Numeric() , 1.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 1); //14-encspot 16-winamp//////////////////////////////////////
        assertEquals(info.getEmphasis() , "none");
        assertTrue(!info.hasVariableBitRate());
        
        /*
          mpg1layI_32k_f256_441hz_dual_5015emph_CRCcopyOrig_sectest.mp3
        */
        info  = new MP3Info(file[2]);
        assertEquals(info.getFileSize(), 26645L);
        assertEquals(info.getBitRate() , 32);		
        assertTrue(info.isCoprighted());		
        assertTrue(info.isOriginal());	
        assertTrue(!info.isPadded()); 	
        assertTrue(info.isPrivate());	
        assertTrue(info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 44100);	
        assertEquals(info.getHeaderBitRate() , 32);
        assertEquals(info.getLayer_Numeric() , 1); 		
        assertEquals(info.getLengthInSeconds() , 6); 
        assertEquals(info.getMode() , "Dual Channel");	
        assertEquals(info.getVersion_Numeric() , 1.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 2); //256-winamp no encspot/////////////////////////////////////
        assertEquals(info.getEmphasis() , "50/15 ms");
        assertTrue(!info.hasVariableBitRate());
        
        /*
          mpg1layII_384k_f24_48hz_stereo_CRCOrig_Gogo1sectest.mp3
        */
        info  = new MP3Info(file[3]);
        assertEquals(info.getFileSize(), 31104L);
        assertEquals(info.getBitRate() , 384);		
        assertTrue(!info.isCoprighted());		
        assertTrue(info.isOriginal());	
        assertTrue(!info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 48000);	
        assertEquals(info.getHeaderBitRate() , 384);
        assertEquals(info.getLayer_Numeric() , 2); 		
        assertEquals(info.getLengthInSeconds() , 0); 
        assertEquals(info.getMode() , "Stereo");	
        assertEquals(info.getVersion_Numeric() , 1.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 27); //24-encspot 27-winamp/////////////////////////////////////
        assertEquals(info.getEmphasis() , "none");
        assertTrue(!info.hasVariableBitRate());

        /*
          mpg1layIII_212k-VBRq0_f1655_441hz_stereo_FhgTAGID3v1_waterson.mp3
        */
        info  = new MP3Info(file[4]);
        assertEquals(info.getFileSize(), 1145541L); //1145669-encspot&winamp (both wrong, not using fhg VBR)
        assertEquals(info.getBitRate() , 211); //212-encspot&winamp	(both wrong, not using fhg VBR)
        assertTrue(!info.isCoprighted());		
        assertTrue(!info.isOriginal());	
        assertTrue(!info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(!info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 44100);	
        assertEquals(info.getHeaderBitRate() , 160); //fhg encoder puts 160 in first header
        assertEquals(info.getLayer_Numeric() , 3); 		
        assertEquals(info.getLengthInSeconds() , 43); //57&43-winamp(wrong) 43-encspot 43-musicmatch
        assertEquals(info.getMode() , "Stereo");	
        assertEquals(info.getVersion_Numeric() , 1.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 1656); //2194-winamp(wrong, not using fhg VBR) 1655-encspot
        assertEquals(info.getEmphasis() , "none");
        assertTrue(info.hasVariableBitRate());
		assertEquals(info.getVBRHeader().getScale() , 100);
		assertNull(info.getVBRHeader().getTableOfContents());

        /*
          mpg1layIII_58k-VBRq30_f1211_441hz_joint_XingTAG_sample.mp3
        */
        info  = new MP3Info(file[5]);
        assertEquals(info.getFileSize(), 232295L); 
        assertEquals(info.getBitRate() , 58); //59-winamp(wrong) 58-encspot	
        assertTrue(info.isCoprighted());		
        assertTrue(info.isOriginal());	
        assertTrue(!info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(!info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 44100);	
        assertEquals(info.getHeaderBitRate() , 48);
        assertEquals(info.getLayer_Numeric() , 3); 		
        assertEquals(info.getLengthInSeconds() , 31); 
        assertEquals(info.getMode() , "Joint Stereo");	
        assertEquals(info.getVersion_Numeric() , 1.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 1212);
        assertEquals(info.getEmphasis() , "none");
        assertTrue(info.hasVariableBitRate());
		assertEquals(info.getVBRHeader().getScale() , 30);
		assertNotNull(info.getVBRHeader().getTableOfContents());
			
        /*
          mpg1layIII_217k-VBRq0NOTAG_f5815_441hz_joint_ID3v1&2_Orig_Knapp.mp3
          MP3 VBR file without tags
        */	
        info  = new MP3Info(file[6]);
        assertEquals(info.getFileSize(), 4121123L);
        assertEquals(info.getBitRate() , 32); //32-winamp(first frame) 217-encspot
        assertTrue(!info.isCoprighted());	
        assertTrue(info.isOriginal());	
        assertTrue(!info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(!info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 44100);	
        assertEquals(info.getHeaderBitRate() , 32);	//32-winamp(first frame) 
        assertEquals(info.getLayer_Numeric() , 3);		
        assertEquals(info.getLengthInSeconds() , 1030);//1030-winamp 151-encspot(correct)
        assertEquals(info.getMode() , "Joint Stereo");	
        assertEquals(info.getVersion_Numeric() , 1.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 39626); //39624-winamp 5815-encspot
        assertEquals(info.getEmphasis() , "none");
        assertTrue(!info.hasVariableBitRate());
        ////////////////////////////////////////////////////////////////////////////////////////////////////////
        /*
          mpg1layIII_138k-VBRq44_f2895_441hz_joint_XingTAG_ID3v2_Lame.mp3
        */	
        info  = new MP3Info(file[7]);
        assertEquals(info.getFileSize(), 1304774L); //1306496-winamp
        assertEquals(info.getBitRate() , 138);	//139-winamp 138-encspot
        assertTrue(info.isCoprighted());	
        assertTrue(info.isOriginal());	
        assertTrue(!info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(!info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 44100);	
        assertEquals(info.getHeaderBitRate() , 64);
        assertEquals(info.getLayer_Numeric() , 3);		
        assertEquals(info.getLengthInSeconds() , 75);	
        assertEquals(info.getMode() , "Joint Stereo");	
        assertEquals(info.getVersion_Numeric() , 1.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 2895); // 2895-winamp 2894-encspot
        assertEquals(info.getEmphasis() , "none");
        assertTrue(info.hasVariableBitRate());
		assertEquals(info.getVBRHeader().getScale() , 44);
		assertNotNull(info.getVBRHeader().getTableOfContents());
        /*
          mpg1layIII_170k-VBRq66_f1721_441hz_stereo_XingTAGcopyID3v1_mutter.mp3
        */	
        info  = new MP3Info(file[8]);
        assertEquals(info.getFileSize(), 955459L); //956416-winamp&encspot (we use VBR data)
        assertEquals(info.getBitRate() , 170);	//173-winamp 170-encspot
        assertTrue(info.isCoprighted());	
        assertTrue(!info.isOriginal());	
        assertTrue(!info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(!info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 44100);	
        assertEquals(info.getHeaderBitRate() , 64); //first frame
        assertEquals(info.getLayer_Numeric() , 3);		
        assertEquals(info.getLengthInSeconds() , 44);	
        assertEquals(info.getMode() , "Stereo");	
        assertEquals(info.getVersion_Numeric() , 1.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 1720); 
        assertEquals(info.getEmphasis() , "none");
        assertTrue(info.hasVariableBitRate());
		assertEquals(info.getVBRHeader().getScale() , 66);
		assertNotNull(info.getVBRHeader().getTableOfContents());
        /*
          mpg1layII_0h_192k_frame44_44100hz_joint_CRCfl11.mp2
        */	
        info  = new MP3Info(file[9]);
        assertEquals(info.getFileSize(), 30720L);
        assertEquals(info.getBitRate() , 192);		
        assertTrue(!info.isCoprighted());	
        assertTrue(!info.isOriginal());	
        assertTrue(info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 44100);	
        assertEquals(info.getHeaderBitRate() , 192);
        assertEquals(info.getLayer_Numeric() , 2);		
        assertEquals(info.getLengthInSeconds() , 1);	
        assertEquals(info.getMode() , "Stereo");	
        assertEquals(info.getVersion_Numeric() , 1.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 48); //44-encspot 49-winamp/////////////////////////////////////
        assertEquals(info.getEmphasis() , "none");
        assertTrue(!info.hasVariableBitRate());
	
        /*
          mpg2.5layIII_16k_f2573or2594_11050hz_joint_CRCcopy_wars.mp3
        */	
        info  = new MP3Info(file[10]);
        assertEquals(info.getFileSize(), 269776L);
        assertEquals(info.getBitRate() , 16);		
        assertTrue(info.isCoprighted());	
        assertTrue(!info.isOriginal());	
        assertTrue(!info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 11025); //11025-encspot	
        assertEquals(info.getHeaderBitRate() , 16);
        assertEquals(info.getLayer_Numeric() , 3);		
        assertEquals(info.getLengthInSeconds() , 134);	
        assertEquals(info.getMode() , "Joint Stereo");	
        assertEquals(info.getVersion_Numeric() , 2.5d, 0D);
        assertEquals(info.getNumberOfFrames() , 1297); //2573-encspot 2594-winamp///////////////////////////////
        assertEquals(info.getEmphasis() , "none");
        assertTrue(!info.hasVariableBitRate());
	
        /*
          mpg2.5layIII_8k-VBRq95_f149_8khz_mono_ID3v2Orig_XingTAGLame.mp3
          !!Can't find Xing tag!! which is why stuff is messed up
        */	
        info  = new MP3Info(file[11]);
        assertEquals(info.getFileSize(), 14336L);
        assertEquals(info.getBitRate() , 32); //18-winamp 8-encspot/////////////////////////////////////////////
        assertTrue(!info.isCoprighted());	
        assertTrue(info.isOriginal());	
        assertTrue(!info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(!info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 8000);	
        assertEquals(info.getHeaderBitRate() , 32);	//18-winamp 8-encspot//////////////////////////////////////
        assertEquals(info.getLayer_Numeric() , 3);		
        assertEquals(info.getLengthInSeconds() , 3); //5-winamp 10-encspot/////////////////////////////////////
        assertEquals(info.getMode() , "Single Channel");	
        assertEquals(info.getVersion_Numeric() , 2.5d, 0D);
        assertEquals(info.getNumberOfFrames() , 24); //149-winamp&encspot//////////////////////////////////////
        assertEquals(info.getEmphasis() , "none");
        assertTrue(!info.hasVariableBitRate());
        //	assertEquals(info.getVBRHeader().getScale() , 95);
        //	assertNotNull(info.getVBRHeader().getTableOfContents());
        /*
          mpg2layI_128k_f54_22050hz_joint_CRCOrig_test33.mp3
        */	
        info  = new MP3Info(file[12]);
        assertEquals(info.getFileSize(), 22572L);
        assertEquals(info.getBitRate() , 128);		
        assertTrue(!info.isCoprighted());	
        assertTrue(info.isOriginal());	
        assertTrue(info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 22050);	
        assertEquals(info.getHeaderBitRate() , 128);
        assertEquals(info.getLayer_Numeric() , 1);		
        assertEquals(info.getLengthInSeconds() , 1);	
        assertEquals(info.getMode() , "Joint Stereo");	
        assertEquals(info.getVersion_Numeric() , 2.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 1); //54-winamp no encspot//////////////////////////////////////
        assertEquals(info.getEmphasis() , "none");
        assertTrue(!info.hasVariableBitRate());
	
        /*
          mpg2layII_16k_f56_24000hz_joint_CRCOrigID3v1&2_test27.mp3
        */	
        info  = new MP3Info(file[13]);
        assertEquals(info.getFileSize(), 4224L);
        assertEquals(info.getBitRate() , 16);		
        assertTrue(!info.isCoprighted());	
        assertTrue(info.isOriginal());	
        assertTrue(!info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 24000);	
        assertEquals(info.getHeaderBitRate() , 16);
        assertEquals(info.getLayer_Numeric() , 2);		
        assertEquals(info.getLengthInSeconds() , 2); //1-winamp 43-musicmatch(correct)//////////////////////////
        assertEquals(info.getMode() , "Joint Stereo");	
        assertEquals(info.getVersion_Numeric() , 2.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 44); // 56-winamp  no encspot//////////////////////////////////
        assertEquals(info.getEmphasis() , "none");
        assertTrue(!info.hasVariableBitRate());
	
        /*
          mpg2layIII_40k_f764or744_22050hz_mono_Orig_Fhgl3Frank.mp3
        */	
        info  = new MP3Info(file[14]);
        assertEquals(info.getFileSize(), 99396L);
        assertEquals(info.getBitRate() , 40);		
        assertTrue(!info.isCoprighted());	
        assertTrue(info.isOriginal());	
        assertTrue(!info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(!info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 22050);	
        assertEquals(info.getHeaderBitRate() , 40);
        assertEquals(info.getLayer_Numeric() , 3);		
        assertEquals(info.getLengthInSeconds() , 19);	
        assertEquals(info.getMode() , "Single Channel");	
        assertEquals(info.getVersion_Numeric() , 2.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 380); // 764-winamp 744-encspot/////////////////////////////////
        assertEquals(info.getEmphasis() , "none");
        assertTrue(!info.hasVariableBitRate());
	
        /*
          mpg2layIII_12kABRq55_f1202_16hz_stereo_XingLameTAG_CRCOrigwaterson.mp3
        */	
        info  = new MP3Info(file[15]);
        assertEquals(info.getFileSize(), 70488L);
        assertEquals(info.getBitRate() , 13); //13-winamp 12-encspot	
        assertTrue(!info.isCoprighted());	
        assertTrue(info.isOriginal());	
        assertTrue(!info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 16000);	
        assertEquals(info.getHeaderBitRate() , 64);	//64 first frame
        assertEquals(info.getLayer_Numeric() , 3);		
        assertEquals(info.getLengthInSeconds() , 43);	
        assertEquals(info.getMode() , "Stereo");	
        assertEquals(info.getVersion_Numeric() , 2.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 1202);
        assertEquals(info.getEmphasis() , "none");
        assertTrue(info.hasVariableBitRate());
		assertEquals(info.getVBRHeader().getScale() , 55);
		assertNotNull(info.getVBRHeader().getTableOfContents());
	
        /*
          mpgPlus_210k-VBR_f1149_441hz_joint_protectedUnt_NOTm1lay-448k-f540-5015.mpc
        */	
        info  = new MP3Info(file[16]);
        assertEquals(info.getFileSize(), 789752L);
        assertEquals(info.getBitRate() , 448); //448-winamp	210-encspot
        assertTrue(info.isCoprighted());	
        assertTrue(!info.isOriginal());	
        assertTrue(info.isPadded()); 	
        assertTrue(!info.isPrivate());	
        assertTrue(!info.isProtected());	
        assertTrue(!info.isRiffWav());		
        assertEquals(info.getFrequency() , 44100);	
        assertEquals(info.getHeaderBitRate() , 448); //448-winamp 210-encspot
        assertEquals(info.getLayer_Numeric() , 1);		
        assertEquals(info.getLengthInSeconds() , 14); //30-encspot
        assertEquals(info.getMode() , "Joint Stereo");	
        assertEquals(info.getVersion_Numeric() , 1.0d, 0D);
        assertEquals(info.getNumberOfFrames() , 65); // 540-winamp 1149-encspot
        assertEquals(info.getEmphasis() , "50/15 ms");
        assertTrue(!info.hasVariableBitRate()); //encspot true
	
        /*
          mpgPRO2layIII_0h_64k_frame2036or2015_22050hz_joint_thomson.mp3
          Mpeg Pro file -	sample provided by manufacturer
        */	
        info  = new MP3Info(file[17] );
        assertEquals(info.getFileSize(), 423521L);
        assertEquals(info.getBitRate() , 64);		
        assertTrue(!info.isCoprighted());		
        assertTrue(!info.isOriginal());	
        assertTrue(info.isPadded()); //true-encspot	
        assertTrue(!info.isPrivate());	
        assertTrue(!info.isProtected());	
        assertTrue(!info.isRiffWav());	
        assertEquals(info.getFrequency() , 22050);	
        assertEquals(info.getHeaderBitRate() , 64);
        assertEquals(info.getLayer_Numeric() , 3);		
        assertEquals(info.getLengthInSeconds() , 52);	
        assertEquals(info.getMode() , "Joint Stereo");	
        assertEquals(info.getVersion_Numeric() , 2.0d, 0D);		
        assertEquals(info.getNumberOfFrames() , 996);	//1015? - 2015-encspot  2036-winamp/////////////////////
        assertEquals(info.getEmphasis() , "none");
        assertTrue(!info.hasVariableBitRate());
	 
        /*
          mpg4_golem160x90first120.avi
          MPEG 4 files contain a 'possible' MP3 header

          info  = new MP3Info(file[18] );
          assertEquals(info.getFileSize(), 743933L);
          assertEquals(info.getBitRate() , 40);		
          assertTrue(!info.isCoprighted());	
          assertTrue(!info.isOriginal());	
          assertTrue(!info.isPadded()); 	
          assertTrue(info.isPrivate());	
          assertTrue(!info.isProtected());	
          assertTrue(!info.isRiffWav());		
          assertEquals(info.getFrequency() , 8000);	
          assertEquals(info.getHeaderBitRate() , 40);
          assertEquals(info.getLayer_Numeric() , 2);		
          assertEquals(info.getLengthInSeconds() , 555);	
          assertEquals(info.getMode() , "Joint Stereo");	
          assertEquals(info.getVersion_Numeric() , 2.5d, 0D);
          assertEquals(info.getNumberOfFrames() , 3857); //39626us - 39624 winamp
          assertEquals(info.getEmphasis() , "none");
          assertTrue(!info.hasVariableBitRate());
        */	
        /*
          corruptFileWithBadHeaders.mp3
        */
        try {
            info  = new MP3Info(file[19]);
            fail("Corrupt file with headers passed!"); //shouldn't make it
        }
        catch (Exception e) {}
	
    }
    /**
     * timing new code
     */
    public void testTIME()
        throws Exception {

        int i = runs;
        long start = 0;
	
        new com.limegroup.gnutella.mp3.MP3Info(file[5]);
	
        start = System.currentTimeMillis();
        while (--i >= 0) {
		
            new com.limegroup.gnutella.mp3.MP3Info(file[i%18]);
	
        }

        System.out.println("NEW time:" + (System.currentTimeMillis()-start));

        /*
///////////////////////////

i = runs;	
new com.limegroup.gnutella.mp3.MP3Info2(file[5] );
start = System.currentTimeMillis();
while (--i >= 0) {
		
new com.limegroup.gnutella.mp3.MP3Info2(file[i%18]);
	
}

System.out.println("OLD time:" + (System.currentTimeMillis()-start));
        */
    }
}
