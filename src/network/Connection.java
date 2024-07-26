package network;

import gui.Pair;
import gui.TempPanel;
import gui.Terminal_panel;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

public class Connection {
    private Socket sck;
    private BufferedOutputStream output;
    private BufferedInputStream input;
    private Receiver receiver;

    private AESCipher cipher = null;

    private boolean secure = false;
    private boolean dynamic = false;
    private SecureRandom random = new SecureRandom();

    public Connection(Socket sck) throws IOException {
        this.sck = sck;
        this.output = new BufferedOutputStream(sck.getOutputStream());
        this.input = new BufferedInputStream(sck.getInputStream());
        this.receiver = new Receiver(input, this);
    }

    public synchronized void write(String msg) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException {
        write(msg.getBytes());
    }

    public synchronized void write(byte[] msg) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException { //non si aspetta nessuna risposta e non invia una risposta
        write(msg, null);
    }

    public synchronized void write(byte[] msg, On_arrival action) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException { //non invia una risposta, e si aspetta una risposta
        byte conv_code = (action == null)? 0x00 : register_conv(action);

        write(conv_code, msg);
    }

    public synchronized void write(byte conv_code, byte[] msg, On_arrival action) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException { //invia una risposta e si aspetta una risposta
        //registra la nuova azione da eseguire una volta ricevuta la risposta
        receiver.new_conv(conv_code, action);
        write(conv_code, msg); //invia la risposta
    }

    public synchronized void write(byte conv_code, byte[] msg) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException { //invia una risposta
        byte[] msg_prefix = concat_array(new byte[] {conv_code}, msg); //concatena conv_code e msg[]
        direct_write(msg_prefix); //se possibile cifra e invia il messaggio

        if (Terminal_panel.DEBUGGING) { Terminal_panel.terminal_write("invio al client " + receiver.username + " [" + conv_code + "] " + new String(msg) + "\n", false); }
    }

    public void direct_write(byte[] msg) throws IOException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        if (secure) { //è una connessione sicura cifra il messaggio
            msg = cipher.encode(msg);
        }

        output.write(new byte[]{(byte) (msg.length & 0xff), (byte) ((msg.length >> 8) & 0xff)}); //invia 2 byte che indicano la dimensione del messaggio
        output.write(msg); //invia il messaggio

        output.flush();
    }

    public byte[] wait_for_bytes() { //se non è una connessione dinamica, attende che vengano inviati dei bytes
        if (!dynamic) {
            try {
                byte[] msg_size_byte = input.readNBytes(2); //legge la dimensione del messaggio che sta arrivando
                int msg_len = (msg_size_byte[0] & 0Xff) | (msg_size_byte[1] << 8); //trasforma i due byte appena letti in un intero

                byte[] msg = input.readNBytes(msg_len); //legge il messaggio appena arrivato

                if (secure) { //se può decifra il messaggio
                    msg = cipher.decode(msg);
                }

                return msg; //se la connessione non è dinamica non viene aggiunto il byte conv_code quindi ritorna il messaggio così come è arrivato
            } catch (Exception e) { //connessione con il server chiusa
                return null;
            }
        }
        else { //se la connessione è dinamica non si possono ricevere bytes in questo modo
            Terminal_panel.terminal_write("wait_for_bytes() può essere utilizzato solamente quando la connessione non è dinamica!\n", true);
            throw new RuntimeException("wait_for_bytes() is a only non-dynamic connection function");
        }
    }

    public boolean set_cipher(AESCipher cipher) {
        if (!secure) {
            this.cipher = cipher;
            this.receiver.set_cipher(cipher);

            secure = true;

            return true;
        }
        else {
            return false;
        }
    }

    public void start_dinamic() {
        this.receiver.start();
        this.dynamic = true;
    }

    public void set_username (String usr) {
        this.receiver.set_username(usr);
    }

    public String get_username() {
        return this.receiver.username;
    }

    public void close() throws IOException {
        input.close();
        output.close();
        sck.close();

        receiver.stop();
    }

    public void pair(Connection conn) {
        receiver.set_paired(conn);
    }

    public String get_paired() { return receiver.get_paired(); }

    public void unpair() throws InvalidAlgorithmParameterException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException, InstantiationException, IllegalAccessException, InvocationTargetException {
        receiver.unpair("");
    }

    private  byte register_conv(On_arrival action) { //registra una nuova conversazione e ritorna il conv_code associato
        byte[] conv_code = new byte[1];
        do { //per evitare di avere conv_code duplicati o 0x00
            random.nextBytes(conv_code); //genera un byte casuale che utilizzerà come conv_code
        } while (conv_code[0] == 0x00 || !receiver.new_conv(conv_code[0], action));



        return conv_code[0];
    }

    private  byte[] concat_array(byte[] arr1, byte[] arr2) {
        int arr1_len = arr1.length;

        arr1 = Arrays.copyOf(arr1, arr1.length + arr2.length);
        System.arraycopy(arr2, 0, arr1, arr1_len, arr2.length);

        return arr1;
    }
}

