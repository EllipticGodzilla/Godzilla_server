package gui;

import network.Net_listener;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class Clients_panel {
    private static JPanel panel = null;

    public static JPanel init() {
        if (panel == null) {
            panel = new JPanel();
            panel.setLayout(new GridLayout(1, 1));

            JPanel list = ClientsList.init();
            GScrollPane scrollPane = new GScrollPane(list);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            panel.add(scrollPane);
        }

        return panel;
    }

    public static void offline(String[] online_users) { //rende tutte le caselle offline
        for (String usr : online_users) { //aggiunge tutte le caselle offline
            if (!offline_client(usr)) { //se questo client è appaiato non riesce a trovare nessuna casella con il suo nome
                ClientsList.add(usr, ClientsList.OFFLINE); //lo aggiunge offline
            }
        }

        //rimuove tutte le caselle pair
        ClientsList.clear_pair();
    }

    public static void init_list(String[] clients) {
        ClientsList.set_registered(clients);
    }

    public synchronized static void register_client(String user) {
        ClientsList.add(user, ClientsList.ONLINE); //viene aggiunto come online
    }

    public synchronized static void login_client(String user) {
        ClientsList.move(user, ClientsList.ONLINE, null); //muove la casella corrispondente a questo client fra i client online
    }

    public synchronized static boolean offline_client(String user) { //muove la casella user fra quelle offline
        return ClientsList.move(user, ClientsList.OFFLINE, null);
    }

    public synchronized static void pair_clients(String user1, String user2) {
        String new_user = user1 + " <--> " + user2;
        ClientsList.move(user1, ClientsList.PAIR, new_user);
        ClientsList.remove(user2);
    }

    public synchronized static void unpair_clients(String user1, String user2) {
        String u1 = user1 + " <--> " + user2,
               u2 = user2 + " <--> " + user1;

        if (!ClientsList.move(u1, ClientsList.ONLINE, user1)) { //se non trova nessuna coppia con u1, cerca con u2
            ClientsList.move(u2, ClientsList.ONLINE, user1);
        }

        ClientsList.add(user2, ClientsList.ONLINE);
    }
}

/*
 * utilizzando un estensione di JList viene più semplice ma aggiungere e rimuovere elementi dalla lista in modo dinamico può provocare problemi grafici
 * dove la lista viene mostrata vuota finché non le si dà un nuovo update, di conseguenza ho creato la mia versione di JList utilizzando varie JTextArea
 * e partendo da un JPanel.
 * Non so bene da che cosa sia dovuto il problema con JList ma sembra essere risolto utilizzando la mia versione
 */
abstract class ClientsList {
    public static final int PAIR = 0;
    public static final int ONLINE = 1;
    public static final int OFFLINE = 2;

    private static Map<String, ListCell> elements = new LinkedHashMap<>();
    private static ListCell selected_cell = null;

    private static JPanel list_panel = new JPanel(), //pannello della lista
                          pair_panel = new JPanel(), //pannello per le caselle che rappresentano coppie di client
                          online_panel = new JPanel(), //pannello per le caselle che rappresentano client non collegati ad altri client ma online
                          offline_panel = new JPanel(); //pannello per le caselle che rappresentano client offline
    private static JTextArea filler = new JTextArea(); //filler per rimepire lo spazio in basso

    public static JPanel init() {
        list_panel.setPreferredSize(new Dimension(200, 200)); //abbastanza casuale, solo per evitare che all'avvio si mostri con una dimensione minuscola poiche vuota

        //inizializza tutti i pannelli con layout, background, foreground
        list_panel.setLayout(new GridBagLayout());
        pair_panel.setLayout(new GridBagLayout());
        online_panel.setLayout(new GridBagLayout());
        offline_panel.setLayout(new GridBagLayout());

        Color foreground = new Color(44, 46, 47);
        Color background = new Color(118, 121, 123);
        list_panel.setForeground(foreground);
        list_panel.setBackground(background);
        pair_panel.setForeground(foreground);
        pair_panel.setBackground(background);
        online_panel.setForeground(foreground);
        online_panel.setBackground(background);
        offline_panel.setForeground(foreground);
        offline_panel.setBackground(background);

        //inizializza il filler
        filler.setBackground(list_panel.getBackground());
        filler.setFocusable(false);
        filler.setEditable(false);

        //aggiunge tutti gli elementi al pannello
        GridBagConstraints c = new GridBagConstraints();

        c.weighty = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        list_panel.add(pair_panel, c);

        c.gridy = 1;
        list_panel.add(online_panel, c);

        c.gridy = 2;
        list_panel.add(offline_panel, c);

        c.weighty = 1;
        c.gridy = 3;
        list_panel.add(filler, c);

        return list_panel;
    }

