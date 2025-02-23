package dev.orangeben.scopeviz;

import javax.imageio.ImageIO;
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

public class ScopeScreen extends JPanel {

    public final int SIZE = 1024;
    @Deprecated
    /**
     * Width of the screen
     */
    public final int WIDTH = SIZE;
    @Deprecated
    /** Height of the screen */
    public final int HEIGHT = SIZE;
    private Color color;
    private Thread updater;
    private volatile boolean updating;
    private final double samplesPerSecond = 44100;
    private final double targetFPS = 60;
    private final int samplesPerFrame = (int) (samplesPerSecond / targetFPS);
    private volatile double fps = 0;
    private BufferedImage screen;
    private JLabel screenLabel;
    private JLabel fpslabel;
    private JCheckBox linecheck;
    private boolean drawLines = true;
    private JCheckBox pointcheck;
    private boolean drawPoints = false;
    private JPanel controlsPanel;
    private JButton stopButton;
    private double decay = 0.1;

    private int lx, ly = 0;

    private SyncSampleBuffer bigbuff;

    private final int decayRate;
    private final double maxdist = Math.sqrt(2)*SIZE;

    private Graphics g;

    public ScopeScreen() {
        setBackground(new Color(8, 8, 8));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        bigbuff = new SyncSampleBuffer((int) samplesPerSecond);
        
        controlsPanel = new JPanel();
        // controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.X_AXIS));
        controlsPanel.setBackground(getBackground());
        // controlsPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
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
     */
    public void save(String filename) {
        try {
            File out = new File(filename);
            ImageIO.write(screen, "png", out);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void redraw() {
        synchronized(g) {
            g.setColor(new Color(0, 0, 0, decayRate));
            g.fillRect(0, 0, SIZE, SIZE);
            g.setColor(color);
            BufferPacket pak = bigbuff.getMany((samplesPerFrame > bigbuff.count()) ? bigbuff.count() : samplesPerFrame);
            while(pak.hasMoreData()) {
                int x = pak.readL();
                int y = pak.readR();
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

    int ovc = 0;

    public void addPoint(int x, int y) {
        if(x >= 0 && x <= SIZE-1 && y >= 0 && y <= SIZE-1) {
            bigbuff.add(x, y);
        }
    }

    public void addPoints(BufferPacket pak) {
        bigbuff.addMany(pak);
    }

    public boolean isUpdating() {
        return updating;
    }

    public void start() {
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
                                bigbuff.skip(sc*samplesPerFrame);
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

    public void stop() {
        updating = false;
        try {
            updater.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        stopButton.setText("Start");
    }

    public int getBuffCount() {
        return bigbuff.count();
    }

    public int getBuffSize() {
        return bigbuff.size();
    }
}
