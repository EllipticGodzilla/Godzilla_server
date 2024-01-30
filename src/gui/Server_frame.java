
//add(String, int) e remove(int) da rifare


package gui;

import network.Net_listener;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

public abstract class Server_frame {
    public static final String project_path = Server_frame.class.getProtectionDomain().getCodeSource().getLocation().getPath();

    private static JFrame frame = null;

    private static JPanel terminal_p;
    private static JPanel buttons_p;
    private static JPanel clients_p;

    public static JFrame init() throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        //inizializza la grafica
        frame = new JFrame("Godzilla server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 500));

        FullScreenLayeredPane layeredPane = new FullScreenLayeredPane(); //permette di aggiungere oggetti in full screen nel layered pane
        layeredPane.setBackground(new Color(58, 61, 63));
        frame.setLayeredPane(layeredPane);

        //crea il pannello principale e lo aggiunge al frame
        JPanel main_panel = new JPanel();
        main_panel.setBackground(new Color(58, 61, 63));
        main_panel.setLayout(new GridBagLayout());

        layeredPane.add_fullscreen(main_panel, JLayeredPane.FRAME_CONTENT_LAYER);

        //inizializza i pannelli che formano la schermata principale
        terminal_p = Terminal_panel.init();
        buttons_p = Buttons_panel.init();
        clients_p = Clients_panel.init();

        //aggiunge i pannelli al main_panel
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;

        c.weighty = 1;
        c.weightx = 0.3;
        c.gridy = 0;
        c.gridx = 0;
        c.gridheight = 2;
        c.insets = new Insets(10, 10, 10, 10);
        main_panel.add(clients_p, c);

        c.weightx = 0.7;
        c.gridx = 1;
        c.gridy = 1;
        c.gridheight = 1;
        c.insets = new Insets(10, 0,10,10);
        main_panel.add(terminal_p, c);

        c.weighty = 0;
        c.gridy = 0;
        c.insets = new Insets(10, 0, 0, 10);
        main_panel.add(buttons_p, c);

        //mostra il frame
        frame.setBounds(100, 100, 200, 200);
        frame.setVisible(true);

        return frame;
    }
}

class GScrollPane extends JScrollPane { //imposta la grafica
    public GScrollPane(Component c) {
        super(c);

        this.setBorder(BorderFactory.createLineBorder(new Color(72, 74, 75)));
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        super.setPreferredSize(new Dimension(width, height));
    }

    @Override
    public JScrollBar createVerticalScrollBar() {
        JScrollBar scrollBar = super.createVerticalScrollBar();

        scrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(78, 81, 83);
                this.thumbDarkShadowColor = new Color(58, 61, 63);
                this.thumbHighlightColor = new Color(108, 111, 113);
            }

            class null_button extends JButton {
                public null_button() {
                    super();
                    this.setPreferredSize(new Dimension(0, 0));
                }
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return new null_button();
            }
            @Override
            protected JButton createIncreaseButton(int orientation) {
                return new null_button();
            }
        });

        scrollBar.setBackground(new Color(128, 131, 133));
        scrollBar.setBorder(BorderFactory.createLineBorder(new Color(72, 74, 75)));

        return scrollBar;
    }

    @Override
    public JScrollBar createHorizontalScrollBar() {
        JScrollBar scrollBar = super.createHorizontalScrollBar();

        scrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(78, 81, 83);
                this.thumbDarkShadowColor = new Color(58, 61, 63);
                this.thumbHighlightColor = new Color(108, 111, 113);
            }

            class null_button extends JButton {
                public null_button() {
                    super();
                    this.setPreferredSize(new Dimension(0, 0));
                }
            }

            @Override
            protected JButton createDecreaseButton(int orientation) { return new null_button(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return new null_button(); }

        });

        scrollBar.setBackground(new Color(128, 131, 133));
        scrollBar.setBorder(BorderFactory.createLineBorder(new Color(72, 74, 75)));

        return scrollBar;
    }
}