    public synchronized static void set_registered(String[] clients) {
        //aggiunge tutti i client registrati alla lista
        for (String client : clients) {
            add(client, OFFLINE);
        }
    }

    public synchronized static void add(String name, int type) { //aggiunge una nuova casella nell'ultima posizione
        ListCell cell = new ListCell(name, 0, type); //inizializza la cella con un index randomico
        elements.put(name, cell);

        add(cell, type); //aggiunge la cella al corretto pannello in base a type

        list_panel.updateUI(); //aggiorna la gui
    }

    private static void add(ListCell cell, int type) {
        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;

        switch (type) {
            case PAIR:
                c.gridy = pair_panel.getComponentCount();
                pair_panel.add(cell, c);

                cell.update_pos(type, c.gridy); //aggiorna la posizione della cella con il corretto indice

                break;

            case ONLINE:
                c.gridy = online_panel.getComponentCount();
                online_panel.add(cell, c);

                cell.update_pos(type, c.gridy); //aggiorna la posizione della cella con il corretto indice

                break;

            case OFFLINE:
                c.gridy = offline_panel.getComponentCount();
                offline_panel.add(cell, c);

                cell.update_pos(type, c.gridy); //aggiorna la posizione della cella con il corretto indice

                break;
        }
    }

    public synchronized static boolean move(String cell_txt, int new_type, String new_txt) {
        ListCell cell = elements.get(cell_txt); //trova la casella associata al testo cell_txt
        if (cell == null) { //se non ha trovato nessuna casella associata al nome cell_txt
            return false;
        }

        move(cell, new_type); //muove la casella nella lista

        if (new_txt != null) { //se si vuole anche cambiare il nome della cella
            elements.remove(cell_txt); //rimuove l'associazione cell_txt -> cell
            elements.put(new_txt, cell); //aggiunge alla mappa new_txt -> cell
            cell.setText(new_txt); //aggiorna il testo della casella
        }

        elements.put(cell.getText(), cell); //registra nuovamente la casella dopo averla rimossa

        list_panel.updateUI();
        return true;
    }

    private static void move(ListCell cell, int new_type) {
        remove(cell, cell.pos.type); //rimuove la cella dalla lista
        cell.update_pos(new_type, 0); //aggiorna la posizione della casella con un index a caso, verrà definito correttamente una volta aggiunta alla lista
        add(cell, new_type); //aggiunge la casella alla lista
    }

    public synchronized static boolean remove(String name) {
        ListCell cell_rem = elements.get(name); //ricava la casella che corrisponde a questo nome
        if (cell_rem == null) { //se non ha trovato nessuna cella corrispondende a questo nome
            return false;
        }
        int type = cell_rem.pos.type;

        remove(cell_rem, type); //rimuove la cella
        list_panel.updateUI();

        return true;
    }

    private static void remove(ListCell cell, int type) {
        String name = cell.getText();
        elements.remove(name); //rimuove questa casella dalla mappa degli elementi

        //rimuove la casella dalla lista
        switch (type) {
            case PAIR:
                pair_panel.remove(cell.pos.index); //rimuove l'elemento cell_rem dalla lista

                fix_cell_position(pair_panel, cell.pos.index); //reimposta la posizione di tutte le caselle sotto a questa nello stesso JPanel
                break;

            case ONLINE:
                online_panel.remove(cell.pos.index); //rimuove l'elemento cell_rem dalla lista

                fix_cell_position(online_panel, cell.pos.index); //reimposta la posizione di tutte le caselle sotto a questa nello stesso JPanel
                break;

            case OFFLINE:
                offline_panel.remove(cell.pos.index); //rimuove l'elemento cell_rem dalla lista

                fix_cell_position(offline_panel, cell.pos.index); //reimposta la posizione di tutte le caselle sotto a questa nello stesso JPanel
                break;
        }

        if (cell.getText().equals(selected_cell)) { //se questa casella era selezionata
            selected_cell = null; //resetta la posizione della casella selezionata
        }
    }

    protected static void clear_pair() { //rimuove tutte le caselle che rappresentano coppie
        pair_panel.removeAll();
    }

