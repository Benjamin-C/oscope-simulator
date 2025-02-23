package dev.orangeben.scopeviz;

import java.io.RandomAccessFile;
import java.util.Arrays;

import javax.naming.ConfigurationException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

public class CaptureSource extends BufferSource {

    private boolean ready = false; 
    private int interfacenum = 4;
    private Mixer.Info mixerinfo;
    private Mixer mixer;
    private AudioFormat format;
    private DataLine.Info datalineinfo;
    private TargetDataLine line;
    private Line.Info lineinfo;
    private final int AUDIO_BUFF_SIZE = 32;
    private byte[] data = new byte[AUDIO_BUFF_SIZE];
    private Thread audioReader;

    public CaptureSource() {
        super(0, 0);
    }

	@Override
	public void start() throws ConfigurationException {
        if(ready) {
            stop();
            ready = false;
        }
        mixerinfo = AudioSystem.getMixerInfo()[interfacenum];
        mixer = AudioSystem.getMixer(mixerinfo);
        format = new AudioFormat(44100, 16, 2, true, false);
        createBuffer(format.getSampleRate(), (int) Math.pow(2, format.getSampleSizeInBits()) - 1);

        datalineinfo = new DataLine.Info(TargetDataLine.class, format);
        lineinfo = mixer.getTargetLineInfo()[0];
        if(!AudioSystem.isLineSupported(lineinfo)) {
            throw new ConfigurationException("Can't open the desired audio line");
        }
        System.out.println("Getting audio from line" + datalineinfo.toString());
        try {
			line = (TargetDataLine) mixer.getLine(datalineinfo);
            ;
            line.open(format, AUDIO_BUFF_SIZE);
            line.start();
            ready = true;
            System.out.println("Getting stream from " + mixerinfo.toString());

            audioReader = new Thread("audio-reader") {
                @Override
                public void run() {
                    while(ready) {
                        int numBytesRead = line.read(data, 0, data.length);
                        BufferPacket pak = new BufferPacket(numBytesRead);
                        for(int i = 0; i < numBytesRead; i += 4) {
                            short l = (short) ((data[i+1] << 8) | (data[i+0] & 0xFF));
                            short r = (short) ((data[i+3] << 8) | (data[i+2] & 0xFF));
                            if(!pak.write(l, r)) {
                                System.out.println("Buffer full, skipping sample!");
                            }
                        }
                        write(pak);
                    }
                }
            };
            audioReader.start();

		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
        if(ready) {
            ready = false;
            try {
                audioReader.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            line.stop();
        }
	}

	@Override
	public boolean ready() {
		return ready;
	}

    static void showLineInfoFormats(final Line.Info lineInfo) {
        if (lineInfo instanceof DataLine.Info) {
            final DataLine.Info dataLineInfo = (DataLine.Info)lineInfo;
            System.out.println(dataLineInfo);
            Arrays.stream(dataLineInfo.getFormats())
            .forEach(format -> System.out.println("    " + format.toString()));
        } else {
            System.out.println("Not dataline");
        }
    }

    public static void displayMixerInfo() {
        Mixer.Info [] mixersInfo = AudioSystem.getMixerInfo();
        
        for (Mixer.Info mixerInfo : mixersInfo) {
            System.out.println("Mixer: " + mixerInfo.getName());
            
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            
            Line.Info [] sourceLineInfo = mixer.getSourceLineInfo();
            for (Line.Info info : sourceLineInfo) {
                System.out.println(info.toString());
                showLineInfoFormats(info);
            }
            
            Line.Info [] targetLineInfo = mixer.getTargetLineInfo();
            for (Line.Info info : targetLineInfo) {
                System.out.println(info.toString());
                showLineInfoFormats(info);
            }
        }
    }

    // System.out.println("Mixers");
        // Mixer.Info[] mixerinfos = AudioSystem.getMixerInfo();
        // for(int i = 0; i < mixerinfos.length; i++) {
        //     System.out.println(i + ": " + mixerinfos[i].toString());
        // }
        
        
        // System.out.println("Source Lines");
        // for(Line.Info l : mixer.getSourceLineInfo()) {
        //     System.out.println(l.toString());
        // }
        // System.out.println("Target Lines");
        // for(Line.Info l : mixer.getTargetLineInfo()) {
        //     System.out.println(l.toString());
        // }
        
        // displayMixerInfo();
        
        // System.out.println("Mixer: " + mixerinfo.getName());
        
        // Line.Info [] sourceLineInfo = mixer.getSourceLineInfo();
        // for (Line.Info info : sourceLineInfo)
        // {
        //     System.out.println(info.toString());
        //     showLineInfoFormats(info);
        // }
        
        // Line.Info [] targetLineInfo = mixer.getTargetLineInfo();
        // for (Line.Info info : targetLineInfo)
        // {
        //     System.out.println(info.toString());
        //     showLineInfoFormats(info);
        // }
}