/*
 * utilizzando un estensione di JList viene più semplice ma aggiungere e rimuovere elementi dalla lista in modo dinamico può provocare problemi grafici
 * dove la lista viene mostrata vuota finché non le si dà un nuovo update, di conseguenza ho creato la mia versione di JList utilizzando varie JTextArea
 * e partendo da un JPanel.
 * Non so bene da che cosa sia dovuto il problema con JList ma sembra essere risolto utilizzando la mia versione
 */
class GList extends JPanel {
    private Map<String, ListCell> elements = new LinkedHashMap<>();
    private int selected_index = -1;

    private JPanel list_panel = new JPanel(); //pannello che contiene tutte le JTextArea della lista
    private JTextArea filler = new JTextArea(); //filler per rimepire lo spazio in basso

    private Constructor PopupMenu = null;

    public GList() {
        super();
        this.setLayout(new GridBagLayout());

        this.setForeground(new Color(44, 46, 47));
        this.setBackground(new Color(98, 101, 103));
        this.setFont(new Font("Liberation Sans", Font.BOLD, 11));

        filler.setBackground(this.getBackground());
        filler.setFocusable(false);
        filler.setEditable(false);

        list_panel.setLayout(new GridBagLayout());
        list_panel.setBackground(this.getBackground());

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0;
        c.weightx = 1;
        this.add(list_panel, c);

        c.gridy = 1;
        c.weighty = 1;
        this.add(filler, c);
    }

    public void set_popup(Class PopupMenu) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        this.PopupMenu = PopupMenu.getDeclaredConstructor(String.class, Boolean.class);

