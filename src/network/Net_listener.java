package network;

import gui.Clients_panel;
import gui.Terminal_panel;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.BindException;
import java.net.ServerSocket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.regex.Pattern;

public abstract class Net_listener {
    public static byte[] certificate;
    public static byte[] server_info;
    public static Cipher decoder;

    private static Map<String, Connection> connected_client = new LinkedHashMap<>();
    private static Map<String, Boolean> paired_client = new LinkedHashMap<>();
    private static Map<String, byte[]> users_credentials = new LinkedHashMap<>();
    private static MessageDigest sha3_digest;

    private static int num_pair = 0;

    private static ServerSocket ss = null;
    private static final int PORT = 31415;
    public static void start() {
        try {
            if (ss == null) {
                sha3_digest = MessageDigest.getInstance("sha3-256");

                init_ce_server_info();
                init_users_credentialis();
            }
            ss = new ServerSocket(PORT);

            new Thread(() -> {
                try {
                    while (true) {
                        new Thread(new Client(ss.accept())).start(); //attende il collegamento di un client
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
                ss.close(); //chiude il socket

                Clients_panel.clean_list();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void logout(String usr, boolean send_list) throws IllegalBlockSizeException, IOException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        connected_client.remove(usr);
        Clients_panel.remove_client(usr);
        paired_client.remove(usr);

        if (send_list) { //invia a tutti i client la lista aggiornata
            String[] clients = connected_client.keySet().toArray(new String[0]);

            for (Connection conn : connected_client.values()) {
                send_clients_update(clients, conn);
            }
        }

        Terminal_panel.terminal_write("il client " + usr + " si è scollegato\n", false);
    }

    public static boolean register_account(String username, byte[] psw_hash) {
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

    public static boolean online(String usr) {
        return connected_client.containsKey(usr);
    }

    public static void login(String usr, Connection c) throws IllegalBlockSizeException, IOException, BadPaddingException, InvocationTargetException, InstantiationException, IllegalAccessException, InvalidAlgorithmParameterException, InvalidKeyException {
        connected_client.put(usr, c); //aggiunge il client alla lista dei client connessi
        Clients_panel.add_client(usr);
        paired_client.put(usr, false); //specifica che questo client non è ancora appaiato
        String[] clients = connected_client.keySet().toArray(new String[0]);

        //invia a tutti i client la lista aggiornata
        for (Connection conn : connected_client.values()) {
            send_clients_update(clients, conn);
        }

        Terminal_panel.terminal_write("il client " + usr + " ha appena eseguito il login\n", false);
    }

    public static synchronized int paired_num() {
        return num_pair;
    }

    //se il client con username = pair_usr non è appaiato con nessuno chiede se vuole essere appaiato ad "usr"
    public static synchronized boolean request_pair(String pair_usr, String usr, On_arrival act) throws IllegalBlockSizeException, IOException, BadPaddingException, InterruptedException, InvalidAlgorithmParameterException, InvalidKeyException {
        Terminal_panel.terminal_write("il client " + pair_usr + " ha richiesto di collegarsi con il client " + usr + "\n", false);
        if (!paired_client.get(pair_usr)) { //se il client usr non è ancora appaiato a nessuno
            Connection conn = connected_client.get(pair_usr);

            //chiede al client pair_usr se vuole essere appaiato con usr
            conn.write("pair:" + usr, false, act);

            return true;
        }
        else { //se è già appaiato con qualcuno
            Terminal_panel.terminal_write("collegamento rifiutato, il client " + usr + " è già collegato ad un altro\n", false);
            return false;
        }
    }

    public static synchronized void pair(String usr1, String usr2) throws InvocationTargetException, InstantiationException, IllegalAccessException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException { //imposta questi due client come appaiati
        Clients_panel.pair_clients(usr1, usr2);

        paired_client.replace(usr1, true);
        paired_client.replace(usr2, true);

        Connection conn1 = connected_client.get(usr1);
        Connection conn2 = connected_client.get(usr2);

        conn1.pair(conn2);
        conn2.pair(conn1);

        //avvisa i due client che l'appaiamento è andato a buon fine
        conn2.write("\001", true);
        conn1.write("\001", true);

        num_pair += 1;
    }

    public static synchronized void unpair(String usr1, String usr2, boolean notify_clients) throws InvocationTargetException, InstantiationException, IllegalAccessException, IllegalBlockSizeException, IOException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException { //imposta questi due client come non appaiati
        Clients_panel.unpair_clients(usr1, usr2);

        paired_client.replace(usr1, false);
        paired_client.replace(usr2, false);

        if (notify_clients) { //se deve avvisare i client che sono stati scollegati
            connected_client.get(usr1).write("EOC", false);
            connected_client.get(usr2).write("EOC", false);
        }

        num_pair -= 1;

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

    public static void save_credentials() throws IOException {
        String file_credentials = "";
        String[] usernames = users_credentials.keySet().toArray(new String[0]);
        for (String username : usernames) {
            file_credentials += username + ";" + Base64.getEncoder().encodeToString(users_credentials.get(username)) + "\n";
        }

        if (!file_credentials.equals("")) { //se sono state trovate delle credenziali da salvare
            FileOutputStream client_cred = new FileOutputStream(Net_listener.class.getResource("/clients_credentials.dat").getPath());
            client_cred.write(file_credentials.getBytes());

            client_cred.close();
        }
    }

    public static void disconnect(String name, boolean send_list) throws IllegalBlockSizeException, IOException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        Connection conn = connected_client.get(name);

        conn.write("EOC", false); //notifica il client che chiude la connessione

        Terminal_panel.terminal_write("disconnessione dal client " + conn.get_username() + "\n", false);
        if (conn.get_username() != null) { //se è già registrato in Net_listener
            Net_listener.logout(conn.get_username(), send_list);
        }

        conn.close();
    }

    private static void disconnect() throws IllegalBlockSizeException, IOException, BadPaddingException, InvocationTargetException, InstantiationException, IllegalAccessException, InvalidAlgorithmParameterException, InvalidKeyException {
        String[] clients = connected_client.keySet().toArray(new String[0]);

        for (String name : clients) { //chiude le connessioni con tutti i client
            disconnect(name, false);
        }
    }

    private static void init_ce_server_info() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidKeyException {
        Terminal_panel.terminal_write("inizializzando le informazioni per il certificato\n", false);

        FileInputStream cert_input = new FileInputStream(Net_listener.class.getResource("/certificate.dat").getPath());
        FileInputStream info_input = new FileInputStream(Net_listener.class.getResource("/server_info.dat").getPath());
        FileInputStream prv_input = new FileInputStream(Net_listener.class.getResource("/private.key").getPath());

        //salva certificato e server info
        certificate = Base64.getDecoder().decode(cert_input.readAllBytes());
        server_info = info_input.readAllBytes();

        cert_input.close();
        info_input.close();

        //inzializza il cipher per decifrare utilizzando la chiave privata del server
        KeyFactory key_f = KeyFactory.getInstance("RSA");

        decoder = Cipher.getInstance("RSA");
        decoder.init(Cipher.DECRYPT_MODE, key_f.generatePrivate(new PKCS8EncodedKeySpec(prv_input.readAllBytes())));

        prv_input.close();
    }

    private static void init_users_credentialis() throws IOException {
        Terminal_panel.terminal_write("inizializzando le informazioni per le credenziali dei client\n", false);
        FileInputStream cred_input = new FileInputStream(Net_listener.class.getResource("/clients_credentials.dat").getPath());
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
        if (credentials.length != 1) { //se il file non è vuoto
            for (int i = 0; i < credentials.length - 1; i += 2) {
                users_credentials.put(credentials[i], Base64.getDecoder().decode(credentials[i+1]));
            }
        }
    }

    private static void send_clients_update(String[] c_list, Connection conn) throws IllegalBlockSizeException, IOException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException { //invia al client conn un update dei client connessi al server
        String msg = "clList:";
        for (String name : c_list) {
            if (!name.equals(conn.get_username())) {
                msg += name + ";";
            }
        }

        if (msg.length() != 7) {
            conn.write(msg.substring(0, msg.length() - 1), false);
        }
        else {
            conn.write(msg.substring(0, msg.length()), false);
        }
    }

    private static byte[] sha3(byte[] msg) {
        return sha3_digest.digest(msg);
    }
}
