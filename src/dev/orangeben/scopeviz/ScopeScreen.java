package dev.orangeben.scopeviz;

import javax.imageio.ImageIO;
import javax.naming.ConfigurationException;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ScopeScreen extends JPanel {

    /** The size of the display */
    public final int SIZE = 1024;
    /** The color of the display */
    private Color color;
    /** Frame updater thread */
    private Thread updater;
    /** If the display is running */
    private volatile boolean updating;
    /** The sample rate of the incoming data. Must be an integer multiple of the target FPS */
    // private final double samplesPerSecond = 44100;
    /** The target nubmer of frames per seconds to display */
    private final double targetFPS = 60;
    /** The number of nanoseconds per frame */
    // private final int samplesPerFrame = (int) (samplesPerSecond / targetFPS);
    /** The actual observed FPS */
    private volatile double fps = 0;
    /** The actual scope screen */
    private BufferedImage screen;
    /** The label that holds the scope screen */
    private JLabel screenLabel;
    /** Label to hold the current FPS */
    private JLabel fpslabel;
    /** Checkbox to control line drawing */
    private JCheckBox linecheck;
    /** If lines are being drawn */
    private boolean drawLines = true;
    /** Checkbox to control point drawing */
    private JCheckBox pointcheck;
    /** If points are being rdrawn */
    private boolean drawPoints = false;
    /** Panel of screen controls */
    private JPanel controlsPanel;
    /** Update stop button */
    private JButton stopButton;
    /** Approximate screen decay time in seconds */
    private double decay = 0.1;
    /** x or y of previously drawn point */
    private int lx, ly = 0;
    /** The source of the data to draw */
    private AudioSource source;
    /** The alpha to dim the screen over time */
    private final int decayRate;
    /** The diagonal screen distance in px */
    private final double maxdist = Math.sqrt(2)*SIZE;
    /** The graphics interface to the screen */
    private Graphics g;

    /**
     * Creates a new ScopeScreen
     * @param source The source of the audio to display
     */
    public ScopeScreen(AudioSource source) {
        if(source == null) {
            throw new NullPointerException("Source may not be null");
        }
        setBackground(new Color(8, 8, 8));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.source = source;
        
        controlsPanel = new JPanel();
        controlsPanel.setBackground(getBackground());
        
        stopButton = new JButton("Stop");
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(updating) {
                    stop();
                } else {
                    start();
                }
            }
        });
        controlsPanel.add(stopButton);
        
        fpslabel = new JLabel();
        controlsPanel.add(fpslabel);

        linecheck = new JCheckBox("Draw lines");
        linecheck.setBackground(getBackground());
        linecheck.setForeground(color);
        linecheck.setSelected(drawLines);
        linecheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                drawLines = linecheck.isSelected();
            }
        });
        controlsPanel.add(linecheck);

        pointcheck = new JCheckBox("Draw points");
        pointcheck.setBackground(getBackground());
        pointcheck.setForeground(color);
        pointcheck.setSelected(drawPoints);
        pointcheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                drawPoints = pointcheck.isSelected();
            }
        });
        controlsPanel.add(pointcheck);

        add(controlsPanel);

        color = new Color(255, 128, 0);
        decayRate = (int) (256 / (decay*targetFPS)) * 3;

        screen = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        g = screen.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 1, 1);
        g.setColor(new Color(0, 0, 0, decayRate));
        for(int i = 0; i < (int) (decay*targetFPS); i++) {
            g.fillRect(0, 0, 1, 1);
        }
        g.setColor(new Color(screen.getRGB(0, 0)));
        g.fillRect(0, 0, SIZE, SIZE);
        // g.dispose();
        screenLabel = new JLabel(new ImageIcon(screen));
        screenLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(screenLabel);
    }

    /**
     * Saves the screen to a PNG
     * @param filename where to save the screen
     * @throws IOException if the file can't be written
     */
    public void save(String filename) throws IOException {
        File out = new File(filename);
        ImageIO.write(screen, "png", out);
    }

    /**
     * Redraws the screen
     */
    private void redraw() {
        synchronized(g) {
            g.setColor(new Color(0, 0, 0, decayRate));
            g.fillRect(0, 0, SIZE, SIZE);
            g.setColor(color);
            BufferPacket pak = source.read((int) (source.getSamplerate()/targetFPS));
            int pad = (int) Math.round((double) Short.MAX_VALUE / (SIZE/2));
            while(pak.hasMoreData()) {
                int l = pak.readL();
                int r = pak.readR();
                int x = (int) ((double) l / pad) + SIZE/2;
                int y = SIZE - (int) (((double) r / pad) + SIZE/2);
                if((x >= 0 && x <= SIZE-1 && y >= 0 && y <= SIZE-1)) {
                    if(drawLines) {
                        int xs = (lx-x);
                        int ys = (ly-y);
                        double bright = 1d - Math.pow(Math.sqrt((xs*xs)+(ys*ys))/maxdist, 0.08);
                        int rv = (int) (color.getRed() * bright)  ;
                        int gv = (int) (color.getGreen() * bright);
                        int bv = (int) (color.getBlue() * bright) ;
                        try {
                            g.setColor(new Color(rv, gv, bv));
                            g.drawLine(lx, ly, x, y);
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("rgb:" + rv + " " + gv + " " + bv);
                        }
                        if(drawPoints) {
                            screen.setRGB(lx, ly, color.getRGB());
                        }
                        lx = x;
                        ly = y;
                    } else if(drawPoints) {
                        screen.setRGB(x, y, color.getRGB());
                    }
                }
                pak.nextRead();
            }
        }
        screenLabel.repaint();
    }

    /**
     * Checks if the updater is running
     * @return If the updater is running
     */
    public boolean isUpdating() {
        return updating;
    }

    /** Starts the updater */
    public void start() {
        try {
			source.start();
		} catch (ConfigurationException e) {
			e.printStackTrace();
            return;
		}
        updating = true;
        updater = new Thread("screen_updater") {
            long last = System.nanoTime();
            long fpslast = System.nanoTime();
            double targetFrameTime = 1e9  / targetFPS;
            @Override
            public void run() {
                while(updating) {
                    try {
                        double tdiff = (last + targetFrameTime) - System.nanoTime();
                        if(tdiff <= 0) {
                            redraw();
                            double ntdiff = System.nanoTime() - (last + targetFrameTime);
                            fps = 1e9d / (System.nanoTime() - fpslast);
                            fpslast = System.nanoTime();
                            fpslabel.setText(String.format("FPS: %.1f", fps));
                            if(ntdiff > targetFrameTime) {
                                double skip = ntdiff / targetFrameTime;
                                System.out.println(String.format("Behind, skipping %.1f frames", skip));
                                int sc = (int) Math.floor(skip);
                                last += targetFrameTime * sc;
                                source.skip((int) (sc*(source.getSamplerate()/targetFPS)));
                                last = System.nanoTime();
                                fpslabel.setForeground(Color.RED);
                            } else {
                                last += targetFrameTime;
                                fpslabel.setForeground(Color.GREEN);
                            }
                        } else {
                            try {
                                sleep((long) (tdiff / 1e6), (int) (tdiff % 1e6));
                            } catch (InterruptedException e) {
                                // continue as normal
                            }
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                        e.printStackTrace();
                    }
                }

            }
        };
        updater.start();
        stopButton.setText("Stop");
    }

    /** Stops the updater */
    public void stop() {
        updating = false;
        try {
            updater.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        source.stop();
        stopButton.setText("Start");
    }
}