class Receiver {
    private BufferedInputStream input;
    private Connection conn;
    public String username = null;

    private Vector<Pair<Byte, Long>> conv_exp = new Vector<>();
    private Map<Byte, On_arrival> conv_map = new LinkedHashMap<>(); //memorizza tutte le conversazioni che ha aperto con il client

    private Connection paired_conn = null;
    private boolean secure = false;
    private AESCipher cipher;
    private static final int CONV_EXP_TIME = 10000; //tempo in millisecondi che impiegano le conversazioni per scadere

    public Receiver(BufferedInputStream input, Connection c) {
        this.input = input;
        this.conn = c;
    }

    public void start() {
        reader.start();
    }

    public boolean new_conv(byte conv_code, On_arrival action) {
        if (conv_map.putIfAbsent(conv_code, action) == null) {
            conv_exp.add(new Pair<>(conv_code, System.currentTimeMillis() + CONV_EXP_TIME));

            if (!exp_checking) {
                new Thread(expiring).start(); //inizia ad eliminare le conversazioni scadute
            }

            return true;
        }
        return false;
    }

    public void set_username(String usr) {
        this.username = usr;
    }

    public boolean set_cipher(AESCipher cipher) {
        if (!secure) {
            this.cipher = cipher;
            secure = true;

            return true;
        }
        else {
            return false;
        }
    }

    public void stop() {
        input = null; //genera un errore nel thread reader e lo stoppa
        conv_map = new LinkedHashMap<>(); //resetta le conversazioni

        //resetta tutte le variabili
        secure = false;
    }

