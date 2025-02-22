package dev.orangeben.scopeviz;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
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

    public final int WIDTH = 1024;
    public final int HEIGHT = 1024;
    private Color color;
    private Thread updater;
    private volatile boolean updating;
    private final double targetFPS = 60;
    private volatile double fps = 0;
    private BufferedImage screen;
    private JLabel screenLabel;
    private JLabel fpslabel;
    private JPanel controlsPanel;
    private JButton stopButton;
    private double decay = 0.1;

    private int xbuff[] = new int[16386];
    private int ybuff[] = new int[16386];
    private int buffpos = 0;
    private Object bufflock = new Object();

    private final int decayRate;

    private Graphics g;

    public ScopeScreen() {
        setBackground(new Color(8, 8, 8));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
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
        
        add(controlsPanel);

        color = Color.GREEN;
        decayRate = (int) (256 / (decay*targetFPS)) * 3;

        screen = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        g = screen.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 1, 1);
        g.setColor(new Color(0, 0, 0, decayRate));
        for(int i = 0; i < (int) (decay*targetFPS); i++) {
            g.fillRect(0, 0, 1, 1);
        }
        g.setColor(new Color(screen.getRGB(0, 0)));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        // g.dispose();
        screenLabel = new JLabel(new ImageIcon(screen));
        screenLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(screenLabel);
    }

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
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setColor(color);
            synchronized(bufflock) {
                for(int i = buffpos-1; i >= 0; i--) {
                    screen.setRGB(xbuff[i], ybuff[i], color.getRGB());
                }
                buffpos = 0;
                ovc = 0;
            }
        }
        screenLabel.repaint();
    }

    int ovc = 0;

    public void addPoint(int x, int y) {
        if(updating) {
            try {
                if(x >= 0 && x <= WIDTH-1 && y >= 0 && y <= HEIGHT-1) {
                    if(buffpos < xbuff.length) {
                        synchronized(bufflock) {
                            xbuff[buffpos] = x;
                            ybuff[buffpos] = y;
                            buffpos++;
                        }
                    } else {
                        System.out.println("Buffer full!" + buffpos + " " + ovc++);
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println(String.format("Point error at (%d, %d)", x, y));
                throw e;
            }
        }
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
}
