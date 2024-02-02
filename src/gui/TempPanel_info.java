package gui;

public class TempPanel_info {
    private boolean show_annulla = true; //imposta la visibilità del pulsante "annulla"
    private boolean request_psw = false; //false = non richiede nessuna password, true richiede delle password
    private String[] requests_text; //se si vogliono richiedere degli input in questo vettore vengono inserite tutte le richieste
    private String msg_text = null; //rimane null se si vogliono richiedere degli input, altrimenti contiene il testo del messaggio da visualizzare
    private boolean[] password_indices = null; //per ogni richiesta di input memorizza se richiede una password o meno

    public TempPanel_info(String txt, boolean show_annulla) { //si vuole far visualizzare un messaggio senza richiedere input
        this.show_annulla = show_annulla;
        msg_text = txt;
    }

    public TempPanel_info(String... requests) { //vengono richiesti vari input e nessuno di questi è una password
        requests_text = requests.clone();
        password_indices = new boolean[requests_text.length]; //inizializza il vettore con tutti false
    }

    public TempPanel_info set_psw_indices(int... psw_indices) { //specifica quali fra le richieste che ha inserito richiedono delle password
        request_psw = true;

        for (int index : psw_indices) { //rende true i boolean in password_indices ad indice contenuto in psw_indices
            password_indices[index] = true;
        }

        return this;
    }

    public boolean annulla_vis() { return show_annulla; }
    public boolean request_input() { return msg_text == null; } //true = richiede degli input, false = non richiede input
    public boolean request_psw() { return request_psw; }
    public String[] get_requests() { return requests_text; }
    public String get_msg() { return msg_text; }
    public boolean[] get_psw_info() { return password_indices; }
}
