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
    
    // public static void feedTestCircle() {
    //     double time = 0;
    //     System.out.println("Test data!");
    //     while(true) {
    //         int x = (int) (screen.SIZE/2  * (Math.cos(2*Math.PI*time*100) + 1));
    //         int y = (int) (screen.SIZE/2 * (Math.sin(2*Math.PI*time*100) + 1));
    //         // System.out.println(String.format("Point %.3f (%d, %d) %f %f", time, x, y, 2*Math.PI*time*1000, Math.cos(2*Math.PI*time*1000)));
    //         screen.addPoint(x, y);
    //         try {
    //             Thread.sleep(0, 50000);
    //         } catch (InterruptedException e) {
    //             // TODO Auto-generated catch block
    //             e.printStackTrace();
    //         }
    //         time += 0.0001;
    //     }
    // }
    
    public static void main(String[] args) throws Exception {

        JFrame jf = new JFrame("Audio Visualizer Scope");
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // BufferSource as = new BufferSource(44100);
        CaptureSource cs = new CaptureSource();
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
        JMenu sourcemenu = new JMenu("Source");
        SelectJMenu mixermenu = new SelectJMenu("Mixer", cs.getMixers(), cs.getInterfaceNum()) {
			@Override
			public void onUpdate(int num, Object arg, SelectJMenu menu) {
                if(menu.getSelectedIndex() != num) {
                    cs.setInterfaceNum(num);
                    ((SelectJMenu) arg).setItems(cs.getLines(num));
                    if(menu.getText().charAt(0) != '*') {
                        menu.setText("*" + menu.getText());
                    }
                }
			}
            
        };
        SelectJMenu linemenu = new SelectJMenu("Line", cs.getLines(), cs.getLineNum()) {
			@Override
			public void onUpdate(int num, Object arg, SelectJMenu menu) {
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
        
        menubar.add(screen.getControlMenu());
        menubar.add(sourcemenu);

        jf.setJMenuBar(menubar);
        
        jp.add(infopannel);
        jp.add(screen);
        
        jf.add(jp);
        
        jf.pack();
        jf.setVisible(true);
        
        // feedTestData(screen);
    }
}
