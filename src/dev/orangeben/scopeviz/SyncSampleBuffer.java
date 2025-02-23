package dev.orangeben.scopeviz;

public class SyncSampleBuffer {

    /** The max number of samples the buffer can hold */
    private int maxSize = 0;
    /** The left channel samples */
    private int[] lsamp;
    /** The right channel samples */
    private int[] rsamp;
    /** The next location to read from */
    private int readhead = 0;
    /** The next location to write to */
    private int writehead = 0;
    /** If the buffer is empty */
    private boolean empty = true;

    public SyncSampleBuffer(int size) {
        if(size < 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        this.maxSize = size;
        lsamp = new int[size];
        rsamp = new int[size];
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
    public synchronized boolean add(int l, int r) {
        if(!isFull()) {
            lsamp[writehead] = l;
            rsamp[writehead] = r;
            writehead = ++writehead % maxSize;
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
    public synchronized int[] get() {
        if(!empty) {
            int[] ret = {lsamp[readhead], rsamp[readhead]};
            readhead = ++readhead % maxSize;
            if(readhead == writehead) {
                empty = true;
            }
            return ret;
        } else {
            int[] ret = {};
            return ret;
        }
    }

    /**
     * Gets up to count samples from the buffer. May return less samples if the buffer doesn't have enough available.
     * @param count The max number of samples to get
     * @return The samples
     */
    public synchronized BufferPacket getMany(int count) {
        if(count <= 0) {
            throw new IllegalArgumentException("Count must be >0");
        }
        BufferPacket ret = new BufferPacket(count);
        for(int i = 0; i < count; i++) {
            if(!empty) {
                ret.write(lsamp[readhead], rsamp[readhead]);
                readhead = ++readhead % maxSize;
                if(readhead == writehead) {
                    empty = true;
                }
            } else {
                break;
            }
        }
        return ret;
    }

    /**
     * Adds many samples to the buffer. The write pointer will be left on the next sample to be added if not all samples are added.
     * @param pak The samples to add
     * @return If all samples were added.
     */
    public synchronized boolean addMany(BufferPacket pak) {
        pak.seek(0);
        for(int i = 0; i < pak.dataCount(); i++) {
            if(!isFull()) {
                lsamp[writehead] = pak.readL();
                rsamp[writehead] = pak.readR();
                pak.nextRead();
                writehead = ++writehead % maxSize;
                empty = false;
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * Skips the next few samples
     * @param count The number of samples to skip
     * @throws IllegalArgumentException if count is negative
     */
    public synchronized void skip(int count) {
        if(count < 0) {
            throw new IllegalArgumentException("Skip count must be positive");
        }
        if(count > size()) {
            clear();
        } else {
            readhead = (readhead + count) % maxSize;
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

    /**
     * Gets the max size of the buffer
     * @return the max size
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Gets the number of packets currently in the buffer
     * @return the number of packets
     */
    public synchronized int size() {
        if(isFull()) {
            return maxSize;
        }
        int tw = writehead;
        if(writehead < readhead) {
            tw += maxSize;
        }
        return tw - readhead;
    }
}
