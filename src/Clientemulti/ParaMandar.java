package Clientemulti;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ParaMandar implements Runnable {
    private final BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
    private final DataOutputStream salida;
    private final Socket socket;

    public ParaMandar(Socket s) throws IOException {
        this.socket = s;
        this.salida = new DataOutputStream(s.getOutputStream());
    }

    @Override
    public void run() {
        try {
            enviarBucle();
        } catch (IOException e) {
            System.out.println("\n[INFO] Desconectado: Error de envío o socket cerrado.");
        } finally {
            cerrarSocketSilenciosamente();
        }
    }

    private void enviarBucle() throws IOException {
        String mensaje;
        while (!socket.isClosed()) {
            System.out.print("Tú: ");
            mensaje = teclado.readLine();

            if (mensaje == null || mensaje.equalsIgnoreCase("/exit")) {
                break;
            }
            enviarMensaje(mensaje);
        }
    }

    private void enviarMensaje(String mensaje) throws IOException {
        if (mensaje != null) {
            salida.writeUTF(mensaje);
        }
    }

    private void cerrarSocketSilenciosamente() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }
}