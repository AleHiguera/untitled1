package ServidorMulti;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class UnCliente implements Runnable {
    private final Socket socket;
    private final DataOutputStream salida;
    private final DataInputStream entrada;
    private String id;

    public UnCliente(Socket s) throws IOException {
        this.socket = s;
        this.salida = new DataOutputStream(s.getOutputStream());
        this.entrada = new DataInputStream(s.getInputStream());
    }

    public void setId(String id) {
        this.id = id;
    }

    public void enviarMensaje(String mensaje) throws IOException {
        salida.writeUTF(mensaje);
    }

    @Override
    public void run() {
        try {
            escucharMensajes();
        } catch (IOException e) {
        } finally {
            ClienteManager.eliminarCliente(id);
            cerrarConexion();
        }
    }

    private void escucharMensajes() throws IOException {
        String mensaje;
        while (!socket.isClosed()) {
            mensaje = entrada.readUTF();
            procesarMensaje(mensaje);
        }
    }

    private void procesarMensaje(String mensaje) throws IOException {
        if (mensaje.startsWith("@")) {
            manejarMensajePrivado(mensaje);
        } else {
            manejarMensajePublico(mensaje);
        }
    }

    private void manejarMensajePublico(String mensaje) throws IOException {
        String mensajeCompleto = "[Cliente #" + id + "]: " + mensaje;
        for (UnCliente cliente : ClienteManager.obtenerTodos().values()) {
            cliente.enviarMensaje(mensajeCompleto);
        }
    }

    private void manejarMensajePrivado(String mensaje) throws IOException {
        String mensajeCompleto = "[PRIVADO de Cliente #" + id + "]: " + mensaje;
        String[] partes = mensaje.substring(1).split(" ", 2);

        if (partes.length < 2) return;

        String[] destinatarios = partes[0].split(",");

        enviarADestinatarios(destinatarios, mensajeCompleto);
    }

    private void enviarADestinatarios(String[] destinatarios, String mensaje) throws IOException {
        boolean enviado = false;
        for (String nombre : destinatarios) {
            String nombreLimpio = nombre.trim();
            UnCliente cliente = ClienteManager.obtenerCliente(nombreLimpio);
            if (cliente != null) {
                cliente.enviarMensaje(mensaje);
                enviado = true;
            }
        }
        if (!enviado) {
            System.err.println("Advertencia: No se encontró a ningún destinatario.");
        }
    }

    private void cerrarConexion() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
        }
    }
}