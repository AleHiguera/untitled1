package ServidorMulti;
import ServidorMulti.Juego.JuegoManager;
import ServidorMulti.ClienteAuthManager;
import ServidorMulti.BlockListManager;
import ServidorMulti.ClienteManager; // Import para ClienteManager

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class UnCliente implements Runnable {
    private final Socket socket;
    private final DataOutputStream salida;
    private final DataInputStream entrada;
    private String id;

    private String displayName;
    private int mensajesEnviados = 0;
    private boolean estaAutenticado = false;
    private static final int LIMITE_MENSAJES = 3;

    public UnCliente(Socket s) throws IOException {
        this.socket = s;
        this.salida = new DataOutputStream(s.getOutputStream());
        this.entrada = new DataInputStream(s.getInputStream());
    }

    public void setId(String id) {
        this.id = id;
        this.displayName = "Invitado #" + id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void enviarMensaje(String mensaje) throws IOException {
        salida.writeUTF(mensaje);
    }

    @Override
    public void run() {
        enviarMensajeInicio();
        try {
            escucharMensajes();
        } catch (IOException e) {
            // Manejo de excepción de desconexión silenciosa
        } finally {
            try {
                JuegoManager.forzarRendicion(this.displayName);
            } catch (IOException ignored) {}
            ClienteManager.eliminarCliente(id);
            cerrarConexion();
        }
    }

    private void enviarMensajeInicio() {
        try {
            enviarMensaje("--- BIENVENIDO CLIENTE " + displayName.toUpperCase() + " ---");
            enviarMensaje("Modo Invitado: Puedes enviar " + LIMITE_MENSAJES + " mensajes antes de iniciar sesión.");
            enviarMensaje("Usa: /register <usuario> <pass> o /login <usuario> <pass>");
            enviarMensaje("Comandos: /block <user>, /unblock <user>, /gato <user>, /aceptar <user>, /rechazar <user>");
        } catch (IOException ignored) {}
    }

    private void escucharMensajes() throws IOException {
        String mensaje;
        while (!socket.isClosed()) {
            mensaje = entrada.readUTF();
            procesarMensaje(mensaje);
        }
    }

    private void procesarMensaje(String mensaje) throws IOException {
        if (esComando(mensaje)) {
            manejarComando(mensaje);
        } else if (esMovimientoGato(mensaje)) {
            manejarMovimientoGato(mensaje);
        } else if (mensaje.startsWith("@")) {
            manejarMensajePrivado(mensaje);
        } else {
            manejarMensajePublico(mensaje);
        }
    }

    // --- LÓGICA DE COMANDOS ---
    private boolean esComando(String mensaje) {
        return mensaje.startsWith("/") &&
                (esComandoDeAutenticacion(mensaje) ||
                        esComandoDeBloqueo(mensaje) ||
                        esComandoDeJuego(mensaje) ||
                        esComandoDeRevancha(mensaje));
    }

    private boolean esComandoDeAutenticacion(String mensaje) {
        return mensaje.startsWith("/login") || mensaje.startsWith("/register");
    }

    private boolean esComandoDeBloqueo(String mensaje) {
        return mensaje.startsWith("/block") || mensaje.startsWith("/unblock");
    }

    private boolean esComandoDeJuego(String mensaje) {
        return mensaje.startsWith("/gato") ||
                mensaje.startsWith("/aceptar") ||
                mensaje.startsWith("/rechazar");
    }

    private boolean esComandoDeRevancha(String mensaje) {
        return mensaje.equalsIgnoreCase("/si") || mensaje.equalsIgnoreCase("/no");
    }

    private void manejarComando(String comando) throws IOException {
        if (esComandoDeAutenticacion(comando)) {
            manejarComandoAutenticacion(comando);
        } else if (esComandoDeBloqueo(comando)) {
            manejarComandoBloqueo(comando);
        } else if (esComandoDeJuego(comando)) {
            manejarComandoJuego(comando);
        } else if (esComandoDeRevancha(comando)) {
            JuegoManager.procesarRevancha(displayName, comando);
        } else {
            enviarMensaje("[ERROR] Comando desconocido o formato incorrecto.");
        }
    }

    private void manejarComandoAutenticacion(String comando) throws IOException {
        String[] partes = comando.split(" ", 3);
        if (partes.length != 3) {
            enviarMensaje("[ERROR] Formato incorrecto. Uso: /comando <usuario> <pass>");
            return;
        }

        String user = partes[1];
        String pass = partes[2];
        if (comando.startsWith("/register")) {
            procesarRegistro(user, pass);
        } else if (comando.startsWith("/login")) {
            procesarLogin(user, pass);
        }
    }

    private void procesarRegistro(String user, String pass) throws IOException {
        if (ClienteAuthManager.registrarUsuario(user, pass)) {
            establecerAutenticacion(user);
        } else {
            enviarMensaje("[ERROR] El usuario '" + user + "' ya existe.");
        }
    }

    private void procesarLogin(String user, String pass) throws IOException {
        if (ClienteAuthManager.verificarCredenciales(user, pass)) {
            establecerAutenticacion(user);
        } else {
            enviarMensaje("[ERROR] Credenciales incorrectas.");
        }
    }

    private void establecerAutenticacion(String user) throws IOException {
        this.estaAutenticado = true;
        this.displayName = user;
        enviarMensaje("[INFO] ¡Autenticación exitosa! Conectado como: " + user);
    }

    private void manejarComandoBloqueo(String comando) throws IOException {
        if (!estaAutenticado) {
            enviarMensaje("[ERROR] Debes iniciar sesión para usar los comandos /block y /unblock.");
            return;
        }

        String[] partes = comando.split(" ", 2);
        if (partes.length != 2) {
            enviarMensaje("[ERROR] Formato incorrecto. Uso: /comando <usuario_objetivo>");
            return;
        }

        String objetivo = partes[1].trim();
        ejecutarAccionBloqueo(comando, objetivo);
    }

    private void ejecutarAccionBloqueo(String comando, String objetivo) throws IOException {
        if (objetivo.equals(displayName)) {
            enviarMensaje("[ERROR] No puedes bloquearte a ti mismo.");
            return;
        }
        if (!ClienteAuthManager.existeUsuario(objetivo)) {
            enviarMensaje("[ERROR] El usuario '" + objetivo + "' no existe.");
            return;
        }

        if (comando.startsWith("/block")) {
            ejecutarBloqueo(objetivo);
        } else if (comando.startsWith("/unblock")) {
            ejecutarDesbloqueo(objetivo);
        }
    }

    private void ejecutarBloqueo(String objetivo) throws IOException {
        if (BlockListManager.bloquearUsuario(displayName, objetivo)) {
            enviarMensaje("[INFO] Bloqueaste a " + objetivo + ". Se ha silenciado la comunicación mutua.");
        } else {
            enviarMensaje("[ADVERTENCIA] " + objetivo + " ya estaba bloqueado.");
        }
    }

    private void ejecutarDesbloqueo(String objetivo) throws IOException {
        if (BlockListManager.desbloquearUsuario(displayName, objetivo)) {
            enviarMensaje("[INFO] Desbloqueaste a " + objetivo + ". La comunicación mutua ha sido restaurada.");
        } else {
            enviarMensaje("[ADVERTENCIA] " + objetivo + " no estaba en tu lista de bloqueo.");
        }
    }

    private void manejarComandoJuego(String comando) throws IOException {
        String[] partes = comando.split(" ", 2);
        if (partes.length != 2) {
            enviarMensaje("[ERROR GATO] Formato incorrecto. Uso: /comando <usuario_objetivo>");
            return;
        }

        String objetivo = partes[1].trim();
        String accion = partes[0];

        if (!estaAutenticado) {
            enviarMensaje("[ERROR GATO] Debes iniciar sesión para jugar.");
            return;
        }
        if (!ClienteAuthManager.existeUsuario(objetivo)) {
            enviarMensaje("[ERROR GATO] El usuario '" + objetivo + "' no existe.");
            return;
        }
        if (objetivo.equals(displayName)) {
            enviarMensaje("[ERROR GATO] No puedes jugar contra ti mismo.");
            return;
        }

        if (accion.equals("/gato")) {
            JuegoManager.invitar(displayName, objetivo);
        } else if (accion.equals("/aceptar")) {
            JuegoManager.iniciarPartida(objetivo, displayName);
        } else if (accion.equals("/rechazar")) {
            JuegoManager.rechazar(objetivo, displayName); // Método que causaba error si no era público en JuegoManager
        }
    }

    private boolean esMovimientoGato(String mensaje) {
        try {
            int pos = Integer.parseInt(mensaje.trim());
            return pos >= 0 && pos <= 8 && JuegoManager.obtenerPartida(displayName) != null;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void manejarMovimientoGato(String mensaje) throws IOException {
        int posicion = Integer.parseInt(mensaje.trim());
        JuegoManager.procesarMovimiento(displayName, posicion);
    }

    private void manejarMensajePublico(String mensaje) throws IOException {
        if (!puedeEnviarMensaje()) {
            enviarMensaje("[ADVERTENCIA] Límite de " + LIMITE_MENSAJES + " mensajes alcanzado. Por favor, regístrate o inicia sesión.");
            return;
        }

        mensajesEnviados++;
        String mensajeCompleto = "[" + displayName + "]: " + mensaje;
        enviarBroadcast(mensajeCompleto);
    }

    private boolean puedeEnviarMensaje() {
        return estaAutenticado || mensajesEnviados < LIMITE_MENSAJES;
    }

    private void enviarBroadcast(String mensaje) throws IOException {
        for (UnCliente cliente : ClienteManager.obtenerTodos().values()) {
            enviarSiNoEstaBloqueadoYNoEsRemitente(cliente, mensaje);
        }
    }

    private void enviarSiNoEstaBloqueadoYNoEsRemitente(UnCliente receptor, String mensaje) throws IOException {
        if (receptor.id.equals(this.id)) return;

        if (BlockListManager.estaBloqueado(receptor.displayName, this.displayName) ||
                BlockListManager.estaBloqueado(this.displayName, receptor.displayName)) {
            return;
        }

        receptor.enviarMensaje(mensaje);
    }

    private void manejarMensajePrivado(String mensaje) throws IOException {
        if (!puedeEnviarMensaje()) {
            enviarMensaje("[ADVERTENCIA] Límite de " + LIMITE_MENSAJES + " mensajes alcanzado.");
            return;
        }
        mensajesEnviados++;

        String mensajeCompleto = "[PRIVADO de " + displayName + "]: " + mensaje;
        String[] partes = mensaje.substring(1).split(" ", 2);

        if (partes.length < 2) {
            enviarMensaje("[ERROR] Formato privado incorrecto. Uso: @<user1,user2> mensaje");
            return;
        }

        String[] destinatarios = partes[0].split(",");
        enviarADestinatarios(destinatarios, mensajeCompleto);
    }

    private void enviarADestinatarios(String[] destinatarios, String mensaje) throws IOException {
        boolean enviado = false;
        for (String nombre : destinatarios) {
            String nombreLimpio = nombre.trim();
            UnCliente cliente = ClienteManager.obtenerClientePorNombre(nombreLimpio);
            if (cliente != null && !BlockListManager.estaBloqueado(cliente.displayName, this.displayName)) {
                cliente.enviarMensaje(mensaje);
                enviado = true;
            } else if (cliente != null && BlockListManager.estaBloqueado(cliente.displayName, this.displayName)) {
                enviarMensaje("[ADVERTENCIA] No se pudo enviar mensaje a " + nombreLimpio + ". Te tiene bloqueado.");
            }
        }
        if (!enviado) {
            enviarMensaje("[ADVERTENCIA] No se encontró o se pudo enviar el mensaje a ningún destinatario.");
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