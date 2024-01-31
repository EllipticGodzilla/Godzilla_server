package network;

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

public class Connection {
    private BufferedOutputStream output;
    private BufferedInputStream input;
    private Receiver receiver;

    private AESCipher cipher = null;

    private boolean secure = false;
    private boolean dynamic = false;
    private SecureRandom random = new SecureRandom();

    public Connection(Socket sck) throws IOException {
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
    }

    public void direct_write(String msg) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException {
        direct_write(msg.getBytes());
    }

    public void direct_write(byte[] msg) throws IOException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        if (secure) { //è una connessione sicura cifra il messaggio
            msg = cipher.encode(msg);
        }

        output.write(new byte[]{(byte) (msg.length & 0xff), (byte) ((msg.length >> 8) & 0xff)}); //invia 2 byte che indicano la dimensione del messaggio
        output.write(msg); //invia il messaggio

        output.flush();
    }

    public String wait_for_string() { //se non è una connessione dinamica, attende che venga inviata una stringa
        if (!dynamic) {
            return new String(wait_for_bytes());
        }
        else {
            Terminal_panel.terminal_write("wait_for_string() può essere utilizzato solamente quando la connessione non è dinamica!\n", true);
            throw new RuntimeException("wait_for_string() is a only non-dynamic connection function!");
        }
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
            } catch (IOException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) { //connessione con il server chiusa
                throw new RuntimeException(e);
            }
        }
        else { //se la connessione è dinamica non si possono ricevere bytes in questo modo
            Terminal_panel.terminal_write("wait_for_bytes() può essere utilizzato solamente quando la connessione non è dinamica!\n", true);
            throw new RuntimeException("wait_for_bytes() is a only non-dynamic connection function!");
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
        this.input.close();
        this.output.close();
    }

    public void pair(Connection conn) {
        receiver.set_paired(conn);
    }

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

    private  Map<Byte, On_arrival> conv_map = new LinkedHashMap<>(); //memorizza tutte le conversazioni che ha aperto con il server

    private Connection paired_conn = null;
    private boolean secure = false;
    private AESCipher cipher;

    public Receiver(BufferedInputStream input, Connection c) {
        this.input = input;
        this.conn = c;
    }

    public void start() {
        reader.start();
    }

    public boolean new_conv(byte conv_code, On_arrival action) {
        return conv_map.putIfAbsent(conv_code, action) == null; // true se il conv_code non era già registrato, false se è un duplicato
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
                }
                else if (paired_conn != null) { //se ha già fatto il login, ed è appaiato ad un altro client
                    process_paired_msg(msg);
                }
                else { //se ha già fatto il login, e non è appaiato con nessun altro client
                    byte conv_code = msg[0]; //memorizza il codice della conversazione
                    msg = Arrays.copyOfRange(msg, 1, msg.length); //elimina il conv_code dal messaggio

                    On_arrival conv_action = conv_map.get(conv_code); //ricava l'azione da eseguire per questa conversazione
                    if (conv_action == null) { //se non è registrata nessuna azione processa il messaggio normalmente
                        process_server_msg(conv_code, msg);
                    }
                    else { //se è specificata un azione la esegue
                        conv_map.remove(conv_code);
                        conv_action.on_arrival(conv_code, msg, this);
                    }
                }
            }
        } catch (Exception e) {
            if (Net_listener.online(username)) {
                try {
                    process_server_msg((byte) 0x00, "EOC".getBytes()); //chiude la connessione
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    });

    //metodi per eseguire il login / registrarsi
    private void login_register(byte conv_code, byte[] msg) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException, InvocationTargetException, InstantiationException, IllegalAccessException { //esegue il login / registra
        String msg_str = new String(msg);

        if (msg_str.substring(0, 6).equals("login:")) { //vuole eseguire il login
            int pwd_off = msg_str.indexOf(";");
            String usr = msg_str.substring(6, pwd_off);
            byte[] pwd = Arrays.copyOfRange(msg, pwd_off + 1, msg.length);

            if (Net_listener.exist(usr, pwd) && !Net_listener.online(usr)) { //se le credenziali sono corrette e questo utente non è già online
                conn.write(conv_code, "log".getBytes());
                conn.set_username(usr);
                Net_listener.login(usr, conn);
            }
            else { //se il login fallisce
                conn.write(conv_code, "err".getBytes());
            }
        }
        else if (msg_str.substring(0, 9).equals("register:")) { //se vuole registrare un nuovo account
            int pwd_off = msg_str.indexOf(";");
            String usr = msg_str.substring(9, pwd_off);
            byte[] pwd = Arrays.copyOfRange(msg, pwd_off + 1, msg.length);

            if (Net_listener.register_account(usr, pwd)) { //riesce a creare il nuovo account
                conn.write(conv_code, "reg".getBytes());
                conn.set_username(usr);
                Net_listener.login(usr, conn);
            }
            else { //la registrazione è fallita
                conn.write(conv_code, "err".getBytes());
            }
        }
        else {
            Terminal_panel.terminal_write("un client ha inviato il messaggio (" + msg + ") senza prima eseguire il login/registrarsi\n", true);
        }
    }

    //metodi che fanno riferimento al collegamento fra client
    private void process_paired_msg(byte[] msg) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException, InvocationTargetException, InstantiationException, IllegalAccessException { //processa i messaggi che si inviano due client appaiati
        String str_msg = new String(Arrays.copyOfRange(msg, 1, msg.length));

        if (str_msg.equals("EOC")) { //se vuole chiudere la connessione con l'altro client
            unpair(paired_conn.get_username());
            paired_conn = null;
        }
        else { //altrimenti inoltra il messaggio al client a cui è appaiato
            Terminal_panel.terminal_write("il client " + username + " ha inviato a " + paired_conn.get_username() + " (" + str_msg + ")\n", false);
            paired_conn.direct_write(msg);
        }
    }

    private void pair_to(String name, byte conv_code) throws IllegalBlockSizeException, IOException, BadPaddingException, InterruptedException, InvalidAlgorithmParameterException, InvalidKeyException {
        pair_act.requester_convCode = conv_code;

        if (!Net_listener.request_pair(name, username, pair_act)) { //se il client a cui ci si vuole collegare è già appaiato
            conn.write(conv_code, "\000".getBytes());
        }
    }

    private PairingResponse pair_act = new PairingResponse() {
        @Override
        public void on_arrival(byte conv_code, byte[] msg, Receiver receiver) { //dovrà ricevere \000 se è stato riufiutato, \001 se è stato accettato
            try {
                if (msg[0] == 0) { //appaiamento rifiutato
                    Terminal_panel.terminal_write("il collegamento fra " + receiver.username + " e " + username + " è stato rifiutato\n", false);
                    conn.write(conv_code, "\000".getBytes());
                }
                else if (msg[0] == 1) { //appaiamento accettato
                    Net_listener.pair(username, receiver.username, requester_convCode, conv_code);
                }
                else { //qualcosa è andato storto, appaiamento rifiutato
                    conn.write(conv_code, "\000".getBytes());
                    Terminal_panel.terminal_write("è stato ricevuto un messaggio inatteso (" + new String(msg) + ") era atteso 0 o 1", true);
                    Terminal_panel.terminal_write("è stato ricevuto un messaggio inatteso (" + new String(msg) + ") era atteso 0 o 1", true);
                    throw new RuntimeException("risposta (" + new String(msg) + ") non valida, tentativo di appaiamento fallito");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    };

    public void set_paired(Connection conn) {
        this.paired_conn = conn;
    }

    protected void unpair(String name) throws IllegalBlockSizeException, IOException, BadPaddingException, InstantiationException, IllegalAccessException, InvalidAlgorithmParameterException, InvalidKeyException, InvocationTargetException {
        if (!name.equals("")) { //se è questo client a voler scollegarsi e non quello a cui è appaiato
            paired_conn.write("EOC");
            paired_conn.unpair();

            Net_listener.unpair(name, username, false);
        }
        paired_conn = null;
    }

    //processo dei messaggi client -> server

    private void process_server_msg(byte conv_code, byte[] msg) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException, InterruptedException { //processa i messaggi che invia il client al server
        String msg_str = new String(msg);

        if (msg_str.equals("EOC")) {
            Terminal_panel.terminal_write("disconnessione dal client " + username + "\n", false);
            if (this.username != null) { //se è già registrato in Net_listener
                Net_listener.logout(this.username, true);
            }
            this.stop();
        }
        else if (msg_str.substring(0, 5).equals("pair:")) { //tentativo di accoppiamento con un altro client
            String pair_name = msg_str.substring(5);

            pair_to(pair_name, conv_code);
        }
        else {
            Terminal_panel.terminal_write("il client " + username + " ha inviato (" + msg_str + ") messaggio non classificabile\n", true);
        }

    }
}

class PairingResponse implements On_arrival {
    public byte requester_convCode;

    @Override
    public void on_arrival(byte conv_code, byte[] msg, Receiver receiver) {}
}



















//package network;
//
//import gui.Terminal_panel;
//import javax.crypto.BadPaddingException;
//import javax.crypto.IllegalBlockSizeException;
//import java.io.BufferedInputStream;
//import java.io.BufferedOutputStream;
//import java.io.IOException;
//import java.lang.reflect.InvocationTargetException;
//import java.net.Socket;
//import java.security.InvalidAlgorithmParameterException;
//import java.security.InvalidKeyException;
//import java.security.NoSuchAlgorithmException;
//import java.util.Arrays;
//import java.util.Vector;
//
//public class Connection {
//    private BufferedOutputStream output;
//    private BufferedInputStream input;
//    private Receiver receiver;
//
//    private AESCipher cipher = null;
//
//    private boolean secure = false;
//    private boolean dinamic = false;
//
//    public Connection(Socket sck) throws IOException {
//        this.output = new BufferedOutputStream(sck.getOutputStream());
//        this.input = new BufferedInputStream(sck.getInputStream());
//        this.receiver = new Receiver(input, this);
//    }
//
//    public synchronized void write(String msg, boolean reply) throws IOException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
//        write(msg.getBytes(), reply);
//    }
//
//    public synchronized void write(String msg, boolean reply, On_arrival action) throws IllegalBlockSizeException, IOException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
//        write(msg.getBytes(), reply, action);
//    }
//
//    public synchronized void write(byte[] msg, boolean reply, On_arrival action) throws IllegalBlockSizeException, IOException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
//        if (receiver.wait_for_msg(action)) { //se riesce a impostare action come azione una volta ricevuta una risposta
//            write(msg, reply);
//        }
//        else { //se c'è già un azione per il prossimo messaggio ricevuto, aggiunge questo alla coda
//            receiver.add_to_queue(new CQueue_el(msg, reply, action));
//        }
//    }
//
//    public synchronized void write(byte[] msg, boolean reply) throws IOException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
//        byte[] msg_prefix = new byte[msg.length + 1];
//        msg_prefix[0] = (reply)? (byte) 1 : (byte) 0;
//        System.arraycopy(msg, 0, msg_prefix, 1, msg.length);
//
//        //se deve cifrare il messaggio
//        if (secure) {
//            msg_prefix = cipher.encode(msg_prefix);
//        }
//
//        output.write(new byte[] {(byte) (msg_prefix.length & 0xff), (byte) ((msg_prefix.length >> 8) & 0xff)}); //invia la lunghezza del messaggio
//        output.write(msg_prefix); //invia il messaggio
//
//        output.flush();
//    }
//
//    public byte[] wait_for_bytes() throws IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
//        if (!dinamic) {
//            try {
//                byte[] msg_size_byte = input.readNBytes(2);
//                int msg_len = (msg_size_byte[0] & 0Xff) | (msg_size_byte[1] << 8);
//
//                byte[] msg = input.readNBytes(msg_len);
//
//                if (secure) { //se deve decifrare il messaggio
//                    msg = cipher.decode(msg);
//                }
//
//                return Arrays.copyOfRange(msg, 1, msg.length); //rimuove il primo byte essendo un indicatore per quando il receiver è in modalità dinamica
//            } catch (IOException e) { //connessione con il server chiusa
//                throw new RuntimeException(e);
//            }
//        }
//        else {
//            throw new RuntimeException("wait_for_bytes() is a only non-dynamic connection function!");
//        }
//    }
//
//    public boolean set_cipher(AESCipher cipher) {
//        if (!secure) {
//            this.cipher = cipher;
//            this.receiver.set_cipher(cipher);
//
//            secure = true;
//
//            return true;
//        }
//        else {
//            return false;
//        }
//    }
//
//    public void start_dinamic() {
//        this.receiver.start();
//        this.dinamic = true;
//    }
//
//    public void set_username (String usr) {
//        this.receiver.set_username(usr);
//    }
//
//    public String get_username() {
//        return this.receiver.username;
//    }
//
//    public void pair(Connection conn) {
//        receiver.set_paired(conn);
//    }
//
//    public void unpair() throws IllegalBlockSizeException, IOException, BadPaddingException, InvocationTargetException, InstantiationException, IllegalAccessException, InvalidAlgorithmParameterException, InvalidKeyException {
//        receiver.unpair("");
//    }
//
//    public void close() throws IOException {
//        this.input.close();
//        this.output.close();
//    }
//}
//
//class Receiver {
//    private BufferedInputStream input;
//    private Connection conn;
//    public String username = null;
//
//    private  final int TIMER = 10; //dopo 10s che attende una risposta dal server ma non la riceve chiude la connessione
//
//    private  Vector<CQueue_el> queue = new Vector<>();
//
//    private Connection paired_conn = null;
//    private boolean secure = false;
//    private AESCipher cipher;
//
//    public Receiver(BufferedInputStream input, Connection c) {
//        this.input = input;
//        this.conn = c;
//    }
//
//    public void start() {
//        this.reader.start();
//    }
//
//    public void set_username(String usr) {
//        this.username = usr;
//    }
//
//    public boolean set_cipher(AESCipher cipher) {
//        if (!secure) {
//            this.cipher = cipher;
//            secure = true;
//
//            return true;
//        }
//        else {
//            return false;
//        }
//    }
//
//    public void stop() throws IOException {
//        conn.close();
//        reading = false;
//    }
//
//    protected boolean wait_for_msg(On_arrival arrival) {
//        if (this.arrival == null) { //se non sta attendendo già un messaggio
//            this.arrival = arrival;
//            new Thread(timer).start();
//
//            return true;
//        }
//        else {
//            return false;
//        }
//    }
//
//    public synchronized void add_to_queue(CQueue_el element) {
//        queue.add(element);
//    }
//
//    private void queue_next() throws IllegalBlockSizeException, IOException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException { //invia il prossimo messaggio nella queue
//        if (queue.size() != 0) { //se c'è almeno un messaggio in coda
//            CQueue_el el = queue.get(0);
//            queue.remove(0);
//
//            arrival = el.get_action();
//            conn.write(el.get_msg(), el.get_reply());
//        } else { //se la coda è vuota resetta arrival
//            arrival = null;
//        }
//    }
//
//    private boolean reading = true;
//    private Thread reader = new Thread(() -> {
//        try {
//            while (reading) {
//                byte[] msg_size_byte = input.readNBytes(2);
//                int msg_len = (msg_size_byte[0] & 0Xff) | (msg_size_byte[1] << 8);
//
//                byte[] msg = input.readNBytes(msg_len);
//                if (secure) {
//                    msg = cipher.decode(msg);
//                }
//
//                if (msg[0] == 0) { //il primo byte del messaggio = 0 indica che non è una risposta ma un nuovo comando dal server
//                    msg = Arrays.copyOfRange(msg, 1, msg.length);
//                    String str_msg = new String(msg);
//
//                    if (username != null) { //se ha già fatto il login processa qualsiasi messaggio sia arrivato
//                        process_server_msg(msg, str_msg);
//                    }
//                    else if (str_msg.substring(0, 6).equals("login:") || str_msg.substring(0, 9).equals("register:")) { //se non ha ancora fatto il login processa il messaggio solo se è un comando di login o register
//                        process_server_msg(msg, str_msg);
//                    }
//                    else { //se non ha ancora fatto il login e tenta di fare qualcosa che non sia login o register ignora il comando
//                        Terminal_panel.terminal_write("un utente ha tentato di eseguire (" + str_msg + ") prima di fare il login o di registrarsi\n", true);
//                    }
//                }
//                else { //il primo byte diverso da zero indica che è una risposta
//                    if (arrival != null) { //se qualcuno sta attendendo un messaggio dal server
//                        On_arrival c_arrival = arrival; //ricorda l'azione da eseguire per questo messaggio
//                        queue_next(); //fa partire il prossimo messaggio in queue
//
//                        c_arrival.on_arrival(Arrays.copyOfRange(msg, 1, msg.length), this);
//                    }
//                    else {
//                        Terminal_panel.terminal_write("è stata ricevuta una risposta dal client, ma non è specificata nessuna azione\n", true);
//                    }
//                }
//            }
//        } catch (Exception e) { //il client si è disconnesso
//            if (Net_listener.online(username)) {
//                try {
//                    process_server_msg("EOC".getBytes(), "EOC"); //chiude la connessione
//                } catch (Exception ex) {
//                    throw new RuntimeException(ex);
//                }
//            }
//        }
//    });
//
//    private Runnable timer = () -> {
//        try {
//            String arriva_pos = arrival.toString();
//
//            Thread.sleep(TIMER * 1000);
//
//            if (arrival != null && arriva_pos.equals(arrival.toString())) { //se non è cambiato l'oggetto arrival, è sempre la stessa richiesta
//                Terminal_panel.terminal_write("l'attesa di una risposta dal client " + username + " ha superato il limite\n", true);
//
//                arrival.timedout(this);
//                arrival = null;
//            }
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    };
//
//    private void process_server_msg(byte[] msg, String msg_str) throws IllegalBlockSizeException, IOException, BadPaddingException, InterruptedException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
//
//        if (msg_str.equals("EOC") && paired_conn == null) { //chiude la connessione con il client
//            Terminal_panel.terminal_write("disconnessione dal client " + username + "\n", false);
//            if (this.username != null) { //se è già registrato in Net_listener
//                Net_listener.logout(this.username, true);
//            }
//            this.stop();
//        }
//        else if (msg_str.equals("EOC") && paired_conn != null) { //si scollega dal client a cui era accoppiato
//            unpair(paired_conn.get_username());
//            paired_conn = null;
//        }
//        else if (paired_conn == null) { //se è un messaggio per il server
//            internal_msg(msg, msg_str);
//        }
//        else if (paired_conn != null && msg_str.charAt(0) == 'r') { //se è una risposta per il client a cui è accoppiato
//            Terminal_panel.terminal_write("il client " + username + " ha inviato a " + paired_conn.get_username() + " (" + msg_str + ")\n", false);
//            paired_conn.write(Arrays.copyOfRange(msg, 1, msg.length), true);
//        }
//        else if (paired_conn != null && msg_str.charAt(0) == 'n') { //se è un nuovo messaggio e non una risposta per il client a cui è accoppiato
//            Terminal_panel.terminal_write("il client " + username + " ha inviato a " + paired_conn.get_username() + " (" + msg_str + ")\n", false);
//            paired_conn.write(Arrays.copyOfRange(msg, 1, msg.length), false);
//        }
//        else {
//            Terminal_panel.terminal_write("il client " + username + " ha inviato (" + msg_str + ") messaggio non classificabile\n", true);
//        }
//    }
//
//    private void internal_msg(byte[] msg, String msg_str) throws IllegalBlockSizeException, IOException, BadPaddingException, InterruptedException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException { //gestisce tutte le richieste del client
//        if (msg_str.substring(0, 5).equals("pair:")) { //tentativo di accoppiamento con un altro client
//            String pair_name = msg_str.substring(5);
//
//            pair_to(pair_name);
//        }
//        else if (msg_str.substring(0, 6).equals("login:")) { //tentativo di login
//            int psw_off = msg_str.indexOf(";");
//            String usr = msg_str.substring(6, psw_off);
//            byte[] psw = Arrays.copyOfRange(msg, psw_off + 1, msg.length);
//
//            login(usr, psw);
//        }
//        else if (msg_str.substring(0, 9).equals("register:")) { //tentativo di registrazione
//            int psw_off = msg_str.indexOf(";");
//            String usr = msg_str.substring(9, psw_off);
//            byte[] psw = Arrays.copyOfRange(msg, psw_off + 1, msg.length);
//
//            register(usr, psw);
//        }
//        else {
//            Terminal_panel.terminal_write("il client " + username + " ha inviato (" + msg_str + ") messaggio non classificabile\n", true);
//        }
//    }
//
//    private void login(String usr, byte[] psw) throws IllegalBlockSizeException, IOException, BadPaddingException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
//        if (Net_listener.exist(usr, psw) && !Net_listener.online(usr)) { //se le credenziali sono corrette, e questo utente non è già online
//            conn.write("log", true);
//
//            conn.set_username(usr);
//            Net_listener.login(usr, conn);
//        } else {
//            conn.write("err", true);
//        }
//    }
//
//    private void register(String usr, byte[] psw) throws IllegalBlockSizeException, IOException, BadPaddingException, InvocationTargetException, InstantiationException, IllegalAccessException, InvalidAlgorithmParameterException, InvalidKeyException {
//        if (Net_listener.register_account(usr, psw)) { //se riesce a creare l'utente con successo:
//            conn.write("reg", true);
//
//            conn.set_username(usr);
//            Net_listener.login(usr, conn);
//        }
//        else { //se esiste già un utente con questo nome
//            conn.write("err", true);
//        }
//    }
//
//    public void set_paired(Connection conn) {
//        this.paired_conn = conn;
//    }
//
//    private void pair_to(String name) throws IllegalBlockSizeException, IOException, BadPaddingException, InterruptedException, InvalidAlgorithmParameterException, InvalidKeyException {
//        if (!Net_listener.request_pair(name, username, pair_act)) { //se il client a cui ci si vuole collegare è già appaiato, o rifiuta
//            conn.write("\000", true);
//        }
//    }
//
//    private On_arrival pair_act = new On_arrival() {
//        @Override
//        public void on_arrival(byte[] msg, Receiver receiver) { //dovrà ricevere \000 se è stato riufiutato, \001 se è stato accettato
//            try {
//                if (msg[0] == 0) { //appaiamento rifiutato
//                    Terminal_panel.terminal_write("il collegamento fra " + receiver.username + " e " + username + " è stato rifiutato\n", false);
//                    conn.write("\000", true);
//                } else if (msg[0] == 1) { //appaiamento accettato
//                    Net_listener.pair(username, receiver.username);
//                } else {
//                    timedout(receiver); //qualcosa è andato storto, appaiamento rifiutato
//                    Terminal_panel.terminal_write("è stato ricevuto un messaggio inatteso (" + new String(msg) + ") era atteso 0 o 1", true);
//                    throw new RuntimeException("risposta (" + new String(msg) + ") non valida, tentativo di appaiamento fallito");
//                }
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        @Override
//        public void timedout(Receiver receiver) {
//            try {
//                Terminal_panel.terminal_write("il collegamento fra " + receiver.username + " e " + username + " è stato rifiutato\n", false);
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }
//    };
//
//    protected void unpair(String name) throws IllegalBlockSizeException, IOException, BadPaddingException, InvocationTargetException, InstantiationException, IllegalAccessException, InvalidAlgorithmParameterException, InvalidKeyException {
//        if (!name.equals("")) { //se è questo client a voler scollegarsi e non quello a cui è appaiato
//            paired_conn.write("EOC", false);
//            paired_conn.unpair();
//
//            Net_listener.unpair(name, username, false);
//        }
//        paired_conn = null;
//    }
//}