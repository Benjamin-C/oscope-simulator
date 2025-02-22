package dev.orangeben.scopeviz;

import java.awt.Color;
import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Random;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class App {

    static double pad = 64;
  
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
    
    public static void displayMixerInfo()
    {
        Mixer.Info [] mixersInfo = AudioSystem.getMixerInfo();
        
        for (Mixer.Info mixerInfo : mixersInfo)
        {
            System.out.println("Mixer: " + mixerInfo.getName());
            
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            
            Line.Info [] sourceLineInfo = mixer.getSourceLineInfo();
            for (Line.Info info : sourceLineInfo)
            {
                System.out.println(info.toString());
                showLineInfoFormats(info);
            }
            
            Line.Info [] targetLineInfo = mixer.getTargetLineInfo();
            for (Line.Info info : targetLineInfo)
            {
                System.out.println(info.toString());
                showLineInfoFormats(info);
            }
        }
    }

    public static void feedTestData(ScopeScreen screen) {
        double time = 0;
        System.out.println("Test data!");
        while(true) {
            int x = (int) (screen.WIDTH/2  * (Math.cos(2*Math.PI*time*100) + 1));
            int y = (int) (screen.HEIGHT/2 * (Math.sin(2*Math.PI*time*100) + 1));
            // System.out.println(String.format("Point %.3f (%d, %d) %f %f", time, x, y, 2*Math.PI*time*1000, Math.cos(2*Math.PI*time*1000)));
            screen.addPoint(x, y);
            try {
                Thread.sleep(0, 50000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            time += 0.0001;
        }
    }
    
    public static void main(String[] args) throws Exception {
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        ScopeScreen screen = new ScopeScreen();
        screen.start();
        
        JPanel jp = new JPanel();
        jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
        jp.setBackground(screen.getBackground());

        JPanel infopannel = new JPanel();
        infopannel.setLayout(new BoxLayout(infopannel, BoxLayout.X_AXIS));
        infopannel.setBackground(screen.getBackground());
        infopannel.setForeground(Color.WHITE);

        JLabel spsLabel = new JLabel("SPS: ");
        infopannel.add(spsLabel);
        infopannel.add(Box.createRigidArea(new Dimension(25, 0)));

        JLabel buffsizeLabel = new JLabel("Buff: ");
        infopannel.add(buffsizeLabel);
        
        
        jp.add(infopannel);
        jp.add(screen);

        jf.add(jp);

        jf.pack();
        jf.setVisible(true);

        // feedTestData(screen);
        
        // System.out.println("Mixers");
        // Mixer.Info[] mixerinfos = AudioSystem.getMixerInfo();
        // for(int i = 0; i < mixerinfos.length; i++) {
        //     System.out.println(i + ": " + mixerinfos[i].toString());
        // }
        int interfacenum = 4;
        Mixer.Info mixerinfo = AudioSystem.getMixerInfo()[interfacenum];
        Mixer mixer = AudioSystem.getMixer(mixerinfo);
        System.out.println("Getting stream from " + mixerinfo.toString());

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

        Retimer retimer = new Retimer(4410, 10, 32768) {
			@Override
			public void tick(short l, short r) {
				int x = (int) ((double) l / pad) + screen.WIDTH/2;
                int y = screen.HEIGHT - (int) (((double) r / pad) + screen.HEIGHT/2);
                screen.addPoint(x, y);
			}
        };
        retimer.start();
        
        AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
        
        // showLineInfoFormats(mixer.getSourceLineInfo()[0]);
        
        TargetDataLine line;
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format); // format is an AudioFormat object
        // showLineInfoFormats(info);
        Line.Info li = mixer.getTargetLineInfo()[0];
        if (!AudioSystem.isLineSupported(li)) {
            // Handle the error ... 
            System.out.println("Can't read line");
        }
        // Obtain and open the line.
        try {
            File wav = new File("log.wav");
            FileOutputStream fos = new FileOutputStream(wav);
            byte[] header = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E', 'f', 'm', 't', ' ', 0x10, 0, 0, 0, 1, 0, 2, 0, 0x44, (byte) 0xAC, 0, 0, 0x10, (byte) 0xB1, 2, 0, 4, 0, 0x10, 0, 'd', 'a', 't', 'a', 0, 0, 0, 0};
            fos.write(header);

            for(byte b : header) {
                System.out.print(String.format(" %02X", b));
            }
            System.out.println();

            int caplen = 0;
            boolean capt = true;

            System.out.println("Getting audio from line" + info.toString());
            line = (TargetDataLine) mixer.getLine(info);
            line.open(format);
            line.start();
            byte[] data = new byte[32];
            retimer.clear();
            long last = System.nanoTime();
            while(true) {
                int numBytesRead = line.read(data, 0, data.length);
                if(numBytesRead < data.length) {
                    System.out.println("Insufficient bytes");
                }
                if(capt) {
                    fos.write(data);
                    caplen += data.length;
                }
                // System.out.println("Buffer has " + retimer.getSize() + " samples left");
                int sz = retimer.count();
                buffsizeLabel.setText(String.format("Buff: %d (%.1f%%) ", sz, ((double)sz/retimer.size())*100d));
                for(int i = 0; i < numBytesRead; i += 4) {
                    short l = (short) ((data[i+1] << 8) | (data[i+0] & 0xFF));
                    short r = (short) ((data[i+3] << 8) | (data[i+2] & 0xFF));
                    if(!retimer.add(l, r)) {
                        System.out.println("Buffer full, skipping sample!");
                    }
                }
                if(capt) {
                    // if(caplen > 4410) {
                    //     screen.stop();
                    // }
                    if(!screen.isUpdating()) {
                        System.out.println("Saving capture");
                        capt = false;
                        fos.close();
                        RandomAccessFile raf = new RandomAccessFile(wav, "rw");
                        raf.seek(4);
                        int fsz = caplen + 36;
                        byte[] wavlenarr = {(byte) (fsz & 0xFF), (byte) ((fsz >> 8) & 0xFF), (byte) ((fsz >> 16) & 0xFF), (byte) ((fsz >> 24) & 0xFF)};
                        raf.write(wavlenarr);
                        raf.seek(40);
                        byte[] caplenarr = {(byte) (caplen & 0xFF), (byte) ((caplen >> 8) & 0xFF), (byte) ((caplen >> 16) & 0xFF), (byte) ((caplen >> 24) & 0xFF)};
                        raf.write(caplenarr);
                        raf.close();
                        System.out.println("Done");

                        screen.save("log.png");
                        System.exit(0);
                    }
                }
                long dur = System.nanoTime() - last;
                last = System.nanoTime();
                double sps = (1e9d / (double) dur);
                spsLabel.setText(String.format("SPS: %.3f", sps/1000d));
                // spsLabel.setText(String.format("SPS: %d", dur));
            }
        } catch (LineUnavailableException ex) {
            // Handle the error ... 
        }
        
        
        
        Random r = new Random();
        
        int xd = r.nextInt(5)-2;
        int yd = r.nextInt(5)-2;
        int x = r.nextInt(screen.WIDTH-1);
        int y = r.nextInt(screen.HEIGHT-1);
        while(true) {
            // for(int j = 0; j < 8; j++) {
            //     for(int i = 0; i < 500; i++) {
            //         screen.addPoint(i, j*4);
            //         Thread.sleep(1);
            //     }
            // }
            // Thread.sleep(16);
            // screen.stop();
            
            x += xd;
            y += yd;
            
            if(x < 0) {
                x = 0;
                xd *= -1;
            }
            if(y < 0) {
                y = 0;
                yd *= -1;
            }
            if(x > screen.WIDTH-1) {
                x = screen.WIDTH-1;
                xd *= -1;
            }
            if(y > screen.HEIGHT-1) {
                y = screen.HEIGHT-1;
                yd *= -1;
            }
            
            screen.addPoint((short) x, (short) y);
            Thread.sleep(1);
        }
    }
}