    private static void fix_cell_position(JPanel panel, int from) { //una volta rimossa una casella da in mezzo ad un pannello alza tutte le caselle che si trovano sotto questa di una posizione
        GridBagLayout layout = (GridBagLayout) panel.getLayout(); //ricava il layout del pannello
        for (; from < panel.getComponentCount(); from++) {
            Component comp = panel.getComponent(from); //prende il prossimo componente

            GridBagConstraints constraints = layout.getConstraints(comp); //ricava i Constraints di questo componente
            constraints.gridy -= 1; //sposta in su di una posizione questo oggetto
            layout.setConstraints(comp, constraints); //imposta i nuovi constraints all'oggetto

            ((ListCell) comp).pos.index -= 1; //abbassa di uno l'indice nella posizione della casella
        }
    }

    public static String getSelectedValue() {
        if (selected_cell == null) { //se nessuna casella è selezionata
            return "";
        }
        else {
            return selected_cell.getText();
        }
    }

    private static void set_selected(CellPosition pos) {
        update_selCell(pos.type, pos.index); //aggiorna la posizione della casella selezionata
    }

    private static void select_next_cell() {
        if (selected_cell.pos.index == get_num_of(selected_cell.pos.type) - 1) { //se la casella selezionata è l'ultima del suo tipo
            if (get_num_of(selected_cell.pos.type + 1) > 0) { //se il prossimo tipo di caselle ne ha almeno una
                update_selCell(selected_cell.pos.type + 1, 0);
            }
            else if (get_num_of(selected_cell.pos.type + 2) > 0) { //se il tipo subito dopo non ha caselle prova con quello sucessivo
                update_selCell(selected_cell.pos.type + 2, 0);
            }
            else {} //se nessuno dei due successivi tipi di celle ne ha visibili vuol dire che la casella selezionata è l'ultima della lista
        }
        else { //esiste una casella dello stesso tipo di qeulla selezionaa dopo di essa
            update_selCell(selected_cell.pos.type, selected_cell.pos.index + 1);
        }
    }

    private static void select_prev_cell() { //seleziona la casella sopra a quella selezionata
        int index;
        if (selected_cell.pos.index == 0) { //se è la prima casella del suo tipo
            if ((index = get_num_of(selected_cell.pos.type - 1)) > 0) { //se ci sono delle caselle del tipo precedente
                update_selCell(selected_cell.pos.type - 1, index - 1);
            }
            else if ((index = get_num_of(selected_cell.pos.type - 2)) > 0) { //se ci sono di un tipo 2 sopra la casella selezionata
                update_selCell(selected_cell.pos.type - 2, index - 1);
            }
        }
        else {
            update_selCell(selected_cell.pos.type, selected_cell.pos.index - 1);
        }
    }

    private static void update_selCell(int new_type, int new_index) {
        if (selected_cell != null) {
            selected_cell.unselect(); //deseleziona la casella selezionata in questo momento-
        }
        selected_cell = get_cell_at(new CellPosition(new_type, new_index)); //aggiorna la posizione della casella selezionata
        selected_cell.set_selected(); //seleziona la nuova casella
    }

    private static void unselect() {
        if (selected_cell != null) {
            selected_cell.unselect();
            selected_cell = null;
        }
    }

    private static ListCell get_cell_at(CellPosition pos) {
        switch (pos.type) {
            case PAIR:
                return (ListCell) pair_panel.getComponent(pos.index);

            case ONLINE:
                return (ListCell) online_panel.getComponent(pos.index);

            case OFFLINE:
                return (ListCell) offline_panel.getComponent(pos.index);
        }

        return null;
    }

    private static int get_num_of(int type) {
        switch (type) {
            case PAIR:
                return pair_panel.getComponentCount();

            case ONLINE:
                return online_panel.getComponentCount();

            case OFFLINE:
                return offline_panel.getComponentCount();
        }

        return -1;
    }

    public static void reset_list() {
        elements = new LinkedHashMap<>();
        pair_panel.removeAll();
        online_panel.removeAll();
        offline_panel.removeAll();
        list_panel.repaint();

        selected_cell = null;
    }

    private static class ListCell extends JTextArea {
        private static final Color  ONLINE_FOREGROUND = new Color(44, 46, 47),
                                    OFFLINE_FOREGROUND = new Color(64, 66, 67),
                                    STD_BACKGROUND = new Color(118, 121, 123),
                                    SEL_BACKGROUND = new Color(136, 141, 145),
                                    SEL_BORDER = new Color(72, 74, 75);

        private CellPosition pos;

