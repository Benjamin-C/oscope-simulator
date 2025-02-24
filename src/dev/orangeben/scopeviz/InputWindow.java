package dev.orangeben.scopeviz;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class InputWindow {

    public interface InputValidator {
        public boolean check(String input);
    }

    public interface OnAcceptRunnable {
        public void go(String value);
    }

    /**
     * Shows a popup and asks for user input
     * @param parent What the dialog should attach to
     * @param title The text to show in the top of the dialog
     * @param message The message to show above the input box
     * @param startval The value to put in the input box to start
     * @param validator A checker to only enable the OK button when it returns true
     * @param onok Called when the user clicks OK
     */
    public static void askUser(Window parent, String title, String message, String startval, InputValidator validator, OnAcceptRunnable onok) {
        JDialog jd = new JDialog(parent);

        JPanel vjp = new JPanel();
        vjp.setLayout(new BoxLayout(vjp, BoxLayout.Y_AXIS));

        
        JLabel messageLabel = new JLabel(message);
        // The text wouldn't align nicely unless it was in a jpanel, so I put it in one
        JPanel idkfix = new JPanel();
        idkfix.add(messageLabel);
        // This left alligns the text. I have no idea why this is needed
        idkfix.setAlignmentX(Component.RIGHT_ALIGNMENT);
        vjp.add(idkfix);

        JTextField input = new JTextField(startval);
        vjp.add(input);

        JPanel controPanel = new JPanel();
        controPanel.setLayout(new BoxLayout(controPanel, BoxLayout.X_AXIS));

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
                input.setText(startval);
			}
        });
        controPanel.add(resetButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
                jd.dispose();
			}
        });
        controPanel.add(cancelButton);

        JButton acceptbuButton = new JButton("OK");
        acceptbuButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
                onok.go(input.getText());
                jd.dispose();
			}
        });
        acceptbuButton.setEnabled(validator.check(startval));
        controPanel.add(acceptbuButton);

        input.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void changedUpdate(DocumentEvent e) {
				acceptbuButton.setEnabled(validator.check(input.getText()));
			}
			@Override public void insertUpdate(DocumentEvent e) {
				acceptbuButton.setEnabled(validator.check(input.getText()));
			}
			@Override public void removeUpdate(DocumentEvent e) {
				acceptbuButton.setEnabled(validator.check(input.getText()));
			}
        });
        input.select(0, Integer.MAX_VALUE);
        input.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				acceptbuButton.doClick();
			}
            
        });

        vjp.add(controPanel);

        jd.add(vjp);
        jd.setTitle(title);

        jd.pack();
        jd.setVisible(true);
    }

}
