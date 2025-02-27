package dev.orangeben.scopeviz;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;

public class ScopeScreen extends JPanel {

    /** The size of the display */
    private int size = 1024;
    /** The menu to change the size */
    private MenuParameter sizeMenu;
    /** The color of the display */
    // private Color color = new Color(255, 128, 0);
    private Color color = new Color(128, 64, 0);
    /** The menu to change the color */
    private MenuParameter colorMenu;
    /** Frame updater thread */
    private Thread updater;
    /** If the display is running */
    private volatile boolean updating;
    /** The target nubmer of frames per seconds to display */
    private final double targetFPS = 60;
    /** The actual observed FPS */
    private volatile double fps = 0;
    /** The actual scope screen */
    private BufferedImage screen;
    /** The label that holds the scope screen */
    private JLabel screenLabel;
    /** The jpanel the screen label lives in */
    private JPanel screenPanel;
    /** Label to hold the current FPS */
    private JMenuItem fpslabel;
    /** Checkbox to control line drawing */
    private JCheckBox linecheck;
    /** If lines are being drawn */
    private boolean drawLines = true;
    /** Checkbox to control point drawing */
    private JCheckBox pointcheck;
    /** If points are being rdrawn */
    private boolean drawPoints = true;
    /** Control menu bar */
    private JCheckBox fpscheck;
    /** Checkbox to control FPS drawing */
    private boolean drawFPS = true;
    /** If FPS is being drawn */
    private JMenu controlsMenu;
    /** Update stop button */
    private JMenuItem stopButton;
    private JMenuItem clearButton;
    /** Approximate screen decay time in seconds */
    private double decay = 0.1;
    /** Menu item to change decay time */
    private MenuParameter decayMenu;
    /** x or y of previously drawn point */
    private int lx, ly = 0;
    /** The source of the data to draw */
    private AudioSource source;
    /** The alpha to dim the screen over time */
    private double decayR, decayG, decayB;
    private double decayRamt, decayGamt, decayBamt;
    /** The diagonal screen distance in px */
    private double maxdist;
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
        
        controlsMenu = new JMenu("Scope");
        controlsMenu.setBackground(getBackground());
        
