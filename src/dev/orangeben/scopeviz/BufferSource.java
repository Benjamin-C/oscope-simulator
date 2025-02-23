package dev.orangeben.scopeviz;

public class BufferSource implements AudioSource {

    private SyncSampleBuffer buff;

    public BufferSource(int buffsize) {
        buff = new SyncSampleBuffer(buffsize);
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
}
