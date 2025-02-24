package dev.orangeben.scopeviz;

import java.awt.Color;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.UIManager;

public class ScopeVisualizer {

    private static ScopeScreen screen;

    static BufferSource as;
    static int buffnano = 000000;
    static int buffmili = 10;
    
    public static void feedTestCircle() {
        double time = 0;
        System.out.println("Test data!");
        while(true) {
            int x = (int)  (Short.MAX_VALUE * Math.cos(2*Math.PI*time*1));
            int y = (int) -(Short.MAX_VALUE * Math.sin(2*Math.PI*time*1));
            // System.out.println(String.format("Point %.3f (%d, %d) %f %f", time, x, y, 2*Math.PI*time*1000, Math.cos(2*Math.PI*time*1000)));
            as.write(x, y);
            try {
                Thread.sleep(buffmili, buffnano);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            time += buffmili/1e3 + buffnano/1e9;
        }
    }
    
    public static void main(String[] args) throws Exception {

        JFrame jf = new JFrame("Audio Visualizer Scope");
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        as = new BufferSource((int) (1e9/(buffnano+(1e6*buffmili))), Short.MAX_VALUE);
        CaptureSource cs = new CaptureSource();
        // CaptureSource cs = null;
        // CircleSource circ = new CircleSource(60, 64);
        screen = new ScopeScreen(cs);
        screen.start();
        
        JPanel jp = new JPanel();
        jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
        jp.setBackground(screen.getBackground());
        
        JPanel infopannel = new JPanel();
        infopannel.setLayout(new BoxLayout(infopannel, BoxLayout.X_AXIS));
        infopannel.setBackground(screen.getBackground());
        infopannel.setForeground(Color.WHITE);

        JMenuBar menubar = new JMenuBar();
        menubar.add(screen.getControlMenu());

        if(cs != null) {
            JMenu sourcemenu = new JMenu("Source");
            MenuSelector mixermenu = new MenuSelector("Mixer", cs.getMixers(), cs.getInterfaceNum()) {
                @Override
                public void onUpdate(int num, Object arg, MenuSelector menu) {
                    if(menu.getSelectedIndex() != num) {
                        cs.setInterfaceNum(num);
                        ((MenuSelector) arg).setItems(cs.getLines(num));
                        if(menu.getText().charAt(0) != '*') {
                            menu.setText("*" + menu.getText());
                        }
                    }
                }
                
            };
            MenuSelector linemenu = new MenuSelector("Line", cs.getLines(), cs.getLineNum()) {
                @Override
                public void onUpdate(int num, Object arg, MenuSelector menu) {
                    System.out.println("Selecting " + num);
                    cs.setLineNum(num);
                    cs.stop();
                    cs.start();
                    if(arg instanceof JMenu) {
                        if(((JMenu) arg).getText().charAt(0) == '*') {
                            ((JMenu) arg).setText(((JMenu) arg).getText().substring(1));
                        }
                    } else {
                        System.out.println("Arg was not a menu");
                        System.out.println(arg);
                    }
                }
                
            };
            mixermenu.setArg(linemenu);
            linemenu.setArg(mixermenu);
            
            sourcemenu.add(mixermenu);
            sourcemenu.add(linemenu);
            menubar.add(sourcemenu);
        }
    
        jf.setJMenuBar(menubar);
        
        jp.add(infopannel);
        jp.add(screen);
        
        jf.add(jp);
        
        jf.pack();
        jf.setVisible(true);
        
        feedTestCircle();
    }
}
