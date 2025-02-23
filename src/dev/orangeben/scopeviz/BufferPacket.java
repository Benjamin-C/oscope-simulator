package dev.orangeben.scopeviz;

public class BufferPacket {

    private int[] ldata;
    private int[] rdata;
    private int rp = 0;
    private int wp = 0;

    public BufferPacket(int size) {
        if(size <= 0) {
            throw new IllegalArgumentException("Size must be >0");
        }
        ldata = new int[size];
        rdata = new int[size];
    }

    /**
     * Adds a sample to the packet
     * @param l the left value
     * @param r the right value
     * @return If the sample was added
     */
    public boolean write(int l, int r) {
        if(wp < ldata.length) {
            ldata[wp] = l;
            rdata[wp] = r;
            wp++;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Reads the next sample from the packet
     * @return An array with the L and R data
     */
    public int[] read() {
        if(rp <= wp) {
            int[] ret = {ldata[rp], rdata[rp]};
            rp++;
            return ret;
        } else {
            int[] ret = {};
            return ret;
        }
    }

    /**
     * Reads the next R value from the packet. Call {@link #nextRead()} after reading L and R to get the next sample
     * @return the next R value
     */
    public int readR() {
        if(rp <= wp) {
            return rdata[rp];
        } else {
            return Integer.MIN_VALUE;
        }
    }

    /**
     * Reads the next L value from the packet. Call {@link #nextRead()} after reading L and R to get the next sample
     * @return the next L value
     */
    public int readL() {
        if(rp <= wp) {
            return ldata[rp];
        } else {
            return Integer.MIN_VALUE;
        }
    }

    /**
     * Increments the read pointer. Only call after getting the value from both {@link #readL()} and {@link #readR()}
     */
    public void nextRead() {
        rp++;
    }

    /**
     * Checks if the packet has more data to read
     * @return If there are more samples to read
     */
    public boolean hasMoreData() {
        return rp < wp;
    }

    /**
     * Gets the number of packets in the buffer
     * @return The total number of packets
     */
    public int dataCount() {
        return wp;
    }

    /**
     * Seeks in the packet to allow you to read previous samples
     * @param loc the new index to read from
     * @throws IllegalArgumentException if the target location isn't allowed
     */
    public void seek(int loc) {
        if(loc >= 0 && loc < wp) {
            rp = loc;
        } else {
            throw new IllegalArgumentException(String.format("Invalid seek location %d for size %d", loc, wp));
        }
    }
}
