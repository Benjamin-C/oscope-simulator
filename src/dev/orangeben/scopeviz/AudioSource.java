package dev.orangeben.scopeviz;

import javax.naming.ConfigurationException;

public interface AudioSource {

    /** Completes any initialization for the source */
    public void start() throws ConfigurationException;
    /** Completes any cleanup for the source */
    public void stop();
    /** Checks if the source ready to provide data  */
    public boolean ready();
    /**
     * Reads data from the source, up to a specified number of samples
     * @param count The max number of samples to read
     * @return The packet of read samples
     */
    public BufferPacket read(int count);
    /**
     * Ignores the next several samples. If there aren't that many samples, all remaining samples will be ignored, but no future samples.
     * @param count The max number of samples to ignore.
     */
    public void skip(int count);
    /**
     * Gets the samplerate the source provides samples at.
     * @return The samplerate of the buffer. May return 0 if the buffer doesn't have a defined samplerate.
     */
    public float getSamplerate();
    /**
     * Gets the max value a sample can be. The minimum value is -getSampleMax()-1
     * @return The max sample value
     */
    public int getSampleMax();

}
