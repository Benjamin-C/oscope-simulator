package dev.orangeben.scopeviz;

public class SyncSampleBuffer {

    /** The max number of samples the buffer can hold */
    private int size = 0;
    /** The left channel samples */
    private short[] lsamp;
    /** The right channel samples */
    private short[] rsamp;
    /** The next location to read from */
    private int readhead = 0;
    /** The next location to write to */
    private int writehead = 0;
    /** If the buffer is empty */
    private boolean empty = true;
    /** Buffer lock */
    private Object lock;

    public SyncSampleBuffer(int size) {
        this.size = size;
        lsamp = new short[size];
        rsamp = new short[size];
        lock = new Object();
    }

    /**
     * Clears the buffer
     */
    public synchronized void clear() {
        readhead = 0;
        writehead = 0;
        empty = true;
    }

    /**
     * Adds a sample to the bufffer
     * @param l The left value
     * @param r The right value
     * @return If the value was added
     */
    public synchronized boolean add(short l, short r) {
        if(!isFull()) {
            lsamp[writehead] = l;
            rsamp[writehead] = r;
            writehead = ++writehead % size;
            empty = false;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets the next sample from the buffer
     * @return The sample as an array, or an empty array if the buffer is empty
     */
    public synchronized short[] get() {
        if(!empty) {
            short[] ret = {lsamp[readhead], rsamp[readhead]};
            readhead = ++readhead % size;
            if(readhead == writehead) {
                empty = true;
            }
            return ret;
        } else {
            short[] ret = {};
            return ret;
        }
    }

    /**
     * Checks if the buffer is empty
     * @return
     */
    public synchronized boolean isEmpty() {
        return empty;
    }

    /**
     * Checks if the buffer is full
     * @return
     */
    public synchronized boolean isFull() {
        return readhead == writehead && !empty;
    }

    public int size() {
        return size;
    }

    public synchronized int count() {
        if(isFull()) {
            return size;
        }
        int tw = writehead;
        if(writehead < readhead) {
            tw += size;
        }
        return tw - readhead;
    }
}
