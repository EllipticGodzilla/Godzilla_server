package network;

import gui.*;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.regex.Pattern;

public abstract class Net_listener {
    private static boolean ready = false;
    private static boolean client_ck = false;
    private static final int CLIENT_CHECK_TIMER = 60000;

    public static byte[] certificate;
    public static byte[] server_info;
    public static Cipher decoder;

    private static Map<String, Connection> connected_client = new LinkedHashMap<>(); //mappa fra nome e connessione di tutti i client attivi
    private static Map<String, Boolean> paired_client = new LinkedHashMap<>(); //mappa fra nome e bool di tutti i client attivi, true se sono appaiati e false se non lo sono
    private static Vector<String> pair_req_client = new Vector<>(); //vettore con tutti i client che stanno richiedendo a qualcun'altro di collegarsi (non possono ricevere richieste di collegamento)
    private static Map<String, byte[]> users_credentials = new LinkedHashMap<>(); //mappa fra nome e hash della password di tutti i client registrati nel server
    private static MessageDigest sha3_digest;

    private static ServerSocket ss = null;
    private static final int PORT = 31415;

    public static void init() throws NoSuchAlgorithmException, NoSuchPaddingException, IOException, InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        sha3_digest = MessageDigest.getInstance("sha3-256");

        init_ce_server_info();
        init_users_credentialis();
    }

    public static void set_ready() {
        ready = true;
    }
    public static boolean is_ready() {
        return ready;
    }
    public static boolean is_paired(String usr) { return paired_client.get(usr); }

    public static void start() {
        try {
            ss = new ServerSocket(PORT);

            new Thread(() -> {
                try {
                    while (true) {
                        new Thread(new Client(ss.accept())).start(); //attende il collegamento di un client

                        if (!client_ck) { //se non sta già controllando che tutti i client siano effettivamente connessi

                        }
                    }
                } catch (IOException e) {
                    //il socket è stato chiuso
                }
            }).start();
        } catch (BindException e) {
            Terminal_panel.terminal_write("la porta standard " + PORT + " è occupata, impossibile inizializzare il server\n", true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void stop() {
        if (ss != null && !ss.isClosed()) { //se il server è attivo
            try {
                disconnect(); //disconnette tutti i client
            } catch (Exception e) {} //se trova un broken pipe significa che un client si è disconnesso ma non è stato trovato, lo ignora

            //resetta tutto
            connected_client = new LinkedHashMap<>();
            paired_client = new LinkedHashMap<>();
            pair_req_client = new Vector<>();

            try {
                ss.close(); //chiude il socket
            } catch (IOException e) {throw new RuntimeException(e);}
        }
    }

    public synchronized static void logout(String usr, boolean send_list) throws IllegalBlockSizeException, IOException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        connected_client.remove(usr);
        Clients_panel.offline_client(usr);
        paired_client.remove(usr);

        if (send_list) { //invia a tutti i client la lista aggiornata
            String[] clients = connected_client.keySet().toArray(new String[0]);

            for (Connection conn : connected_client.values()) {
                try {
                    send_clients_update(clients, conn);
                } catch (SocketException e) {} //se il client si è scollegato ma non è ancora stato rimosso dalla lista semplicemente lo ignora
            }
        }

        Terminal_panel.terminal_write("il client " + usr + " si è scollegato\n", false);
    }

    public synchronized static boolean register_account(String username, byte[] psw_hash) {
        if (!users_credentials.containsKey(username)) { //se non esiste un altro utente con questo username
            psw_hash = sha3(psw_hash); //calcola l'hash della password e memorizza questo

            users_credentials.put(username, psw_hash);
            Terminal_panel.terminal_write("un nuovo account è stato creato: " + username + "\n", false);

            return true;
        }
        else {
            return false;
        }

    }

    public static boolean is_online(String usr) {
        return connected_client.containsKey(usr);
    }

    public synchronized static void login(String usr, Connection c) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException {
        Clients_panel.login_client(usr);
        new_client(usr, c);
    }

    public synchronized static void register(String usr, Connection c) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException {
        Clients_panel.register_client(usr);
        new_client(usr, c);
    }

    public static boolean change_psw(String usr, byte[] new_psw) {
        return users_credentials.replace(usr, new_psw) != null;
    }

    //si è connesso e registrato / fatto il login un nuovo utente
    private static void new_client(String usr, Connection c) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException {
        connected_client.put(usr, c); //aggiunge il client alla lista dei client connessi
        paired_client.put(usr, false); //specifica che questo client non è ancora appaiato
        String[] clients = connected_client.keySet().toArray(new String[0]);

        //invia a tutti i client la lista aggiornata
        for (Connection conn : connected_client.values()) {
            send_clients_update(clients, conn);
        }

        if (!client_ck) { //se non sta controllando le connessioni dei client
            new Thread(client_checking).start();
        }
    }

    public static synchronized boolean request_pair(String pair_usr, String req_usr, PairingResponse act) throws IllegalBlockSizeException, IOException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        Terminal_panel.terminal_write("il client " + req_usr + " ha richiesto di collegarsi con il client " + pair_usr + "\n", false);
        if (!paired_client.get(pair_usr) && !pair_req_client.contains(pair_usr)) { //se il client usr non è ancora appaiato a nessuno e non sta richiedendo a nessuno di appaiarsi
            pair_req_client.add(req_usr); //aggiunge il req client alla lista di client che stanno richiedendo un collegamento

            Connection conn = connected_client.get(pair_usr);
            act.rec_conn = conn; //memorizza la connessione con il ricevitore

            //chiede al ricevitore se vuole essere appaiato con req_usr
            conn.write(("pair:" + req_usr).getBytes(), act);

            return true;
        }
        else { //se è già appaiato con qualcuno
            Terminal_panel.terminal_write("collegamento rifiutato, il client " + req_usr + " è già collegato ad un altro\n", false);
            return false;
        }
    }

    public static void end_req(String usr) { //il client usr ha finito la richiesta di collegamento
        pair_req_client.remove(usr);
    }

    public static synchronized void pair(String usr1, String usr2) { //imposta questi due client come appaiati
        Clients_panel.pair_clients(usr1, usr2);

        paired_client.replace(usr1, true);
        paired_client.replace(usr2, true);
    }

    public static synchronized void unpair(String usr1, String usr2, boolean notify_clients) throws IllegalBlockSizeException, IOException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, InvocationTargetException, InstantiationException, IllegalAccessException { //imposta questi due client come non appaiati
        Clients_panel.unpair_clients(usr1, usr2);

        paired_client.replace(usr1, false);
        paired_client.replace(usr2, false);

        Connection usr1_conn = connected_client.get(usr1),
                   usr2_conn = connected_client.get(usr2);

        usr1_conn.unpair();
        usr2_conn.unpair();

        if (notify_clients) { //se deve avvisare i client che sono stati scollegati
            usr1_conn.write("EOP");
            usr2_conn.write("EOP");
        }

        Terminal_panel.terminal_write("i client " + usr1 + " - " + usr2 + " si scollegano\n", false);
    }

    public static boolean exist(String username, byte[] psw_hash) {
        psw_hash = sha3(psw_hash);
        int diff = Arrays.compare(psw_hash, users_credentials.get(username));

        if (diff != 0) {
            Terminal_panel.terminal_write("tentativo di login all'utente " + username + " fallito, " + ((users_credentials.containsKey(username))? "password sbagliata" : "l'utente non esiste") + "\n", true);
        }

        return diff == 0;
    }

    public static boolean forget(String usr) {
        return users_credentials.remove(usr) != null;
    }

    public static void save_credentials() throws IOException {
        String file_credentials = "";
        String[] usernames = users_credentials.keySet().toArray(new String[0]);
        for (String username : usernames) {
            file_credentials += username + ";" + Base64.getEncoder().encodeToString(users_credentials.get(username)) + "\n";
        }

        if (!file_credentials.equals("")) { //se sono state trovate delle credenziali da salvare
            FileOutputStream client_cred = new FileOutputStream(Server_frame.project_path + "/database/clients_credentials.dat");
            client_cred.write(file_credentials.getBytes());

            client_cred.close();
        }
    }

    public static void disconnect(String name, boolean send_list) throws IllegalBlockSizeException, IOException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        Connection conn = connected_client.get(name);

        conn.write("EOC"); //notifica il client che chiude la connessione

        Terminal_panel.terminal_write("disconnessione dal client " + conn.get_username() + "\n", false);
        if (conn.get_username() != null) { //se è già registrato in Net_listener
            Net_listener.logout(conn.get_username(), send_list);
        }

        conn.close();
    }

    private static void disconnect() throws IllegalBlockSizeException, IOException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        String[] clients = connected_client.keySet().toArray(new String[0]);
        Clients_panel.offline(clients); //tutti le caselle nella lista diventano offline

        for (String name : clients) { //chiude le connessioni con tutti i client
            if (paired_client.get(name)) { //se questo client è appaiato
                connected_client.get(name).write((byte) 0x00, "EOC".getBytes()); //scollega questo client
            }

            disconnect(name, false);
        }
    }

    private static void init_ce_server_info() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        //legge e decodifica le informazioni relative al server
        server_info = Server_frame.file_decoder.doFinal(Net_listener.class.getClassLoader().getResourceAsStream("files/server_info.dat").readAllBytes());
        byte[] prv_key = Server_frame.file_decoder.doFinal(Net_listener.class.getClassLoader().getResourceAsStream("files/private.key").readAllBytes()); //dovresti cifrare questo file come nel client

        //inzializza il cipher per decifrare utilizzando la chiave privata del server
        KeyFactory key_f = KeyFactory.getInstance("RSA");

        decoder = Cipher.getInstance("RSA");
        decoder.init(Cipher.DECRYPT_MODE, key_f.generatePrivate(new PKCS8EncodedKeySpec(prv_key)));

        //tenta di leggere il certificato del server, se non lo ha memorizzato lo richiede all'utente
        if (new File(Server_frame.project_path + "/database/certificate.dat").exists()) {
            byte[] ce_byte = new FileInputStream(Server_frame.project_path + "/database/certificate.dat").readAllBytes();

            if (ce_byte.length > 0) { //se è già stato registrato ad un dns
                Net_listener.certificate = Server_frame.file_decoder.doFinal(ce_byte);
            }
            else {
                start_ce_loop(false);
            }
        }
        else {
            start_ce_loop(true);
        }
    }

    private static void start_ce_loop(boolean mk_file) throws IOException {
        Pattern comma_sep = Pattern.compile(";");
        String[] info = comma_sep.split(new String(server_info));

        Terminal_panel.terminal_write("non è ancora stato inserito un certificato valido\n", true);
        Terminal_panel.terminal_write(
                "le informazioni con cui si deve registrare il server sono:\n" +
                        "   - nome: " + info[0] + "\n" +
                        "   - ip: " + info[1] + "\n" +
                        "   - link: " + info[2] + "\n" +
                        "   - public key: " + info[3] + "\n" +
                        "   - mail: " + info[4] + "\n"
                , false
        );

        if (mk_file) {
            new File(Server_frame.project_path + "/database/certificate.dat").createNewFile(); //crea il file vuoto
        }
        ask_ce.success(); //richiede il certificato
    }

    private static TempPanel_action setup_ce = new TempPanel_action() {
        @Override
        public void success() {
            try {
                byte[] ce_byte = Base64.getDecoder().decode(input.elementAt(0));

                if (ce_byte.length > 0) { //è stato inserito effettivamente un certificato
                    Net_listener.certificate = ce_byte; //memorizza il certificato

                    //cifra e salva il certificato nel file
                    FileOutputStream ce_file_os = new FileOutputStream(Server_frame.project_path + "/database/certificate.dat");
                    ce_file_os.write(Server_frame.file_encoder.doFinal(ce_byte));
                    ce_file_os.close();

                    //rende il file read-only
                    if (new File(Server_frame.project_path + "/database/certificate.dat").setReadOnly()) {
                        TempPanel.show(new TempPanel_info(
                                TempPanel_info.SINGLE_MSG,
                                false,
                                "certificato registrato con successo, file in read-only"
                        ), null);
                    }
                    else {
                        TempPanel.show(new TempPanel_info(
                                TempPanel_info.SINGLE_MSG,
                                false,
                                "certificato registrato con successo, impossibile impostare il file in read-only"
                        ), null);
                    }
                }
                else {
                    fail();
                }
            } catch (Exception e) {
                fail();
            }
        }

        @Override
        public void fail() {
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.SINGLE_MSG,
                    false,
                    "è stato riscontrato un errore durante il setup del certificato, riprova"
            ), ask_ce);
        }
    };

    private static TempPanel_action ask_ce = new TempPanel_action() {
        @Override
        public void success() {
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.INPUT_REQ,
                    false,
                    "inserisci il certificato:"
            ), setup_ce);
        }

        @Override
        public void fail() {} //non è possibile premere annulla
    };

    private static void init_users_credentialis() throws IOException {
        FileInputStream cred_input = new FileInputStream(Server_frame.project_path + "/database/clients_credentials.dat");
        String file_txt = new String(cred_input.readAllBytes());
        cred_input.close();

        Pattern pat = Pattern.compile("[;\n]");
        /*
        * in server_info.dat sono contenuti tutti gli account creati su questo server come:
        * <username1>;<password hash1>
        * <username2>;<password hash2>
        * ...
        * ...
        *
        * viene diviso il testo in corrispondenza di ";" o "\n" in modo da trovare un array:
        * {<username1>, <password hash1>, <username2>, <password hash2>, ...}
         */

        String[] credentials = pat.split(file_txt);
        String[] usernames = new String[credentials.length / 2]; //tutti gli username degli utenti registrati
        if (credentials.length != 1) { //se il file non è vuoto
            for (int i = 0; i < credentials.length - 1; i += 2) {
                users_credentials.put(credentials[i], Base64.getDecoder().decode(credentials[i+1]));
                usernames[i/2] = credentials[i];
            }
        }

        Clients_panel.init_list(usernames); //inizializza la lista dei client con tutti gli utenti registrati
    }

    private static void send_clients_update(String[] c_list, Connection conn) throws IllegalBlockSizeException, IOException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException { //invia al client conn un update dei client connessi al server
        String msg = "clList:";
        for (String name : c_list) {
            if (!name.equals(conn.get_username())) {
                msg += name + ";";
            }
        }

        if (msg.length() != 7) {
            conn.write(msg.substring(0, msg.length() - 1));
        }
        else {
            conn.write(msg.substring(0, msg.length()));
        }
    }

    public static byte[] sha3(byte[] msg) {
        return sha3_digest.digest(msg);
    }

    private static Runnable client_checking = new Runnable() {
        @Override
        public void run() {
            client_ck = true;

            while (connected_client.size() != 0) {
                try {
                    Thread.sleep(CLIENT_CHECK_TIMER); //ogni minuto controlla

                    for (Connection c : connected_client.values()) {
                        try {
                            c.write("con_ck".getBytes(), new On_arrival() {
                                @Override
                                public void on_arrival(byte conv_code, byte[] msg, Receiver receiver) {} //se viene risposto la connessione va bene

                                @Override
                                public void timeout(byte conv_code, Receiver receiver) { //se non riceve nulla sconnette il client
                                    try {
                                        disconnect(c);
                                    } catch (Exception e) {}
                                }
                            });
                        } catch (Exception e) { //se ha dei problemi sconnette questo client
                            disconnect(c);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            client_ck = false;
        }

        private void disconnect(Connection c) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidKeyException, InvocationTargetException, InstantiationException, IllegalAccessException {
            if (c.get_paired() != null) {
                unpair(c.get_username(), c.get_paired(), true);
            }

            logout(c.get_username(), true);
            c.close();
        }
    };
}
