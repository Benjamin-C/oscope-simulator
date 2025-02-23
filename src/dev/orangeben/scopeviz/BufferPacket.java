package dev.orangeben.scopeviz;

public class BufferPacket {

    private int[] ldata;
    private int[] rdata;
    private int rp = 0;
    private int wp = 0;

    public BufferPacket(int size) {
        ldata = new int[size];
        rdata = new int[size];
    }

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

    public int readR() {
        if(rp <= wp) {
            return rdata[rp];
        } else {
            return Integer.MIN_VALUE;
        }
    }

    public int readL() {
        if(rp <= wp) {
            return ldata[rp];
        } else {
            return Integer.MIN_VALUE;
        }
    }

    public void nextRead() {
        rp++;
    }

    public boolean hasMoreData() {
        return rp < wp;
    }

    public int dataCount() {
        return wp;
    }

    public void seek(int loc) {
        if(loc < wp) {
            rp = loc;
        }
    }
}
