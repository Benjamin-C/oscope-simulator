package dev.orangeben.scopeviz;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

public abstract class MenuParameter extends JMenuItem {

    public interface StartValueGetter {
        public String getStartValue();
    }

    private String menuline;
    private final MenuParameter me = this;
    
    public MenuParameter(String menuline, String title, String message) {
        super(menuline);
        this.menuline = menuline;

        addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
                InputWindow.askUser(SwingUtilities.getWindowAncestor(me), title, message, getStarter(),
                    (String val) -> {return validateInput(val);},
                    (String val) -> {onOK(val); updateValue();}
                );
			}
        });

        updateValue();
    }

    public void updateValue() {
        setText(menuline + ": " + getStarter());
    }

    /**
     * Get the value that the dialog should start with
     * @return The starting value
     */
    public abstract String getStarter();

    /**
     * Checks if the provided input is valid
     * @param val The value of the input
     * @return If the input is valid
     */
    public abstract boolean validateInput(String val);

    /**
     * Called when the OK button is pressed
     * @param val The value of the input
     */
    public abstract void onOK(String val);

}
