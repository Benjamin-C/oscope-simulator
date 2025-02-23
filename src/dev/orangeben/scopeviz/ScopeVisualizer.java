package dev.orangeben.scopeviz;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

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

        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // BufferSource as = new BufferSource(44100);
        CaptureSource as = new CaptureSource();
        screen = new ScopeScreen(as);
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
    }
}
