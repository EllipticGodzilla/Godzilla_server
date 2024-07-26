package network;

public interface On_arrival {
    void on_arrival(byte conv_code, byte[] msg, Receiver receiver);
    void timeout(byte conv_code, Receiver receiver);
}