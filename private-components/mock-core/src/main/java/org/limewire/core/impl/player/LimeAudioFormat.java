package org.limewire.core.impl.player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.limewire.player.api.AudioSource;

//import org.limewire.player.api.AudioSource;
//import org.limewire.util.FileUtils;
//import org.limewire.util.GenericsUtils;
//import org.limewire.util.GenericsUtils.ScanMode;
//import org.tritonus.share.sampled.TAudioFormat;
//import org.tritonus.share.sampled.file.TAudioFileFormat;

/**
 * <p>
 *  This handles creation/destruction of an audio source. This includes
 *  extracting audio properties about the source and any decoding that may
 *  need to take place. Once created, the owner can safely read/write from 
 *  the input stream and to the dataline.
 *  </p>
 *  <p>
 *  When opening an input stream to read from, encoded formats such as
 *  .mp3, .flac, .ogg, .mp4, etc.., must be wrapped in their own unique 
 *  audioInputStream which will decode all input streams into a PCM format.
 *  PCM is a format that the sound card will understand.
 *  </p>
 *  The process of initializing a new song to read from is as follows:
 *  <pre>
 *      - create an AudioInputStream (this creates AudioFormat information about the
 *              encoding of the input stream, such as # of channels, sameple rate, 
 *              encoding format, etc.)
 *      - create a decoded AudioInputStream (now that we have the AudioFormat information
 *              about the encoded audio source, we can construct a proper AudioInputStream
 *              that will decode the audio source into PCM format)
 *      - Optional: extract audio properties about stream
 *      - create a SourceDataLine (depending on a the AudioFormat of the input stream and the
 *              sound card, a proper data line must be created to write to. An input Stream 
 *              in mono will be handled differently than one in stereo, etc.) The sourceDataLine
 *              will write the information to the sound card for playback.
 *      - reading/writing - this is handled by the Object that created this instance
 *      - Finally: upon completion of writing the song, the input streams and 
 *              sourceDataLine must be closed and discarded.
 *  </pre>
 */
public class LimeAudioFormat {
    
    /**
     * Loads a file into the player and initializes all the input and output streams.
     * After loading the audio source, it is safe to begin reading/writing the audio
     * source to the data line.
     */
    public LimeAudioFormat(File file, long position) throws UnsupportedAudioFileException, 
                    IOException, LineUnavailableException, NullPointerException {
//        this( new AudioSource(file), position );
    }

    /**
     * Loads a stream into the player and initializes all the input and output streams
     * After loading the audio source, it is safe to begin reading/writing the audio
     * source to the data line.
     */
    public LimeAudioFormat(InputStream stream, long position) throws UnsupportedAudioFileException, 
                    IOException, LineUnavailableException, NullPointerException {
//        this( new AudioSource(stream), position );
    }

    /**
     * Loads an audioSource into the player and initializes all the input and output streams
     * After loading the audio source, it is safe to begin reading/writing the audio
     * source to the data line.
     */ 
    public LimeAudioFormat(AudioSource audioSource, long position) throws UnsupportedAudioFileException, 
                    IOException, LineUnavailableException, NullPointerException {

    }

    /**
     * Creates an audioInputStream for reading from. An audioInputStream is 
     * an inputStream with a specified audio format and length. Unlike 
     * InputStreams, the length is expressed in frames not bytes and the 
     * AudioFormat contains specifications for how the input stream is encoded 
     * such as number of bytes per frame, sample rate, # of channels, etc.. 
     *  
     *  NOTE: The audioInputStream returned here is not guarenteed to
     *  write to the sound card. Most audio sources, even .wav  files already 
     *  in PCM format, need to be decoded to a proper format that the 
     *  sourceDataLine can understand prior to reading from.
     * 
     * @param source - audio source to read from, either a file, url or 
     *      inputStream
     * @param skip - number of frames from the begining of the file to skip
     * @return AudioInputStream - based on <code>source</code> creates an input 
     *      stream containing audioFormat properties about the encoding of the
     *      stream
     */
    public static AudioInputStream createAudioInputStream(AudioSource source,
            long skip) throws UnsupportedAudioFileException, IOException, NullPointerException {
        return null;
    }

    /**
     * Creates a decoded audioInputStream. All audio input streams must be in a
     * PCM format compatible with the OS and sound card in order to written 
     * correctly by the sound card. To write to the soundcard we open a source 
     * data line to read data from the input stream. The sourceDataLine expects 
     * data to be in a specific audio format regardless of how the data is encoded. 
     * 
     * To ensure that all supported formats are decoded properly, the original 
     * audioInputStream is decoded into a new audioInputStream. The java AudioSystem
     * uses a bit of reflection to create a new AudioInputStream which can decode 
     * a given audioInputStream into a PCM formatted stream. 
     * 
     * 
     * @param audioInputStream - encoded inputStream to read from which contains 
     *          specific audioFormat properties such as a number of channels, 
     *          encoding method, sample rate, etc..
     * @return AudioInputStream - a decoded audioInputStream in PCM format
     */
    public static AudioInputStream createDecodedAudioInputStream(
            AudioInputStream audioInputStream) {
        return null;
    }

