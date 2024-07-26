package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.Vector;

abstract public class TempPanel {
    private static JButton ok_button = new JButton();
    private static JButton annulla_button = new JButton();
    private static final int MIN_WIDTH = 220; //ok_button.width + annulla_button.width + 30 (insects)
    private static final int MIN_HEIGHT = 40; //butto.height + 20 (insects)

    public static Vector<JTextField> input_array = new Vector<>();
    private static TempPanel_action action = null;
    private static Vector<Pair<TempPanel_info, TempPanel_action>> queue = new Vector<>();

    private static JPanel temp_panel = null; //temp panel
    private static JPanel txt_panel = new JPanel(); //pannello che contiene le txt area
    private static boolean visible = false;

    public static JPanel init() {
        if (temp_panel == null) {
            //imposta layout, background, border dei JPanel
            temp_panel = new JPanel();
            temp_panel.setLayout(new GridBagLayout());
            txt_panel.setLayout(new GridBagLayout());
            temp_panel.setBackground(new Color(58, 61, 63));
            txt_panel.setBackground(new Color(58, 61, 63));
            temp_panel.setBorder(BorderFactory.createLineBorder(new Color(38, 41, 43)));
            txt_panel.setBorder(null);

            //inizializza i bottoni ok ed annulla
            ok_button.setIcon(new ImageIcon(TempPanel.class.getResource("/images/ok.png")));
            ok_button.setPressedIcon(new ImageIcon(TempPanel.class.getResource("/images/ok_pres.png")));
            ok_button.setSelectedIcon(new ImageIcon(TempPanel.class.getResource("/images/ok_sel.png")));
            annulla_button.setIcon(new ImageIcon(TempPanel.class.getResource("/images/cancel.png")));
            annulla_button.setPressedIcon(new ImageIcon(TempPanel.class.getResource("/images/cancel_pres.png")));
            annulla_button.setSelectedIcon(new ImageIcon(TempPanel.class.getResource("/images/cancel_sel.png")));

            ok_button.addActionListener(ok_listener);
            annulla_button.addActionListener(annulla_listener);

            ok_button.setBorder(null);
            annulla_button.setBorder(null);

            ok_button.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {}
                @Override
                public void keyReleased(KeyEvent e) {}

                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == 10) { //se viene premuto invio è come premere ok
                        ok_button.doClick();
                    } else if (annulla_button.isVisible() && e.getKeyCode() == 27) { //se viene premuto esc è come premere annulla
                        annulla_button.doClick();
                    }
                }
            });

            //aggiunge gli elementi al tempPanel
            GridBagConstraints c = new GridBagConstraints();

            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(10, 10, 0, 0);
            c.weighty = 1;
            c.weightx = 1;
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 2;
            temp_panel.add(txt_panel, c);

            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.FIRST_LINE_START;
            c.insets = new Insets(0, 10, 10, 10);
            c.weighty = 0;
            c.gridy = 1;
            c.gridwidth = 1;
            temp_panel.add(annulla_button, c);

            c.anchor = GridBagConstraints.FIRST_LINE_END;
            c.insets.left = 0;
            c.gridx = 1;
            temp_panel.add(ok_button, c);

            //imposta il colore del background del testo selezionato
            UIManager.put("TextField.selectionBackground", new javax.swing.plaf.ColorUIResource(new Color(178, 191, 193)));
            UIManager.put("TextArea.selectionBackground", new javax.swing.plaf.ColorUIResource(new Color(178, 191, 193)));
            UIManager.put("PasswordField.selectionBackground", new javax.swing.plaf.ColorUIResource(new Color(178, 191, 193)));
        }
        return temp_panel;
    }

    private static ActionListener ok_listener = e -> {
        Vector<String> input_txt = new Vector<>(); //copia tutti i testi contenuti nelle input area in questo array
        while (input_array.size() != 0) {
            input_txt.add(input_array.elementAt(0).getText());
            input_array.removeElementAt(0);
        }

        TempPanel_action action = TempPanel.action; //memorizza l'azione da eseguire per questo panel
        reset(); //resetta tutta la grafica e fa partire il prossimo in coda

        if (action != null) { //se è stata specificata un azione
            if (input_txt.size() == 0 || valid_input()) { //se è un messaggio, o se richiede un input ed è stato inserito testo valido
                action.input.removeAllElements();
                action.input.addAll(input_txt); //fa partire il codice con tutti gli input ricavati
                new Thread(() -> action.success()).start();
            } else { //se non è stato inserito un input valido
                new Thread(() -> action.fail()).start();
            }
        }
    };

    private static ActionListener annulla_listener = e -> {
        input_array.removeAllElements(); //rimuove tutti gli input precedenti

        reset(); //resetta tutta la grafica e fa partire il prossimo in coda

        if (action != null) {
            new Thread(() -> action.fail()).start();
        }
    };

    public static void show(TempPanel_info info, TempPanel_action action) {
        if (!visible) {
            try {
                //imposta questa azione come quella da eseguire una volta chiusa la finestra
                TempPanel.action = action;

                //imposta la visibilità del tasto annulla
                annulla_button.setVisible(info.annulla_vis()); //mostra il pulsante annulla solo se richiesto

                //distingue nei vari tipi di finestra
                int panel_type = info.get_type();
                if (panel_type == TempPanel_info.INPUT_REQ) { //se richiede degli input
                    request_input(info.get_txts(), info.request_psw(), info.get_psw_index());
                }
                else if (panel_type == TempPanel_info.SINGLE_MSG) { //se mostra un singolo messaggio
                    show_msg(info.get_txts());
                }
                else if (panel_type == TempPanel_info.DOUBLE_COL_MSG) {
                    show_dual_con_msg(info.get_txts());
                }
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            queue.add(new Pair<>(info, action)); //aggiunge questa richiesta alla coda
        }
    }

    private static void show_msg(String[] txts) { //mostra un messaggio
        //aggiunge tutte le linee a txt_panel
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 10, 10);

        for (int i = 0; i < txts.length; i++) {
            NoEditTextArea line_area = new NoEditTextArea(txts[i]);

            c.insets.bottom = (i == txts.length - 1)? 10 : 0;
            txt_panel.add(line_area, c);

            c.gridy ++;
        }

        show_panel(txt_panel.getPreferredSize().width + 20, txt_panel.getPreferredSize().height + 10); //rende visibile il pannello
        ok_button.requestFocus(); //richiede il focus, in modo che se premuto invio appena il popup compare equivale a premere "ok"
    }

    private static void show_dual_con_msg(String[] txts) {
        //aggiunge tutte le linee a txt_panel
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 10, 10);

        for (int i = 0; i < txts.length; i++) {
            NoEditTextArea line_area1 = new NoEditTextArea(txts[i]);
            NoEditTextArea line_area2 = new NoEditTextArea(txts[++i]);

            c.insets.bottom = (i == txts.length - 1)? 10 : 0;
            c.gridx = 0;
            txt_panel.add(line_area1, c);

            c.gridx = 1;
            txt_panel.add(line_area2, c);

            c.gridy ++;
        }

        show_panel(txt_panel.getPreferredSize().width + 20, txt_panel.getPreferredSize().height + 10); //rende visibile il pannello
        ok_button.requestFocus(); //richiede il focus, in modo che se premuto invio appena il popup compare equivale a premere "ok"
    }

    private static void request_input(String[] requests, boolean request_psw, boolean[] psw_indices) throws IOException {
        int max_width = 0; //contiene la lunghezza della JTextArea che contiene il messaggio più lungo

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 10, 10);

        //genera e aggiunge al pannello txt_panel tutti le JTextArea
        for (c.gridy = 0; c.gridy < requests.length; c.gridy++) {
            //genera il JTextField che mostra il messaggio per richiedere l'input e lo aggiunge al pannello
            NoEditTextArea msg_area = new NoEditTextArea(requests[c.gridy]);
            if (msg_area.getPreferredSize().width > max_width) {
                max_width = msg_area.getPreferredSize().width;
            }

            c.weightx = 1;
            c.gridx = 0;
            txt_panel.add(msg_area, c);

            JTextField input_field;
            //aggiunge il TextField dove poter inserire l'input richiesto
            if (psw_indices[c.gridy]) { //se deve aggiungere un PasswordField
                input_field = new PasswordField(c.gridy);

                //aggiunge al pannello PasswordField ed il pulsante per togglare la visibilità della scritta
                c.weightx = 0;
                c.gridx = 1;
                c.insets.right = 3;
                txt_panel.add(input_field, c);

                c.gridx = 2;
                c.insets.right = 10;
                txt_panel.add(((PasswordField) input_field).get_toggle_button(), c);
            }
            else //se deve aggiungere un FocusField
            {
                input_field = new EditTextField(c.gridy);

                //aggiunge al pannello input_field
                c.weightx = 0;
                c.gridx = 1;
                c.gridwidth = (request_psw)? 2 : 1; //se richiede delle password i campi di inserimento normali si estendono anche nella colonna del pulsante per mostrare il testo delle password
                txt_panel.add(input_field, c);

                c.gridwidth = 1; //resetta gridwidth
            }

            input_array.add(input_field); //aggiunge gli input_field in un vettore per poi ricavarne i testi inseriti
        }

        show_panel(max_width + EditTextField.WIDTH + 30, (EditTextField.HEIGHT + 10) * input_array.size());
        input_array.elementAt(0).requestFocusInWindow(); //richiede il focus nella prima input area
    }

    private static void show_panel(int comp_width, int comp_height) {
        //calcola le dimensioni
        temp_panel.setSize(
                (comp_width > MIN_WIDTH)? comp_width : MIN_WIDTH,
                comp_height + MIN_HEIGHT
        );

        //disattiva tutti i pannelli in Godzilla_frame e ricenta TempPanel
        Server_frame.recenter_temp_panel();

        //mostra il pannello
        temp_panel.setVisible(true);
        temp_panel.updateUI();
        visible = true;
    }

    private static boolean valid_input() //controlla che nessun campo di input sia stato lasciato vuoto o con solo uno spazio/a capo
    {
        for (JTextField txt_field : input_array)
        {
            String txt = txt_field.getText();
            txt = txt.replaceAll("[ \n]", ""); //rimuove tutti gli spazi e \n

            if (txt.equals(""))
            {
                return false;
            }
        }
        return true;
    }

    private static void reset() {
        //resetta il pannello e lo rende invisibile
        visible = false;
        annulla_button.setVisible(true);
        temp_panel.setVisible(false);
        txt_panel.removeAll();

        if (queue.size() != 0) { //se c'è qualche elemento nella coda
            Pair<TempPanel_info, TempPanel_action> next_in_queue = queue.elementAt(0); //mostra il prossimo elemento nella coda
            queue.removeElementAt(0);
            show(next_in_queue.el1, next_in_queue.el2);
        }
    }

    private static class NoEditTextArea extends JTextArea {
        private static final int MAX_WIDTH = 600;
        private static final int MAX_HEIGHT = 100;
        private static final int LINE_HEIGHT = 17;
        public NoEditTextArea(String txt) {
            super(txt);

            this.setBackground(new Color(58, 61, 63));
            this.setBorder(null);
            this.setFont(new Font("Charter Bd BT", Font.PLAIN, 13));
            this.setForeground(new Color(188, 191,  193));

            calculate_size();

            this.setEditable(false);
        }

        private void calculate_size() {
            if(this.getPreferredSize().width > MAX_WIDTH) {
                this.setLineWrap(true);

                FontMetrics fm = this.getFontMetrics(this.getFont());
                this.setPreferredSize(new Dimension(
                        MAX_WIDTH,
                        (fm.stringWidth(this.getText()) / MAX_WIDTH + 1) * LINE_HEIGHT
                ));
            }
            else {
                this.setPreferredSize(new Dimension(
                        this.getPreferredSize().width,
                        (this.getPreferredSize().height > MAX_HEIGHT) ? MAX_HEIGHT : this.getPreferredSize().height
                ));
            }
        }
    }

    private static class EditTextField extends JTextField {
        protected static final int WIDTH  = 150;
        protected static final int HEIGHT = 20;

        private final int INDEX;
        public EditTextField(int index) {
            super();

            this.INDEX = index;

            this.setBackground(new Color(108, 111, 113));
            this.setBorder(BorderFactory.createLineBorder(new Color(68, 71, 73)));
            this.setFont(new Font("Arial", Font.BOLD, 14));
            this.setForeground(new Color(218, 221, 223));

            this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
            this.setMinimumSize(this.getPreferredSize());

            this.addKeyListener(enter_list);
        }

        private KeyListener enter_list = new KeyListener() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == 10) { //10 -> enter
                    setText(getText().replaceAll("\n", "")); //rimuove la nuova linea

                    try {
                        input_array.elementAt(INDEX + 1).grabFocus(); //passa il focus all'input successivo
                    } catch (Exception ex) { //se non esiste un input con index > di questo
                        ok_button.doClick(); //simula il tasto "ok"
                    }
                }
                else if (e.getKeyCode() == 27 && annulla_button.isVisible()) { //27 -> esc
                    annulla_button.doClick();
                }
            }
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {}
        };
    }

    private static class PasswordField extends JPasswordField {
        private static final int WIDTH  = 127;
        private static final int HEIGHT = 20;

        private JButton toggle_button = null;
        private static ImageIcon[] eye_icons = new ImageIcon[] {
                new ImageIcon(TempPanel.class.getResource("/images/eye.png")),
                new ImageIcon(TempPanel.class.getResource("/images/eye_pres.png")),
                new ImageIcon(TempPanel.class.getResource("/images/eye_sel.png"))

        };
        private static ImageIcon[] no_eye_icons = new ImageIcon[] {
                new ImageIcon(TempPanel.class.getResource("/images/no_eye.png")),
                new ImageIcon(TempPanel.class.getResource("/images/no_eye_pres.png")),
                new ImageIcon(TempPanel.class.getResource("/images/no_eye_sel.png"))
        };

        private final int INDEX;
        private boolean clear_text = false;

        public PasswordField(int index) {
            super();
            this.INDEX = index;

            this.setBackground(new Color(108, 111, 113));
            this.setBorder(BorderFactory.createLineBorder(new Color(68, 71, 73)));
            this.setFont(new Font("Arial", Font.BOLD, 14));
            this.setForeground(new Color(218, 221, 223));

            this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
            this.setMinimumSize(this.getPreferredSize());

            this.setEchoChar('*'); //nasconde il testo
            gen_toggle_button(); //genera il pulsante per togglare la visibilità del testo

            this.addKeyListener(enter_list);
        }

        public JButton get_toggle_button() {
            return toggle_button;
        }

        private void gen_toggle_button() { //genera un pulsante che premuto toggla la visibilità del testo
            toggle_button = new JButton();

            //inizializza la grafica del pulsante con le icone dell'occhio senza la barra
            toggle_button.setIcon(eye_icons[0]);
            toggle_button.setPressedIcon(eye_icons[1]);
            toggle_button.setRolloverIcon(eye_icons[2]);

            toggle_button.setBorder(null);

            //aggiunge action listener e ritorna il pulsante
            toggle_button.addActionListener(toggle_list);
        }

        private ActionListener toggle_list = e -> {
            if (clear_text) //se in questo momento il testo si vede in chiaro
            {
                setEchoChar('*'); //nasconde il testo

                //modifica le icone del pulsante
                toggle_button.setIcon(eye_icons[0]);
                toggle_button.setPressedIcon(eye_icons[1]);
                toggle_button.setRolloverIcon(eye_icons[2]);
            }
            else //se in questo momeno il testo è nascosto
            {
                setEchoChar((char) 0); //mostra il testo

                //modifica le icone del pulsante
                toggle_button.setIcon(no_eye_icons[0]);
                toggle_button.setPressedIcon(no_eye_icons[1]);
                toggle_button.setRolloverIcon(no_eye_icons[2]);
            }

            clear_text = !clear_text;
        };

        private KeyListener enter_list = new KeyListener() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == 10) { //10 -> enter
                    setText(getText().replaceAll("\n", "")); //rimuove la nuova linea

                    try {
                        input_array.elementAt(INDEX + 1).grabFocus(); //passa il focus all'input successivo
                    } catch (Exception ex) { //se non esiste un input con index > di questo
                        ok_button.doClick(); //simula il tasto "ok"
                    }
                }
                else if (e.getKeyCode() == 27 && annulla_button.isVisible()) { //27 -> esc
                    annulla_button.doClick();
                }
            }
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {}
        };
    }
}