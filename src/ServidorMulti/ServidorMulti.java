package ServidorMulti;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorMulti {
    private static final int PUERTO = 8080;

    public static void main(String[] args) {
        try (ServerSocket servidorSocket = new ServerSocket(PUERTO)) {
            System.out.println("Servidor iniciado en el puerto " + PUERTO);
            while (true) {
                manejarNuevaConexion(servidorSocket.accept());
            }
        } catch (IOException e) {
            System.err.println("Error al iniciar o ejecutar el servidor: " + e.getMessage());
        }
    }

    private static void manejarNuevaConexion(Socket socket) throws IOException {
        UnCliente unCliente = new UnCliente(socket);
        String id = ClienteManager.asignarNuevoCliente(unCliente);
        unCliente.setId(id);

        Thread hilo = new Thread(unCliente);
        hilo.start();
        System.out.println("Se conectó el cliente #" + id);
    }
}