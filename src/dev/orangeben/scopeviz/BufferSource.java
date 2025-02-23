package dev.orangeben.scopeviz;

import javax.naming.ConfigurationException;

public class BufferSource implements AudioSource {

    protected SyncSampleBuffer buff;
    protected float samplerate;
    protected int maxval;

    public BufferSource(float samplerate, int maxval) {
        createBuffer(samplerate, maxval);
    }

	@Override
	public void start() throws ConfigurationException{
        // Don't need to do anything special here
	}

	@Override
	public void stop() {
        buff.clear();
	}

	@Override
	public boolean ready() {
        // Has no setup or cleanup to do, so always running
        return true;
	}

	@Override
	public BufferPacket read(int count) {
        return buff.getMany(count);
	}

	@Override
	public void skip(int count) {
        buff.skip(count);
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
