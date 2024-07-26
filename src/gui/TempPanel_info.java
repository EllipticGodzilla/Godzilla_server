package gui;

public class TempPanel_info {
    public static final int INPUT_REQ = 0,
            SINGLE_MSG = 1,
            DOUBLE_COL_MSG = 2;
    private final int TYPE;

    private boolean show_annulla; //imposta la visibilità del pulsante "annulla"
    private boolean request_psw = false; //false = non richiede nessuna password, true richiede delle password
    private String[] msg_text; //contiene tutti i messaggi da mostrare nel temp_panel
    private boolean[] password_indices = null; //per ogni richiesta di input memorizza se richiede una password o meno

    public TempPanel_info(int type, boolean show_annulla, String... txts) {
        this.TYPE = type;
        this.msg_text = txts;
        this.show_annulla = show_annulla;

        password_indices = new boolean[txts.length]; // di default inizializza l'array con tutti false
    }

    public TempPanel_info set_psw_indices(int... psw_indices) { //specifica quali fra le richieste che ha inserito richiedono delle password
        request_psw = true;

        for (int index : psw_indices) { //rende true i boolean in password_indices ad indice contenuto in psw_indices
            password_indices[index] = true;
        }

        return this;
    }

    public boolean annulla_vis() { return show_annulla; }
    public int get_type() { return TYPE; }
    public boolean request_psw() { return request_psw; }
    public String[] get_txts() { return msg_text; }
    public boolean[] get_psw_index() { return password_indices; }
}