        for (ListCell cell : elements.values()) { //aggiunge il popup menu per tutti gli elementi che sono già nella lista
            boolean is_a_pair = cell.my_index <= Net_listener.paired_num();
            cell.setComponentPopupMenu((JPopupMenu) this.PopupMenu.newInstance(cell.getText(), is_a_pair));
        }
    }

    public void add(String name, boolean pair) throws InvocationTargetException, InstantiationException, IllegalAccessException { //aggiunge una nuova casella nell'ultima posizione
        ListCell cell = new ListCell(name, this, list_panel.getComponentCount(), pair);
        elements.put(name, cell);
        if (this.PopupMenu != null) { //se non è una coppia ed aggiunge un popupMenu
            cell.setComponentPopupMenu((JPopupMenu) this.PopupMenu.newInstance(name, pair));
        }

        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        c.gridy = list_panel.getComponentCount();
        c.gridx = 0;

        list_panel.add(cell, c);

        this.updateUI(); //aggiorna la gui
    }

    public synchronized void add(String name, int index, boolean pair) throws InvocationTargetException, InstantiationException, IllegalAccessException { //aggiunge una nuova casella alla posizione specificata
        if (index < list_panel.getComponentCount()) {
            ListCell index_cell = (ListCell) list_panel.getComponent(0);
            String index_name = index_cell.getText();
            boolean index_pair = index_cell.pair;

            if (index_cell.pair ^ pair) { //se le due celle sono di due tipi diversi
                if (PopupMenu != null) { //se è definito un popup menu
                    ((gui.PopupMenu) index_cell.getComponentPopupMenu()).reset(name, pair);
                }

                index_cell.pair = pair;
            }

            index_cell.setText(name); //cambia il nome della casella alla posizione index
            elements.remove(index_name);
            elements.put(name, index_cell);

            add(index_name, index + 1, index_pair);
        }
        else { //crea una nuova casella
            add(name, pair);
        }
    }

    public boolean remove(String name) {
        ListCell cell_rem = elements.get(name);
        int cell_index = indexof(name);

        if (elements.remove(name) == null) { //rimuove l'elemento "name", ritorna false se non trova nessun elemento con la chiave "name"
            return false;
        }
        list_panel.remove(cell_rem); //rimuove la casella dalla lista

        GridBagLayout layout = (GridBagLayout) list_panel.getLayout();
        for (int i = cell_index; i < list_panel.getComponentCount(); i++) { //reimposta la posizione di tutte le caselle sotto a questa
            Component comp = list_panel.getComponent(i);

            GridBagConstraints constraints = layout.getConstraints(comp); //sposta la casella di una posizione sopra
            constraints.gridy -= 1;
            layout.setConstraints(comp, constraints);

            ((ListCell) comp).my_index = constraints.gridy; //reimposta l'index della casella
        }

        if (cell_rem.my_index == selected_index) { //se questa casella era selezionata
            selected_index = -1;
        }

        this.updateUI(); //aggiorna la gui

        return true;
    }

    public void rename_element(String old_name, String new_name) {
        for (ListCell cell : elements.values()) {
            if (cell.getText().equals(old_name)) {
                cell.setText(new_name);
                break;
            }
        }
    }

    public String getSelectedValue() {
        if (selected_index == -1) { //se non è selezionata nessuna casella
            return "";
        }
        else {
            return ((ListCell) list_panel.getComponent(selected_index)).getText();
        }
    }

    public void reset_list() {
        elements = new LinkedHashMap<>();
        list_panel.removeAll();
        this.repaint();

        selected_index = -1;
    }

    private int indexof(String name) {
        for (Component cell_comp : list_panel.getComponents()) {
            ListCell cell = (ListCell) cell_comp;

            if (cell.getText().equals(name)) { //se i nomi coincidono
                return cell.my_index;
            }
        }

        return -1; //se non esiste una cella con questo nome
    }

    class ListCell extends JTextArea {
        private static final Color STD_BACKGROUND = new Color(98, 101, 103);
        private static final Color SEL_BACKGROUND = new Color(116, 121, 125);
        private static final Color SEL_BORDER = new Color(72, 74, 75);

        private final GList PARENT_LIST;

        private int my_index;
        private boolean pair;

        public ListCell(String text, GList list, int index, boolean pair) {
            super(text);
            this.PARENT_LIST = list;
            this.my_index = index;
            this.pair = pair;

            //imposta tutti i colori
            this.setForeground(new Color(44, 46, 47));
            this.setBackground(STD_BACKGROUND);
            this.setFont(new Font("custom_list", Font.BOLD, 11));
            this.setBorder(null);

            this.setEditable(false);
            this.setCaretColor(STD_BACKGROUND);
            this.setCursor(null);

            this.addKeyListener(key_l);
            this.addMouseListener(mouse_l);
        }

        public boolean isAPair() {
            return pair;
        }

        public void setIndex(int index) {
            my_index = index;
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
                        try {
                            ListCell next_cell = (ListCell) PARENT_LIST.list_panel.getComponent(my_index + 1);

                            next_cell.set_selected();
                            next_cell.requestFocus();
                        } catch (Exception ex) {} //se non esiste un elemento ad index my_index + 1
                        break;

                    case 38: //freccia in alto
                        try {
                            ListCell prev_cell = (ListCell) PARENT_LIST.list_panel.getComponent(my_index - 1);

                            prev_cell.set_selected();
                            prev_cell.requestFocus();
                        } catch (Exception ex) {} //se non esiste un elemento ad index my_index - 1
                        break;

                    case 27: //esc
                        unselect();
                        break;

                    case 10: //invio, si collega a questo server
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
                set_selected();
            }
        };

        public void set_selected() {
            if (PARENT_LIST.selected_index != my_index) {
                //deseleziona la casella selezionata in precedenza, se ne era selezionata una
                if (PARENT_LIST.selected_index != -1) {
                    ((ListCell) PARENT_LIST.list_panel.getComponent(PARENT_LIST.selected_index)).unselect();
                }

                //imposta questa JTextArea come selezionata
                setBackground(SEL_BACKGROUND);
                setBorder(BorderFactory.createLineBorder(SEL_BORDER));
                setCaretColor(SEL_BACKGROUND);
                setSelectionColor(SEL_BACKGROUND);

                PARENT_LIST.selected_index = my_index;
            }
        }

        public void unselect() {
            setBackground(STD_BACKGROUND);
            setBorder(null);
            setCaretColor(STD_BACKGROUND);
            setSelectionColor(STD_BACKGROUND);

            PARENT_LIST.selected_index = -1;
        }
    }
}

class FullScreenLayeredPane extends JLayeredPane {
    private Vector<Component> full_screen_components = new Vector<>();
    public void add_fullscreen(Component comp, int index) {
        full_screen_components.add(comp);
        super.add(comp, index);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);

        for (Component c : full_screen_components) {
            c.setBounds(0, 0, width, height);
        }
    }
}