        stopButton = new JMenuItem("Stop");
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(updating) {
                    stop();
                } else {
                    start();
                }
            }
        });
        controlsMenu.add(stopButton);

        clearButton = new JMenuItem("Clear");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                synchronized(g) {
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, size, size);
                }
            }
        });
        controlsMenu.add(clearButton);
        
        fpslabel = new JMenuItem();
        controlsMenu.add(fpslabel);

        JMenuItem screenshotMenu = new JMenuItem("Screenshot");
        screenshotMenu.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
                try {
					save("screenshot.png");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
            
        });
        controlsMenu.add(screenshotMenu);

        sizeMenu = new MenuParameter("Size", "Scope Size", "Scope size in px") {
			@Override public String getStarter() {
                return String.format("%d", size);
			}
			@Override public boolean validateInput(String val) {
                try {
                    double testdecay = Integer.parseInt(val);
                    if(testdecay > 0) {
                        return true;
                    }    
                } catch (Exception e) {
                    // Don't need to do anything
                }
                return false;
			}
			@Override public void onOK(String val) {
                setScreenSize(Integer.parseInt(val));
                System.out.println("[Scope] Changed screen size to " + val);
			}
        };
        controlsMenu.add(sizeMenu);

        decayMenu = new MenuParameter("Decay", "Decay Time", "New approx decay time (s)") {
			@Override public String getStarter() {
                return String.format("%.2f", decay);
			}
			@Override public boolean validateInput(String val) {
                try {
                    double testdecay = Double.parseDouble(val);
                    if(testdecay > 0.01) {
                        return true;
                    }    
                } catch (Exception e) {
                    // Don't need to do anything
                }
                return false;
			}
			@Override public void onOK(String val) {
                setDecay(Double.parseDouble(val));
                System.out.println("[Scope] Set delay to " + val);
			}
        };
        controlsMenu.add(decayMenu);

        colorMenu = new MenuParameter("Color", "Set Color", "Hex code for color") {
			@Override public String getStarter() {
                return String.format("%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
			}
			@Override public boolean validateInput(String val) {
                try {
                    if(val.length() == 6) {
                        @SuppressWarnings("unused")
						Color testcol = new Color(Integer.parseInt(val,16));
                        return true;
                    }
                } catch (Exception e) {
                    // Don't need to do anything
                }
                return false;
			}
			@Override public void onOK(String val) {
                color = new Color(Integer.parseInt(val,16));
                calcDecay();
                System.out.println("[Scope] Changed color to " + val);
			}
        };
        controlsMenu.add(colorMenu);

        linecheck = new JCheckBox("Draw lines");
        linecheck.setSelected(drawLines);
        linecheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                drawLines = linecheck.isSelected();
                System.out.println("[Scope] Lines " + ((drawLines) ? "enabled" : "disabled"));
            }
        });
        controlsMenu.add(linecheck);

        pointcheck = new JCheckBox("Draw points");
        pointcheck.setSelected(drawPoints);
        pointcheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                drawPoints = pointcheck.isSelected();
                System.out.println("[Scope] Points " + ((drawPoints) ? "enabled" : "disabled"));
            }
        });
        controlsMenu.add(pointcheck);

        fpscheck = new JCheckBox("Draw FPS");
        fpscheck.setSelected(drawFPS);
        fpscheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                drawFPS = fpscheck.isSelected();
                System.out.println("[Scope] FPS " + ((drawFPS) ? "enabled" : "disabled"));
            }
        });
        controlsMenu.add(fpscheck);

        add(controlsMenu);

        setDecay(decay);
        
        screenLabel = new JLabel();

        createScreen();

        screenPanel = new JPanel();
        screenPanel.setBackground(getBackground());
        screenPanel.add(screenLabel);
        add(screenPanel);
    }

    public void setDecay(Double newDecay) {
        decay = newDecay;
        calcDecay();
    }

    public void calcDecay() {
        decayR = (double) color.getRed()   / (decay*targetFPS);
        decayG = (double) color.getGreen() / (decay*targetFPS);
        decayB = (double) color.getBlue()  / (decay*targetFPS);
        System.out.println(String.format("Decay %.2f: %f %f %f", decay, decayR, decayG, decayB));
    }

    public JMenu getControlMenu() {
        return controlsMenu;
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
     * Creates the screen to use in the scope
     */
    private void createScreen() {
        // Make a fake object to use for initial setup
        Object sync = g;
        if(sync == null) {
            sync = new Object();
        }
        synchronized(sync) {
            maxdist = Math.sqrt(2)*size;
            screen = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            g = screen.createGraphics();
            screenLabel.setIcon(new ImageIcon(screen));
        }
    }

    /**
     * Sets the size of the screen
     * @param size The new size of the screen in pixels
     */
    public void setScreenSize(int size) {
        if(size < 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        synchronized(g) {
            this.size = size;
            createScreen();
        }
        SwingUtilities.getWindowAncestor(this).pack();
    }

    /**
     * Redraws the screen
     */
    private void redraw() {
        synchronized(g) {
            decayRamt += decayR;
            decayGamt += decayG;
            decayBamt += decayB;

            int thisR = (int) decayRamt;
            int thisG = (int) decayGamt;
            int thisB = (int) decayBamt;
            
            decayRamt -= thisR;
            decayGamt -= thisG;
            decayBamt -= thisB;

            if(screen.getType() != BufferedImage.TYPE_INT_RGB) {
                stop();
                throw new IllegalStateException("The image type must be BufferedImage.TYPE_INT_RGB = 1");
            }

            int[] buff = ((DataBufferInt)screen.getRaster().getDataBuffer()).getData();
            for(int i = 0; i < buff.length; i++) {
                Color c = new Color(buff[i]);
                int r = c.getRed() - thisR;
                int g = c.getGreen() - thisG;
                int b = c.getBlue() - thisB;
                r = (r<0) ? 0 : r;
                g = (g<0) ? 0 : g;
                b = (b<0) ? 0 : b;
                buff[i] = new Color(r, g, b).getRGB();
            }
            

            g.setColor(color);
            BufferPacket pak = source.read((int) (source.getSamplerate()/targetFPS));
            int pad = (int) Math.round((double) Short.MAX_VALUE / (size/2));
            while(pak.hasMoreData()) {
                int l = pak.readL();
                int r = pak.readR();
                int x = (int) ((double) l / pad) + size/2;
                int y = size - (int) (((double) r / pad) + size/2);
                if((x >= 0 && x <= size-1 && y >= 0 && y <= size-1)) {
                    if(drawLines) {
                        int xs = (lx-x);
                        int ys = (ly-y);
                        double bright = 1d - Math.pow(Math.sqrt((xs*xs)+(ys*ys))/maxdist, 0.08);
                        // double bright = 1d - Math.sqrt((xs*xs)+(ys*ys))/maxdist;
                        int rv = (int) (color.getRed() * bright)  ;
                        int gv = (int) (color.getGreen() * bright);
                        int bv = (int) (color.getBlue() * bright) ;
                        try {
                            int dx = (lx < x) ? x - lx : lx - x;
                            int dy = (ly < y) ? y - ly : ly - y;

                            int startA, endA, startB, endB, startX, startY;

                            int[] swap = {0, 0, 0, 0};

                            if(dx > dy) {
                                swap[0] = 1;
                                swap[3] = 1;
                                if(lx < x) {
                                    startA = lx;
                                    startX = lx;
                                    endA = x;
                                    startB = ly;
                                    startY = ly;
                                    endB = y;
                                } else {
                                    startA = x;
                                    startX = x;
                                    endA = lx;
                                    startB = y;
                                    startY = y;
                                    endB = ly;
                                }
                            } else {
                                swap[1] = 1;
                                swap[2] = 1;
                                if(ly < y) {
                                    startA = ly;
                                    startY = ly;
                                    endA = y;
                                    startB = lx;
                                    startX = lx;
                                    endB = x;
                                } else {
                                    startA = y;
                                    startY = y;
                                    endA = ly;
                                    startB = x;
                                    startX = x;
                                    endB = lx;
                                }
                            }

                            double gb = (double) (endB - startB) / (endA - startA);
                            double accum = 0;

                            for(int a = 0; a < endA-startA; a++) {
                                accum += gb;
                                int cb = (int) (accum + 0.5);
                                int drawx = (a*swap[0]) + ((cb*swap[1])) + startX;
                                int drawy = (a*swap[2]) + ((cb*swap[3])) + startY;
                                int index = (drawy*size)+drawx;
                                try {
                                    Color bc = new Color(buff[index]);
                                    int newr = rv+bc.getRed();
                                    int newg = gv+bc.getGreen();
                                    int newb = bv+bc.getBlue();
                                    Color towrite;
                                    if(newr > 255 || newg > 255 || newb > 255) {
                                        int maxv = Math.max(newr, Math.max(newg, newb));
                                        double sf = 255d / maxv;
                                        newr = (int) (sf*newr);
                                        newg = (int) (sf*newg);
                                        newb = (int) (sf*newb);
                                    }
                                    towrite = new Color(newr, newg, newb);
                                    buff[index] = towrite.getRGB();
                                } catch(ArrayIndexOutOfBoundsException e) {
                                    System.out.println("Array index out of bounds while drawing");
                                }
                            }
                        } catch (IllegalArgumentException e) {
                            System.out.println("Color was out of range, this is fine if you just changed something. rgb:" + rv + " " + gv + " " + bv);
                        }
                        if(drawPoints) {
                            screen.setRGB(lx, ly, color.getRGB());
                        }
                        lx = x;
                        ly = y;
                    } else if(drawPoints) {
                        // screen.setRGB(x, y, color.getRGB());
                        buff[y*size+x] = color.getRGB();
                    }
                }
                pak.nextRead();
            }
            if(drawFPS) {
                String str = String.format("FPS: % 4.1f", fps);
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, (int) g.getFontMetrics().getStringBounds(str, g).getWidth(), g.getFontMetrics().getHeight()+2);
                g.setColor(fpslabel.getForeground());
                g.drawString(str, 4, g.getFontMetrics().getHeight());
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
        System.out.println("[Scope] Starting ... ");
        source.start();
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
        System.out.println("[Scope] Started");
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
        System.out.println("[Scope] Stopped");
    }
}