        public ListCell(String text, int index, int type) {
            super(text);
            this.pos = new CellPosition(type, index);

            //imposta tutti i colori
            this.setForeground((type == OFFLINE)? OFFLINE_FOREGROUND : ONLINE_FOREGROUND);
            this.setBackground(STD_BACKGROUND);
            this.setFont(new Font("custom_list", Font.BOLD, 11));
            this.setBorder(null);

            this.setEditable(false);
            this.setCaretColor(STD_BACKGROUND);
            this.setCursor(null);

            //aggiunge i listener
            this.addKeyListener(key_l);
            this.addMouseListener(mouse_l);

            //aggiunge il popup
            this.setComponentPopupMenu(new ListCell_popup(type, text));

            //imposta le dimensioni di default
            this.setPreferredSize(new Dimension(200, 14));
        }

        public void update_pos(int new_type, int new_index) {
            if (new_type != pos.type) { //se è cambiata di tipologia
                set_type(new_type); //aggiorna il popup
                pos.type = new_type; //aggiorna il tipo di client che rappresenta
            }

            pos.index = new_index; //aggiorna il suo index
        }

        private void set_type(int type) { //cambia il tipo di popup
            if (type == ClientsList.OFFLINE) { //aggiorna il colore della scritta
                this.setForeground(OFFLINE_FOREGROUND);
            }
            else {
                this.setForeground(ONLINE_FOREGROUND);
            }
            ((ListCell_popup)this.getComponentPopupMenu()).change_to(type);
        }

