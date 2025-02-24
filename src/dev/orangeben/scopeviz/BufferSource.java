package dev.orangeben.scopeviz;

public class BufferSource implements AudioSource {

    protected SyncSampleBuffer buff;
    protected float samplerate;
    protected int maxval;
    protected boolean ready = true;

    public BufferSource(float samplerate, int maxval) {
        createBuffer(samplerate, maxval);
    }

	@Override
	public void start() {
        // Don't need to do anything special here
	}

	@Override
	public void stop() {
        buff.clear();
	}

	@Override
	public boolean ready() {
        // Has no setup or cleanup to do, so always running
        return ready;
	}

	@Override
	public BufferPacket read(int count) {
        if(ready) {
            return buff.getMany(count);
        } else {
            return new BufferPacket(0);
        }
	}

	@Override
	public void skip(int count) {
        if(ready) {
            buff.skip(count);
        }
	}

    @Override
    public float getSamplerate() {
        return samplerate;
    }

    /**
     * Writes data into the buffer
     * @param pkt the packet of data to write
     */
    public void write(BufferPacket pkt) {
        buff.addMany(pkt);
    }

    public void write(int l, int r) {
        buff.add(l, r);
    }

    /**
     * Gets the number of samples in the buffer
     * @return The number of samples in the buffer
     */
    public int size() {
        return buff.size();
    }

    /**
     * Gets the max number of samples in the buffer
     * @return The max nubmer of samples
     */
    public int maxSize() {
        return buff.getMaxSize();
    }

    /**
     * Recreates the audio buffer at the new samplerate. Any existing samples are lost.
     * @param samplerate The new samplerate
     */
    protected void createBuffer(float samplerate, int maxval) {
        this.samplerate = samplerate;
        this.maxval = maxval;
        buff = new SyncSampleBuffer((int) (samplerate/10));
    }

	@Override
	public int getSampleMax() {
        return maxval;
	}
}
