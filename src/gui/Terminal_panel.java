package gui;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public abstract class Terminal_panel {
    private static TerminalJTextArea terminal = new TerminalJTextArea();
    public static final boolean DEBUGGING = true;

    private static JPanel terminal_panel = null;
    private static GScrollPane terminal_scroller;

    private static final EventQueue EVENT_QUEUE = Toolkit.getDefaultToolkit().getSystemEventQueue();
    private static final Color ERROR_BACKGROUND = Color.lightGray;
    private static final Color ERROR_FOREGROUND = Color.red.darker();

    public static JPanel init() {
        if (terminal_panel == null) {
            terminal_panel = new JPanel();
            terminal_scroller = new GScrollPane(terminal);

            terminal_scroller.setBackground(new Color(128, 131, 133));

            terminal_panel.setLayout(new GridLayout(1, 1));
            terminal_panel.add(terminal_scroller);
        }
        return terminal_panel;
    }

    public synchronized static void terminal_write(String txt, boolean error) {
        if (error) {
            EVENT_QUEUE.postEvent(new TerminalEvent(terminal, txt, ERROR_BACKGROUND, ERROR_FOREGROUND, true));
        }
        else {
            EVENT_QUEUE.postEvent(new TerminalEvent(terminal, txt));
        }
    }

    public static String get_terminal_log() {
        return terminal.getText();
    }

    private static void show_last_line() {
        if (terminal_scroller.getVerticalScrollBar().isValid()) {
            terminal_scroller.getVerticalScrollBar().setValue(terminal_scroller.getVerticalScrollBar().getMaximum());
        }
    }

    private static class TerminalJTextArea extends JTextPane {
        private Document doc;

        public TerminalJTextArea() {
            super();

            this.doc = this.getStyledDocument();
            this.enableEvents(TerminalEvent.ID); //accetta gli eventi del terminal

            this.setBackground(Color.BLACK);
            this.setForeground(Color.lightGray);
            this.setCaretColor(Color.WHITE);
            this.setSelectionColor(new Color(180, 180, 180));
            this.setSelectedTextColor(new Color(30, 30, 30));

            this.setEditable(false);

            this.setText(" ================================== Server Starting " + get_data_time() + " ==================================\n");
        }

        private static String get_data_time() {
            String pattern = "dd.MM.yyyy - HH:mm.ss";
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            Calendar c = Calendar.getInstance();

            return sdf.format(c.getTime());
        }

        @Override
        protected void processEvent(AWTEvent e) {
            if (e instanceof TerminalEvent) {
                try {
                    TerminalEvent event = (TerminalEvent) e; //ricava l'oggetto TerminalEvent
                    SimpleAttributeSet attrib = new SimpleAttributeSet();

                    StyleConstants.setForeground(attrib, event.get_foreground()); //imposta la grafica del testo che vuole aggiungere al terminale
                    StyleConstants.setBackground(attrib, event.get_background());
                    StyleConstants.setBold(attrib, event.is_bold());

                    //aggiunge il testo al terminale e "va in giù" con la scrollbar
                    doc.insertString(doc.getLength(), event.get_txt(), attrib);
                    show_last_line();
                } catch (BadLocationException ex) {
                    throw new RuntimeException(ex);
                }
            }
            else {
                super.processEvent(e);
            }
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return getUI().getPreferredSize(this).width <= getParent().getSize().width;
        }
    }

}

class TerminalEvent extends AWTEvent {
    public static final int ID = AWTEvent.RESERVED_ID_MAX + 1;
    private static int counter = 0;

    private String txt;
    private Color background = Color.BLACK;
    private Color foreground = Color.lightGray.brighter();
    private boolean bold = false;

    public TerminalEvent(Object source, String txt) {
        super(source, ID);
        this.txt = txt;
    }

    public TerminalEvent(Object source, String txt, Color background, Color foreground, boolean bold) {
        super(source, ID);

        this.txt = txt;
        this.background = background;
        this.foreground = foreground;
        this.bold = bold;
    }

    public String get_txt() {
        return this.txt;
    }
    public Color get_background() { return background; }
    public Color get_foreground() { return foreground; }
    public boolean is_bold() { return bold; }
}