package Clientemulti;
import java.io.IOException;
import java.net.Socket;

public class ClienteMulti {
    private static final String HOST = "localhost";
    private static final int PUERTO = 8080;

    public static void main(String[] args) {
        Socket s = null;
        try {
            s = new Socket(HOST, PUERTO);
            System.out.println("Conectado al servidor. Escribe /exit para salir.");
            iniciarHilosComunicacion(s);
        } catch (IOException e) {
            System.err.println("Error de conexión al servidor. Asegúrate de que el servidor está activo.");
            cerrarSocketSilenciosamente(s);
        }
    }

    private static void iniciarHilosComunicacion(Socket s) throws IOException {
        ParaMandar paraMandar = new ParaMandar(s);
        new Thread(paraMandar).start();

        ParaRecibir paraRecibir = new ParaRecibir(s);
        new Thread(paraRecibir).start();
    }

    private static void cerrarSocketSilenciosamente(Socket s) {
        try {
            if (s != null && !s.isClosed()) {
                s.close();
            }
        } catch (IOException ignored) {}
    }
}