package gui;

import javax.crypto.Cipher;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

public abstract class Server_frame {
    public static final String project_path;
    public static Cipher file_decoder;
    public static Cipher file_encoder;

    static { //calcola la posizione assoluta del progetto
        String jar_path = Server_frame.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        jar_path = jar_path.substring(0, jar_path.length() - 1); //rimuove l'ultimo /
        project_path = jar_path.substring(0, jar_path.lastIndexOf('/'));
    }

    private static JFrame frame = null;

    private static JPanel terminal_p;
    private static JPanel buttons_p;
    private static JPanel clients_p;
    private static JPanel temp_panel;

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

        //inizializza i pannelli che formano la schermata principale
        terminal_p = Terminal_panel.init();
        buttons_p = Buttons_panel.init();
        clients_p = Clients_panel.init();
        temp_panel = TempPanel.init();

        layeredPane.add_fullscreen(main_panel, JLayeredPane.FRAME_CONTENT_LAYER);
        layeredPane.add(temp_panel, JLayeredPane.POPUP_LAYER);

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

        frame.addComponentListener(new ComponentListener() {
            @Override
            public void componentMoved(ComponentEvent e) {}
            @Override
            public void componentShown(ComponentEvent e) {}

            @Override
            public void componentHidden(ComponentEvent e) {}
            @Override
            public void componentResized(ComponentEvent e) {
                if (temp_panel.isVisible()) { //se temp panel è visibile lo ricentra ogni volta che viene ridimensionato il frame
                    recenter_temp_panel();
                }
            }
        });

        return frame;
    }

    public static void recenter_temp_panel() { //ricentra il pannello TempPanel
        temp_panel.setLocation(
                frame.getWidth() / 2 - temp_panel.getWidth() / 2,
                frame.getHeight() / 2 - temp_panel.getHeight() / 2
        );
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