    private  boolean reading = true;
    private Thread reader = new Thread(() -> {
        try {
            while (reading) {
                byte[] msg_size_byte = input.readNBytes(2);
                int msg_len = (msg_size_byte[0] & 0Xff) | (msg_size_byte[1] << 8);

                byte[] msg = input.readNBytes(msg_len);
                if (secure) {
                    msg = cipher.decode(msg);
                }

                if (username == null) { //se non ha fatto ancora il login è obbligato a farlo/registrarsi prima d'inviare altri messaggi
                    byte conv_code = msg[0]; //memorizza il codice della conversazione
                    msg = Arrays.copyOfRange(msg, 1, msg.length); //elimina il conv_code dal messaggio

                    login_register(conv_code, msg);
                } else { //se ha già fatto il login, processa il messaggio o lo inoltra al client a cui è collegato
                    byte conv_code = msg[0]; //memorizza il codice della conversazione
                    msg = Arrays.copyOfRange(msg, 1, msg.length); //elimina il conv_code dal messaggio

                    if (Terminal_panel.DEBUGGING) { Terminal_panel.terminal_write("ricevuto dal client " + username + " [" + conv_code + "] " + new String(msg) + "\n", false); }

                    On_arrival conv_action = conv_map.get(conv_code); //ricava l'azione da eseguire per questa conversazione
                    if (conv_action == null && paired_conn == null) { //se non è una risposta ad una conversazione ed il client non è appaiato ad un altro client
                        process_server_msg(conv_code, msg);
                    } else if (conv_action == null && paired_conn != null) { //se non è una risposta ad una nuova conversazione ed il client è appaiato ad un altro
                        process_paired_msg(conv_code, msg);
                    } else if (conv_action != null) { //se è specificata un azione la esegue
                        conv_map.remove(conv_code);
                        for (int i = 0; i < conv_exp.size(); i++) { //rimuove la conversazione da quelle da controllare per la scandenza
                            if (conv_exp.elementAt(i).el1 == conv_code) {
                                conv_exp.remove(i);
                                break;
                            }
                        }

                        conv_action.on_arrival(conv_code, msg, this);
                    } else { //se il conv_code inviato dal client non è riconosciuto dal server
                        Terminal_panel.terminal_write("il client " + username + " ha risposto alla conversazione " + (int) conv_code + " ma non è registrata nel server\n", true);
                        conn.write(conv_code, new byte[]{0x00}); //risponde con 0x00 come messaggio di errore generale
                    }
                }
            }
        } catch (IOException | NullPointerException | ArrayIndexOutOfBoundsException e) {
            if (Net_listener.is_online(username)) {
                try {
                    process_server_msg((byte) 0x00, "EOC".getBytes()); //chiude la connessione
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    });

    private boolean exp_checking = false;
    private Runnable expiring = () -> {
        exp_checking = true;

        try {
            Thread.sleep(CONV_EXP_TIME); //appena fatto partire vuol dire che la conversazione che scadrà per prima è stata appena aggiunta, non ha senso controllare ogni 0.1s da subito

            while (conv_exp.size() != 0) {
                try {
                    for (int i = 0; conv_exp.elementAt(i).el2 <= System.currentTimeMillis(); i++) {
                        byte conv_code = conv_exp.elementAt(i).el1;
                        On_arrival act = conv_map.get(conv_code);

                        //rimuove la conversazione scaduta dai due registri
                        conv_exp.remove(i);
                        conv_map.remove(conv_code);

                        //fa partire il codice per quando una conversazione scade
                        act.timeout(conv_code, this);
                    }
                } catch (ArrayIndexOutOfBoundsException e) {}

                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        exp_checking = false;
    };

    private void login_register(byte conv_code, byte[] msg) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException { //esegue il login / registra
        String msg_str = new String(msg);

        if (msg_str.substring(0, 6).equals("login:")) { //vuole eseguire il login
            int pwd_off = msg_str.indexOf(";");
            String usr = msg_str.substring(6, pwd_off);
            byte[] pwd = Arrays.copyOfRange(msg, pwd_off + 1, msg.length);

            if (Net_listener.exist(usr, pwd) && !Net_listener.is_online(usr)) { //se le credenziali sono corrette e questo utente non è già online
                Terminal_panel.terminal_write("il client " + usr + " ha appena eseguito il login\n", false);

                conn.write(conv_code, "log".getBytes());
                conn.set_username(usr);
                Net_listener.login(usr, conn);
            }
            else { //se il login fallisce
                Terminal_panel.terminal_write("tentativo di login a " + usr + " fallito\n", true);
                conn.write(conv_code, "err".getBytes());
            }
        }
        else if (msg_str.substring(0, 9).equals("register:")) { //se vuole registrare un nuovo account
            int pwd_off = msg_str.indexOf(";");
            String usr = msg_str.substring(9, pwd_off);
            byte[] pwd = Arrays.copyOfRange(msg, pwd_off + 1, msg.length);

            if (Net_listener.register_account(usr, pwd)) { //riesce a creare il nuovo account
                Terminal_panel.terminal_write("è stato creato il nuovo account " + usr + "\n", false);

                conn.write(conv_code, "reg".getBytes());
                conn.set_username(usr);
                Net_listener.register(usr, conn);
            }
            else { //la registrazione è fallita
                Terminal_panel.terminal_write("tentativo di creare l'account " + usr + " fallito\n", true);

                conn.write(conv_code, "err".getBytes());
            }
        }
        else {
            Terminal_panel.terminal_write("un client ha inviato il messaggio (" + msg + ") senza prima eseguire il login/registrarsi\n", true);
        }
    }

    //metodi che fanno riferimento al collegamento fra client
    private void process_paired_msg(byte conv_code, byte[] msg) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException, InvocationTargetException, InstantiationException, IllegalAccessException { //processa i messaggi che si inviano due client appaiati
        String str_msg = new String(msg);

        if (str_msg.equals("EOP")) { //se vuole chiudere la connessione con l'altro client
            unpair(paired_conn.get_username());
            paired_conn = null;
        }
        else { //altrimenti inoltra il messaggio al client a cui è appaiato
            paired_conn.write(conv_code, msg);
        }
    }

    private void pair_to(String name, byte conv_code) throws IllegalBlockSizeException, IOException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        PairingResponse pair_act = new PairingResponse(
                conv_code, //conv_code del requester
                (byte) 0x00, // conv_code del ricevitore, non viene utilizzato in questa azione
                null, // viene inizializzata in Net_listener
                conn, // connessione con il requester
                username // username del requester
        );

        if (!Net_listener.request_pair(name, username, pair_act)) { //se il client a cui ci si vuole collegare non è già appaiato
            conn.write(conv_code, "den".getBytes());
        }
    }

    public String get_paired() {
        try {
            return paired_conn.get_username();
        }
        catch (Exception e) {
            return null;
        }
    }

    public void set_paired(Connection conn) {
        this.paired_conn = conn;
    }

    protected void unpair(String name) throws IllegalBlockSizeException, IOException, BadPaddingException, InstantiationException, IllegalAccessException, InvalidAlgorithmParameterException, InvalidKeyException, InvocationTargetException {
        if (!name.equals("")) { //se è questo client a voler scollegarsi e non quello a cui è appaiato
            paired_conn.write("EOP");
            paired_conn.unpair();

            Net_listener.unpair(name, username, false);
        }
        paired_conn = null;
    }

    //processo dei messaggi client -> server

    private void process_server_msg(byte conv_code, byte[] msg) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException, InvocationTargetException, InstantiationException, IllegalAccessException { //processa i messaggi che invia il client al server
        String msg_str = new String(msg);

        try {
            if (msg_str.equals("EOC")) {
                Terminal_panel.terminal_write("disconnessione dal client " + username + "\n", false);
                if (username != null) { //se è già registrato in Net_listener
                    if (Net_listener.is_paired(username)) {
                        Net_listener.unpair(username, paired_conn.get_username(), false);
                    }

                    Net_listener.logout(username, true);
                }
                this.stop();
            } else if (msg_str.substring(0, 5).equals("pair:")) { //tentativo di accoppiamento con un altro client
                String pair_name = msg_str.substring(5);

                pair_to(pair_name, conv_code);
            } else {
                throw new StringIndexOutOfBoundsException();
            }
        }
        catch (StringIndexOutOfBoundsException e) {
            Terminal_panel.terminal_write("il client " + username + " ha inviato [" + conv_code + "] " + msg_str + " messaggio non classificabile\n", true);
            conn.write(conv_code, new byte[] {0x00}); //risponde con 0x00 come messaggio di errore generale
        }
    }
}

class PairingResponse implements On_arrival {
    public byte req_convCode;
    public byte rec_convCode;
    public Connection rec_conn;
    public Connection req_conn;
    public String req_usr;

    public PairingResponse(byte req_convCode, byte rec_convCode, Connection rec_conn, Connection req_conn, String req_usr) {
        this.req_convCode = req_convCode;
        this.rec_convCode = rec_convCode;
        this.rec_conn = rec_conn;
        this.req_conn = req_conn;
        this.req_usr = req_usr;
    }

    @Override
    public void on_arrival(byte conv_code, byte[] msg, Receiver receiver) { //dovrà ricevere den se è stato rifiutato, acc se è stato accettato
        try {
            if (new String(msg).equals("acc")) {
                PairingResponse conferm_pairing = new PairingResponse(req_convCode, conv_code, rec_conn, req_conn, req_usr) {
                    @Override
                    public void on_arrival(byte conv_code, byte[] msg, Receiver receiver) {
                        try {
                            Net_listener.pair(req_usr, rec_conn.get_username());
                            rec_conn.write(rec_convCode, "acc".getBytes());

                            req_conn.pair(rec_conn);
                            rec_conn.pair(req_conn);

                            Net_listener.end_req(req_usr);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void timeout(byte conv_code, Receiver receiver) {
                        try {
                            rec_conn.write(rec_convCode, "den".getBytes());
                            Net_listener.end_req(req_usr);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
                req_conn.write(req_convCode, "acc".getBytes(), conferm_pairing);
            }
            else {
                Terminal_panel.terminal_write("il collegamento fra " + receiver.username + " e " + req_usr + " è stato rifiutato\n", false);
                req_conn.write(req_convCode, "den".getBytes());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void timeout(byte conv_code, Receiver receiver) { //il receiver non ha risposto in tempo, la connessione non è accettata
        try {
            req_conn.write(req_convCode, "den".getBytes());
            Net_listener.end_req(req_usr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}