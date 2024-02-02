import gui.Server_frame;
import gui.TempPanel;
import gui.TempPanel_info;
import gui.Terminal_panel;
import network.Net_listener;

import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Vector;

public class Godzilla_server {
    public static void main(String[] args) throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        Vector<Image> icons = new Vector<>();

        icons.add(new ImageIcon(Server_frame.project_path + "/images/icon_16.png").getImage());
        icons.add(new ImageIcon(Server_frame.project_path + "/images/icon_32.png").getImage());
        icons.add(new ImageIcon(Server_frame.project_path + "/images/icon_64.png").getImage());
        icons.add(new ImageIcon(Server_frame.project_path + "/images/icon_128.png").getImage());

        Net_listener.init();
        Server_frame.init().setIconImages(icons);
        Runtime.getRuntime().addShutdownHook(new Thread(shutdown_hook));
    }

    private static Runnable shutdown_hook = new Runnable() {
        @Override
        public void run() {
            try {
                Net_listener.stop();

                Net_listener.save_credentials();
                save_terminal_log();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void save_terminal_log() throws IOException {
            FileOutputStream file_writer = new FileOutputStream(Server_frame.project_path + "/database/log.dat");
            file_writer.write(Terminal_panel.get_terminal_log().getBytes());

            file_writer.close();
        }
    };
}