    /**
     * Opens a sourceDataLine for writing to an audio card from a given inputstream. 
     * SourceDataLines are the link between the source of an audiostream and the java Mixer. 
     * From the Mixer, all the input streams are combined and written to the sound card. 
     * SourceDataLines wrap a given audioInputStream and ensures that
     * all inputs to the mixer are in the same format.
     * 
     * Each audioInputStream contains an audioFormat( ie. # of channels, frame size, sample
     * rate, etc.). A SourceDataLine is created based on the audioFormat's properties. 
     * 
     * @param audioInputStream - the decoded audio input stream that is being read from
     * @return SourceDataLine - a properlly formated data line to write to based on the 
     *              audio format of the audioInputStream
     */
    private SourceDataLine createSourceDataLine(AudioInputStream audioInputStream)
            throws LineUnavailableException {
        return createSourceDataLine(audioInputStream, -1);
    }

    /**
     * Opens a sourceDataLine for writing to an audio card from a given inputstream. 
     * SourceDataLines are the link between the source of an audiostream and the java Mixer. 
     * From the Mixer, all the input streams are combined and written to the sound card. 
     * SourceDataLines wrap a given audioInputStream and ensures that
     * all inputs to the mixer are in the same format.
     * 
     * Each audioInputStream contains an audioFormat( ie. # of channels, frame size, sample
     * rate, etc.). A SourceDataLine is created based on the audioFormat's properties. 
     * 
     * @param audioInputStream - the decoded audio input stream that is being read from
     * @return SourceDataLine - a properlly formated data line to write to based on the 
     *              audio format of the audioInputStream
     */
    private SourceDataLine createSourceDataLine(
            AudioInputStream audioInputStream, int bufferSize)
            throws LineUnavailableException {
        return null;
    }

    /**
     * Creates a map of properties about the current inputstream. Unlike many inputStreams,
     * audioInputStreams have a variety of extra properties associated with them such as
     * <pre>
     *  - frame size
     *  - sample rate
     *  - frames per second
     *  - audio type
     *  - length in # of frames
     *  - # of audio channels
     *  - etc.
     * </pre>
     * This information is often useful to the application that initiated the song. This information
     * is extracted in case another class wishes to use it. 
     * 
     * @param source - the audio source that the audioInputStream is created from for reading
     * @return a Map<String,Object> containing properties about the audio source
     */
    private static Map<String, Object> createProperties(AudioSource source)
            throws UnsupportedAudioFileException, IOException {
        return null;
    }
    

    /**
     * @return the audio source of the inputStream
     */
    public AudioSource getSource() {
        return null;
    }

    /**
     * @return the audioInputStream for reading from
     */
    public AudioInputStream getAudioInputStream() {
        return null;
    }

    /**
     * @return the sourcedataline for writing to
     */
    public SourceDataLine getSourceDataLine() {
        return null;
    }

    /**
     * @return the properties associated with this audio source
     *      such as sampleRate, framesize, number of frames, etc..
     */
    public Map<String, Object> getProperties() {
        return null;
    }

    /**
     * @return the total number of frames in the input stream 
     */
    public long totalLength() {
        return 0;
    }

    /**
     * @return the number of frames left to read.
     */
    public int available() {
        int avail = -1;
        return avail;
    }
       
    /**
     * Prior to writing to a new or stopped sourceDataLine, the dataLine needs to
     * be opened
     */
    public void startSourceDataLine(){
    }
    
    /**
     * Stops the current sourceDataLine from writing. This should be called when the 
     * stream has been paused with intent to reopen it
     */
    public void stopSourceDataLine(){
    }
    
    /**
     * @return frame position in the current song being played
     */
    public int getEncodedStreamPosition() {
        return 1;
    }

    /**
     * Seeks to a current position in the song.
     * 
     * @param position - position from the begining of the file to seek to
     * @return - the number of bytes actually skipped
     */
    public long seek(long position) {
       return -1; 
    }
    
    /**
     *  Closes all the open streams. This is a convienance method for when the 
     *  the song is done being read from. 
     */
    public void closeStreams() {
    }
    
    /**
     * Returns Gain value.
     */
    public float getGainValue() {
            return 0.0F;
    }

    /**
     * Gets max Gain value.
     */
    public float getMaximumGain() {
            return 0.0F;
    }

    /**
     * Gets min Gain value.
     */
    public float getMinimumGain() {
            return 0.0F;
    }

    /**
     * Returns true if Gain control is supported.
     */
    public boolean hasGainControl() {
        return false;
    }

    /**
     * Sets the gain(volume) for the outputline
     * 
     * @param gain - [0.0 <-> 1.0]
     * @throws IOException - thrown when the soundcard does not support this
     *         operation
     */
    public void setGain(double fGain) throws IOException {
    }


}