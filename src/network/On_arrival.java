package network;

import java.io.IOException;

public interface On_arrival {
    void on_arrival(byte conv_code, byte[] msg, Receiver receiver);
}