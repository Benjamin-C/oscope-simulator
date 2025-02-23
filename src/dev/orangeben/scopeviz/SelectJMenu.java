package dev.orangeben.scopeviz;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

public abstract class SelectJMenu extends JMenu {

    private String[] names;
    private int selection;
    private JMenuItem[] items;
    private SelectJMenu me;
    private Object arg;

    public SelectJMenu(String[] names) {
        this(null, names, 0);
    }
    public SelectJMenu(String label, String[] names) {
        this(label, names, 0);
    }
    public SelectJMenu(String[] names, int selection) {
        this(null, names, selection);
    }
    public SelectJMenu(String label, String[] names, int selection) {
        super();
        if(label != null) {
            setText(label);
        }
        me = this;
        setItems(names, selection);
    }

    public void setArg(Object obj) {
        this.arg = obj;
    }

    public int getSelectedIndex() {
        return selection;
    }

    public void setSelectedIndex(int num) {
        if(0 <= num && num < names.length) {
            selection = num;
        } else {
            throw new ArrayIndexOutOfBoundsException("Invalid selection number");
        }
    }

    public void setItems(String[] names) {
        setItems(names, 0);
    }

    public void setItems(String[] names, int selnum) {
        this.names = names;
        this.items = new JMenuItem[names.length];
        removeAll();
        setSelectedIndex(selnum);
        for(int i = 0; i < this.names.length; i++) {
            final int mynum = i;
            items[i] = new JMenuItem();
            items[i].addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    onUpdate(mynum, arg, me);
                    selection = mynum;
                    updateMenuItems();
                }
            });
            add(items[i]);
        }
        updateMenuItems();
    }

    private void updateMenuItems() {
        for(int i = 0; i < this.names.length; i++) {
            items[i].setText(((selection==i) ? "* " : "  ") + names[i]);
        }
    }

    /**
     * Selection var is not updated until after this is called. Use the num variable for what is about to be selected.
     * @param num The index of the soon-to-be selected item
     * @param arg A user-supplied argument
     * @param menu The menu that called this function
     */
    public abstract void onUpdate(int num, Object arg, SelectJMenu menu);



}