        private KeyListener key_l = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyPressed(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case 40: //freccia in basso
                        ClientsList.select_next_cell();
                        break;

                    case 38: //freccia in alto
                        ClientsList.select_prev_cell();
                        break;

                    case 27: //esc
                        ClientsList.unselect();
                        break;
                }
            }
        };

        private MouseListener mouse_l = new MouseListener() {
            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}
            @Override
            public void mouseClicked(MouseEvent e) {}

            @Override
            public void mousePressed(MouseEvent e) {
                ClientsList.set_selected(pos);
            }
        };

        public void set_selected() {
            //imposta questa JTextArea come selezionata
            setBackground(SEL_BACKGROUND);
            setBorder(BorderFactory.createLineBorder(SEL_BORDER));
            setCaretColor(SEL_BACKGROUND);
            setSelectionColor(SEL_BACKGROUND);
        }

        public void unselect() {
            setBackground(STD_BACKGROUND);
            setBorder(null);
            setCaretColor(STD_BACKGROUND);
            setSelectionColor(STD_BACKGROUND);
        }

        @Override
        public void setText(String t) {
            super.setText(t);

            if (this.getComponentPopupMenu() != null) { //se è definito un popup menu
                ((ListCell_popup) this.getComponentPopupMenu()).cell_txt = t; //aggiorna il testo nel popup
            }
        }
    }

    private static class ListCell_popup extends JPopupMenu { //popup menu delle celle di questa lista
        private String cell_txt;

        private JMenuItem pair_unpair,
                          pair_disconnect,
                          online_disconnect,
                          offline_remove,
                          offline_changePsw;

        public ListCell_popup(int type, String txt) {
            super();
            this.cell_txt = txt;

            pair_unpair = new JMenuItem("unpair");
            pair_disconnect = new JMenuItem("disconnect");
            online_disconnect = new JMenuItem("disconnect");
            offline_remove = new JMenuItem("remove");
            offline_changePsw = new JMenuItem("change password");

            UIManager.put("MenuItem.selectionBackground", new Color(108, 111, 113));
            UIManager.put("MenuItem.selectionForeground", new Color(158, 161, 163));

            this.setBorder(BorderFactory.createLineBorder(new Color(28, 31, 33)));
            pair_unpair.setBorder(BorderFactory.createLineBorder(new Color(78, 81, 83)));
            pair_disconnect.setBorder(BorderFactory.createLineBorder(new Color(78, 81, 83)));
            online_disconnect.setBorder(BorderFactory.createLineBorder(new Color(78, 81, 83)));
            offline_remove.setBorder(BorderFactory.createLineBorder(new Color(78, 81, 83)));
            offline_changePsw.setBorder(BorderFactory.createLineBorder(new Color(78, 81, 83)));

            pair_unpair.setBackground(new Color(88, 91, 93));
            pair_disconnect.setBackground(new Color(88, 91, 93));
            online_disconnect.setBackground(new Color(88, 91, 93));
            offline_remove.setBackground(new Color(88, 91, 93));
            offline_changePsw.setBackground(new Color(88, 91, 93));

            pair_unpair.setForeground(new Color(158, 161, 163));
            pair_disconnect.setForeground(new Color(158, 161, 163));
            online_disconnect.setForeground(new Color(158, 161, 163));
            offline_remove.setForeground(new Color(158, 161, 163));
            offline_changePsw.setForeground(new Color(158, 161, 163));

            pair_unpair.addActionListener(pair_unpair_listener);
            pair_disconnect.addActionListener(pair_disconnect_listener);
            online_disconnect.addActionListener(online_disconnect_listener);
            offline_remove.addActionListener(offline_remove_listener);
            offline_changePsw.addActionListener(offline_changePsw_listener);

            this.add(pair_unpair);
            this.add(pair_disconnect);
            this.add(online_disconnect);
            this.add(offline_remove);
            this.add(offline_changePsw);

            change_to(type);
        }

        public void change_to(int type) {
            switch (type) {
                case PAIR:
                    to_pair();
                    break;

                case ONLINE:
                    to_online();
                    break;

                case OFFLINE:
                    to_offline();
                    break;
            }
        }

        private void to_pair() { //diventa un popup per celle che rappresentano coppie di client
            reset();

            pair_unpair.setVisible(true);
            pair_disconnect.setVisible(true);
        }

        private ActionListener pair_unpair_listener = e -> {
            try {
                unpair_clients();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };

        private ActionListener pair_disconnect_listener = e -> {
            try {
                String[] names = unpair_clients(); //divide i due client

                Net_listener.disconnect(names[0], false); //scollega i due client
                Net_listener.disconnect(names[1], true);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };

        private String[] unpair_clients() throws InvalidAlgorithmParameterException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException, InvocationTargetException, InstantiationException, IllegalAccessException { //divide i due client
            Pattern sep = Pattern.compile(" <--> ");
            String[] names = sep.split(cell_txt); //trova i nomi dei due client appaiati

            Net_listener.unpair(names[0], names[1], true);

            return names;
        }

        private void to_online() { //diventa un popup per celle che rappresentano client online
            reset();

            online_disconnect.setVisible(true);
        }

        private ActionListener online_disconnect_listener = e -> {
            try {
                Net_listener.disconnect(cell_txt, true);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };

        private void to_offline() { //diventa un popup per celle che rappresentano client offline
            reset();

            offline_remove.setVisible(true);
            offline_changePsw.setVisible(true);
        }

        private ActionListener offline_remove_listener = e -> {
            Net_listener.forget(cell_txt); //dimentica le credenziali dell'utente
            ClientsList.remove(cell_txt); //rimuove la casella dalla lista
        };

        private TempPanel_action change_psw = new TempPanel_action() {
            @Override
            public void success() {
                if (!check_password(input.elementAt(0), input.elementAt(1))) { //se le due password inserite non sono uguali
                    TempPanel.show(new TempPanel_info(
                            TempPanel_info.SINGLE_MSG,
                            false,
                            "le password inserite non sono uguali"
                    ), null);
                }
                else {
                    String psw = input.elementAt(0);
                    byte[] hash = calculate_hash(psw); //calcola l'hash della nuova password

                    Net_listener.change_psw(cell_txt, hash); //cambia la password del client
                    TempPanel.show(new TempPanel_info(
                            TempPanel_info.SINGLE_MSG,
                            false,
                            "password di " + cell_txt + " cambiata con successo"
                    ), null);
                }
            }

            private boolean check_password(String psw1, String psw2) {
                return psw1.equals(psw2);
            }

            private byte[] calculate_hash(String psw) {
                byte[] usr_inverse = cell_txt.getBytes();

                for (int i = 0; i < cell_txt.length(); i++) {
                    usr_inverse[i] = (byte) (usr_inverse[i] ^ 0xff);
                }

                int psw_len = psw.length();
                byte[] hash = Arrays.copyOf(psw.getBytes(), psw_len + usr_inverse.length); //aumenta la lunghezza di psw[]
                System.arraycopy(usr_inverse, 0, hash, psw_len, usr_inverse.length); //copia usr_inverse[] in psw[]

                return Net_listener.sha3(Net_listener.sha3(hash)); //ritorna il doppio hash di psw[] che viene salvato nella tabella in Net_listener
            }

            @Override
            public void fail() {} //se non vuole più cambiare la password
        };

        private ActionListener offline_changePsw_listener = e -> {
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.INPUT_REQ,
                    true,
                    "inserisci la nuova password:",
                    "inserisci nuovamente la password:"
            ).set_psw_indices(0, 1), change_psw);
        };

        private void reset() { //resetta la lista di menu item nel popup
            pair_disconnect.setVisible(false);
            pair_unpair.setVisible(false);
            online_disconnect.setVisible(false);
            offline_changePsw.setVisible(false);
            offline_remove.setVisible(false);
        }
    }

    private static class CellPosition {
        private int type;
        private int index;

        public CellPosition(int type, int index) {
            this.type = type;
            this.index = index;
        }
    }
}