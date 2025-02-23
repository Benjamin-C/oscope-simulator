package dev.orangeben.scopeviz;

public interface AudioSource {

    /** Completes any initialization for the source */
    public void start();
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

}
