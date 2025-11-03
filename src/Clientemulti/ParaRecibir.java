package Clientemulti;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public class ParaRecibir implements Runnable {
    private final DataInputStream entrada;
    private final Socket socket;

    public ParaRecibir(Socket s) throws IOException {
        this.socket = s;
        entrada = new DataInputStream(s.getInputStream());
    }

    @Override
    public void run() {
        try {
            recibirBucle();
        } catch (EOFException e) {
            System.out.println("\n[INFO] El servidor cerró la conexión (EOF).");
        } catch (IOException e) {
            System.out.println("\n[INFO] Conexión perdida con el servidor.");
        } finally {
            cerrarSocketSilenciosamente();
        }
    }

    private void recibirBucle() throws IOException {
        String mensaje;
        while (!socket.isClosed()) {
            mensaje = entrada.readUTF();
            imprimirMensaje(mensaje);
        }
    }

    private void imprimirMensaje(String mensaje) {
        System.out.print("\r" + mensaje + "        \n");
        System.out.print("Tú: ");
    }

    private void cerrarSocketSilenciosamente() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }
}