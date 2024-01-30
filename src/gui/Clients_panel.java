package gui;

import network.Net_listener;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.regex.Pattern;

public abstract class Clients_panel {
    private static JPanel panel = null;

    private static GList list = new GList();

    public static JPanel init() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (panel == null) {
            panel = new JPanel();
            panel.setLayout(new GridLayout(1, 1));

            GScrollPane scrollPane = new GScrollPane(list);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            list.set_popup(PopupMenu.class);

            panel.add(scrollPane);
        }

        return panel;
    }

    public static void add_client(String user) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        list.add(user, false);
    }

    public static void remove_client(String user) {
        list.remove(user);
    }

    public static void pair_clients(String user1, String user2) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        String new_user = user1 + " <--> " + user2;

        list.remove(user1);
        list.remove(user2);

        list.add(new_user, Net_listener.paired_num(), true);
    }

    public static void unpair_clients(String user1, String user2) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        String u1 = user1 + " <--> " + user2,
               u2 = user2 + " <--> " + user1;

        if (!list.remove(u1)) { //se non trova nessuna coppia con u1, cerca con u2
            list.remove(u2);
        }

        list.add(user1, false);
        list.add(user2, false);
    }

    public static void clean_list() { //rimuove tutti gli elementi nella lista
        list.reset_list();
    }
}

class PopupMenu extends JPopupMenu {
    private String name = "";
    private boolean paired;

    private JMenuItem unpair;

    public PopupMenu(String name, Boolean paired) {
        this.name = name;
        this.paired = paired;

        JMenuItem close = new JMenuItem("disconnect");
        unpair = new JMenuItem("unpair");

        UIManager.put("MenuItem.selectionBackground", new Color(108, 111, 113));
        UIManager.put("MenuItem.selectionForeground", new Color(158, 161, 163));

        this.setBorder(BorderFactory.createLineBorder(new Color(28, 31, 33)));
        close.setBorder(BorderFactory.createLineBorder(new Color(78, 81, 83)));
        unpair.setBorder(BorderFactory.createLineBorder(new Color(78, 81, 83)));

        close.setBackground(new Color(88, 91, 93));
        unpair.setBackground(new Color(88, 91, 93));
        close.setForeground(new Color(158, 161, 163));
        unpair.setForeground(new Color(158, 161, 163));

        close.addActionListener(close_l);
        unpair.addActionListener(unpair_l);

        this.add(close);
        if (paired) {
            this.add(unpair);
        }
    }

    public void reset(String name, boolean pair) {
        if (pair && this.getComponentCount() == 1) { //se ora rappresenta una coppia e "unpair" non è visibile
            this.add(unpair);
        }
        else if (this.getComponentCount() == 2) { //se ora non rappresenta una coppia e "unpair" è visibile
            this.remove(unpair);
        }

        this.paired = pair;
        this.name = name;
    }

    private ActionListener close_l = e -> {
        try {
            if (paired) { //se la cella a cui appartiene e di una coppia di client
                String[] names = unpair_clients(); //divide i due client

                Net_listener.disconnect(names[0], false); //scollega i due client
                Net_listener.disconnect(names[1], true);
            }
            else { //se la cella a cui appartiene è di un client solo
                Net_listener.disconnect(name, true); //scollega quel client
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    };

    private ActionListener unpair_l = e -> {
        try {
            unpair_clients();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    };

    private String[] unpair_clients() throws InvocationTargetException, InstantiationException, IllegalAccessException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException { //divide i due client
        Pattern sep = Pattern.compile(" <--> ");
        String[] names = sep.split(name); //trova i nomi dei due client appaiati

        Net_listener.unpair(names[0], names[1], true); //li divide

        return names;
    }
}