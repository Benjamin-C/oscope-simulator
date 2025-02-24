package dev.orangeben.scopeviz;

public class CircleSource implements AudioSource {

    int samplerate;
    int samplecount;
    int[] xbuff;
    int[] ybuff;
    int reader = 0;

    public CircleSource(int samplerate, int samplecount) {
        this.samplerate = samplerate;
        this.samplecount = samplecount;
        xbuff = new int[samplecount+1];
        ybuff = new int[samplecount+1];

        for(double d = 0; d < Math.PI*2; d += (Math.PI*2)/samplecount) {
            xbuff[reader] = (int)  (Short.MAX_VALUE * Math.cos(d));
            ybuff[reader] = (int) -(Short.MAX_VALUE * Math.sin(d));
            reader++;
        }
        reader = 0;
    }

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public boolean ready() {
        return true;
	}

	@Override
	public BufferPacket read(int count) {
        BufferPacket bp = new BufferPacket(count);
        for(int i = 0; i < count; i++) {
            bp.write(xbuff[reader], ybuff[reader]);
            reader = ++reader % samplecount;
        }
        return bp;
	}

	@Override
	public void skip(int count) {
        reader = (reader + count) % samplecount;
	}

	@Override
	public float getSamplerate() {
        return samplerate;
	}

	@Override
	public int getSampleMax() {
        return Short.MAX_VALUE;
	}
}
