package gui;

import network.Net_listener;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class Buttons_panel {
    public static String active_mod = "";

    private static JButton start_button;
    private static JButton stop_button;
    private static JButton info_button;

    private static JPanel buttons_list;
    private static JScrollPane buttons_scroller;
    private static Map<String, Runnable> stop_activities = new LinkedHashMap<>();

    private static JPanel buttons_panel = null;
    protected static JPanel init() throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if (buttons_list == null) {
            buttons_panel = new JPanel();
            buttons_panel.setLayout(new GridBagLayout());

            //inizializza tutti i componenti della gui
            JButton right_shift = new JButton();
            JButton left_shift = new JButton();
            JTextArea sep = new JTextArea();
            start_button = new JButton();
            stop_button = new JButton();
            info_button = new JButton();
            buttons_list = new JPanel();
            buttons_scroller = new JScrollPane(buttons_list, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            stop_button.setEnabled(false);

            sep.setFocusable(false);
            sep.setEditable(false);

            stop_button.setPreferredSize(new Dimension(30, 30));
            start_button.setPreferredSize(new Dimension(30, 30));
            info_button.setPreferredSize(new Dimension(30, 30));
            sep.setPreferredSize(new Dimension(1, 25));
            buttons_list.add(start_button);
            buttons_list.add(stop_button);
            buttons_list.add(info_button);
            buttons_list.add(sep); //separator

            right_shift.setIcon(new ImageIcon(Buttons_panel.class.getResource("/images/right_arrow.png")));
            right_shift.setRolloverIcon(new ImageIcon(Buttons_panel.class.getResource("/images/right_arrow_sel.png")));
            right_shift.setPressedIcon(new ImageIcon(Buttons_panel.class.getResource("/images/right_arrow_pres.png")));
            left_shift.setIcon(new ImageIcon(Buttons_panel.class.getResource("/images/left_arrow.png")));
            left_shift.setRolloverIcon(new ImageIcon(Buttons_panel.class.getResource("/images/left_arrow_sel.png")));
            left_shift.setPressedIcon(new ImageIcon(Buttons_panel.class.getResource("/images/left_arrow_pres.png")));
            start_button.setIcon(new ImageIcon(Buttons_panel.class.getResource("/images/start.png")));
            start_button.setRolloverIcon(new ImageIcon(Buttons_panel.class.getResource("/images/start_sel.png")));
            start_button.setPressedIcon(new ImageIcon(Buttons_panel.class.getResource("/images/start_pres.png")));
            start_button.setDisabledIcon(new ImageIcon(Buttons_panel.class.getResource("/images/start_dis.png")));
            stop_button.setIcon(new ImageIcon(Buttons_panel.class.getResource("/images/stop.png")));
            stop_button.setRolloverIcon(new ImageIcon(Buttons_panel.class.getResource("/images/stop_sel.png")));
            stop_button.setPressedIcon(new ImageIcon(Buttons_panel.class.getResource("/images/stop_pres.png")));
            stop_button.setDisabledIcon(new ImageIcon(Buttons_panel.class.getResource("/images/stop_dis.png")));
            info_button.setIcon(new ImageIcon(Buttons_panel.class.getResource("/images/info.png")));
            info_button.setRolloverIcon(new ImageIcon(Buttons_panel.class.getResource("/images/info_sel.png")));
            info_button.setPressedIcon(new ImageIcon(Buttons_panel.class.getResource("/images/info_pres.png")));
            info_button.setDisabledIcon(info_button.getIcon());

            right_shift.setBorder(null);
            left_shift.setBorder(null);
            start_button.setBorder(null);
            stop_button.setBorder(null);
            info_button.setBorder(null);
            buttons_scroller.setBorder(null);
            sep.setBorder(BorderFactory.createLineBorder(Color.GRAY.darker()));

            right_shift.addActionListener(right_shift_listener);
            left_shift.addActionListener(left_shift_listener);
            start_button.addActionListener(start_listener);
            stop_button.addActionListener(stop_listener);
            info_button.addActionListener(info_listener);

            buttons_list.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
            buttons_list.setBackground(new Color(58, 61, 63));

            //aggiunge tutti i componenti al pannello organizzandoli nella griglia
            GridBagConstraints c = new GridBagConstraints();

            c.fill = GridBagConstraints.BOTH;
            c.gridy = 0;
            c.weightx = 0; //i due pulsanti non vengono ridimensionati

            c.gridx = 0;
            buttons_panel.add(left_shift, c);

            c.gridx = 2;
            buttons_panel.add(right_shift, c);

            c.weightx = 1;

            c.gridx = 1;
            buttons_panel.add(buttons_scroller, c);

            buttons_panel.setPreferredSize(new Dimension(0, 30));
            init_buttons();
        }
        return buttons_panel;
    }

    public static void init_buttons() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Terminal_panel.terminal_write("inizializzo i bottoni nella gui:", false);
        File buttons_folder = new File(Server_frame.project_path + "/mod");
        String[] button_class_files = buttons_folder.list();

        class Button_class extends ClassLoader {
            public Class find_class(String class_name) throws IOException {
                byte[] class_data = new FileInputStream(Server_frame.project_path + "/mod/" + class_name + ".class").readAllBytes(); //read file
                return defineClass(class_name, class_data, 0, class_data.length); //define class
            }
        }
        Button_class class_gen = new Button_class();

        for (String file_name : button_class_files) {
            Terminal_panel.terminal_write("\n   inizializzo - " + file_name + ": ", false);

            if (file_name.substring(file_name.length() - 6, file_name.length()).equals(".class")) { //se è un file .class
                String class_name = file_name.substring(0, file_name.length() - 6);
                Class button_class = class_gen.find_class(class_name);

                //imposta la funzione "public static void on_press(String name)" all'interno della classe per essere invocata una volta premuto un pulsante
                Buttons_panel.on_press = button_class.getDeclaredMethod("on_press", String.class);

                //all'interno della classe dovrà essere definita una funzione "public static void register_button()" che viene invocata ora per far registrare tutti i bottoni
                button_class.getDeclaredMethod("register_button").invoke(null);
            }
        }
        Terminal_panel.terminal_write(" - finito\n", false);
    }

    private static Method on_press;
    public static void register_button(ButtonInfo info, Runnable stop) {
        stop_activities.put(info.name, stop); //registra il metodo per stoppare l'azione di questo pulsante
        Terminal_panel.terminal_write("pulsante registrato!\n", false);

        JButton b = new JButton();
        b.setToolTipText(info.name);

        b.setIcon(info.default_icon);
        b.setRolloverIcon(info.rollover_icon);
        b.setPressedIcon(info.pressed_icon);
        b.setDisabledIcon(info.disabled_icon);

        b.addActionListener(new ActionListener() {
            private final Method on_press = Buttons_panel.on_press;
            private final String name = b.getToolTipText();
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (Buttons_panel.active_mod.equals("")) {
                        Terminal_panel.terminal_write("la mod " + name + " è stata attivata\n", false);

                        Buttons_panel.active_mod = name;
                        on_press.invoke(null, name);
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        b.setPreferredSize(new Dimension(info.default_icon.getIconWidth(), info.default_icon.getIconHeight()));
        b.setBorder(null);

        buttons_list.add(b);
        buttons_scroller.updateUI();
        buttons_scroller.setBorder(null); //altrimenti con updateUI() si mostra il bordo
    }

    public static void end_mod() {
        if (!Buttons_panel.active_mod.equals("")) { //se c'è effettivamente una mod attiva
            Terminal_panel.terminal_write("la mod " + Buttons_panel.active_mod + " è stata disattivata\n", false);

            stop_activities.get(Buttons_panel.active_mod).run();
            Buttons_panel.active_mod = "";
        }
    }

    private static ActionListener left_shift_listener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            buttons_scroller.getHorizontalScrollBar().setValue(
                    buttons_scroller.getHorizontalScrollBar().getValue() - 30
            );
        }
    };

    private static ActionListener right_shift_listener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            buttons_scroller.getHorizontalScrollBar().setValue(
                    buttons_scroller.getHorizontalScrollBar().getValue() + 30
            );
        }
    };

    private static ActionListener start_listener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (Net_listener.is_ready()) {
                start_button.setEnabled(false);
                stop_button.setEnabled(true);

                Net_listener.start();
                Terminal_panel.terminal_write("----- server attivo -----\n", false);
            }
        }
    };

    private static ActionListener stop_listener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            stop_button.setEnabled(false);
            start_button.setEnabled(true);

            Net_listener.stop();
            Terminal_panel.terminal_write("----- server stoppato -----\n", false);
        }
    };

    private static ActionListener info_listener = e -> {
        try {
            Pattern new_info = Pattern.compile(";");
            String[] info = new_info.split(new String(Net_listener.server_info)); //ricava tutte le informazioni del server

            TempPanel.show(new TempPanel_info(
                    TempPanel_info.DOUBLE_COL_MSG,
                    false,
                    "registered name:", info[0],
                    "server link:", info[1],
                    "server ip:", info[2],
                    "server public key:", info[3],
                    "server certificate:", Base64.getEncoder().encodeToString(Net_listener.certificate),
                    "server mail:", info[4]
            ), null);
        }
        catch (NullPointerException ex) {} // se non è ancora stata inserita la password per decifrare i file non è possibile conoscere le informazioni del server
    };
}