package ServidorMulti;
import ServidorMulti.Juego.JuegoManager;
import ServidorMulti.Juego.RankingManager;
import ServidorMulti.Grupos.GroupManager;
import ServidorMulti.Util.MensajeUtil;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.regex.Pattern;

public class UnCliente implements Runnable {
    private final Socket socket;
    private final DataOutputStream salida;
    private final DataInputStream entrada;
    private String id;
    private String displayName;
    private int accionesRealizadas = 0;
    private boolean estaAutenticado = false;
    private static final int LIMITE_ACCIONES = 3;

    public UnCliente(Socket s) throws IOException {
        this.socket = s;
        this.salida = new DataOutputStream(s.getOutputStream());
        this.entrada = new DataInputStream(s.getInputStream());
    }

    public void setId(String id) {
        this.id = id;
        this.displayName = "Invitado #" + id;}

    public String getDisplayName() {
        return displayName;}

    public void enviarMensaje(String mensaje) throws IOException {
        salida.writeUTF(mensaje);}

    @Override
    public void run() {
        inicializarCliente();
        try {
            escucharMensajes();
        } catch (IOException e) {
            System.out.println("Cliente desconectado: " + displayName);
        } finally {
            limpiarRecursos();}
    }

    private void inicializarCliente() {
        GroupManager.asignarGrupoInicial(this.displayName, !this.estaAutenticado);
        enviarMensajeInicio();
    }

    private void limpiarRecursos() {
        JuegoManager.forzarRendicion(this.displayName);
        GroupManager.removerMiembroDeTodos(this.displayName);
        ClienteManager.eliminarCliente(id);
        cerrarConexion();}

    private void cerrarConexion() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }

    private void enviarMensajeInicio() {
        try {
            enviarMensaje("--- BIENVENIDO CLIENTE " + displayName.toUpperCase() + " ---");
            enviarMensaje("Modo Invitado: Puedes realizar **" + LIMITE_ACCIONES + " acciones** antes de iniciar sesión.");
            enviarMensaje("Usa: /register <usuario> <pass> o /login <usuario> <pass>");
            enviarMensaje("Comandos: /block <user>, /unblock <user>, /gato <user>, /aceptar <user>, /rechazar <user>");
            enviarMensaje("Comandos RANKING: /ranking general, /ranking yo, /ranking <user1> vs <user2>");
            enviarMensaje("Comandos GRUPO: /grupo [lista|crear <nombre>|borrar <nombre>|unir <nombre>|cambiar <nombre>|ver]");
            enviarMensaje("Grupo actual: " + GroupManager.obtenerNombreGrupoActual(displayName));
        } catch (IOException ignored) {}
    }

    private void escucharMensajes() throws IOException {
        String mensaje;
        while (!socket.isClosed()) {
            mensaje = entrada.readUTF();
            procesarEntrada(mensaje);}
    }

    private void procesarEntrada(String mensaje) throws IOException {
        String trimmedMsg = mensaje.trim();
        if (MensajeUtil.esComandoAutenticacion(trimmedMsg)) {
            manejarComandoAutenticacion(trimmedMsg);
            return;}
        if (!puedeRealizarAccion()) {
            enviarMensajeLimiteAlcanzado();
            return;}

        accionesRealizadas++;
        delegarProcesamiento(trimmedMsg);
        chequearLimiteAcciones();
    }

    private void chequearLimiteAcciones() throws IOException {
        if (!estaAutenticado && accionesRealizadas == LIMITE_ACCIONES) {
            enviarMensajeLimiteAlcanzado();
        } else if (!estaAutenticado) {
            int restantes = LIMITE_ACCIONES - accionesRealizadas;
            enviarMensaje("[INFO] Acción registrada. Acciones restantes como invitado: " + restantes);}
    }

    private void enviarMensajeLimiteAlcanzado() throws IOException {
        enviarMensaje("[ADVERTENCIA] ¡Límite de " + LIMITE_ACCIONES + " acciones alcanzado como invitado!");
        enviarMensaje("[ADVERTENCIA] Por favor, usa /login <usuario> <pass> o /register <usuario> <pass> para continuar.");
    }

    private boolean puedeRealizarAccion() {
        return estaAutenticado || accionesRealizadas < LIMITE_ACCIONES;}

    private void delegarProcesamiento(String mensaje) throws IOException {
        if (mensaje.isEmpty()) {
        } else if (MensajeUtil.esComando(mensaje)) {
            manejarComando(mensaje);
        } else if (esMovimientoGato(mensaje)) {
            manejarMovimientoGato(mensaje);
        } else if (mensaje.startsWith("@")) {
            manejarMensajePrivado(mensaje);
        } else {
            manejarMensajePublico(mensaje);}
    }

    private void manejarComando(String comando) throws IOException {
        if (comando.startsWith("/grupo")) {
            manejarComandoGrupo(comando);
        } else if (comando.startsWith("/ranking")) {
            manejarComandoRanking(comando);
        } else if (MensajeUtil.esComandoBloqueo(comando)) {
            manejarComandoBloqueo(comando);
        } else if (MensajeUtil.esComandoJuego(comando)) {
            manejarComandoJuego(comando);
        } else if (MensajeUtil.esComandoRevancha(comando)) {
            JuegoManager.procesarRevancha(displayName, comando);
        } else {
            enviarMensaje("[ERROR] Comando desconocido o formato incorrecto.");}
    }

    private void manejarComandoGrupo(String comando) throws IOException {
        String[] partes = comando.trim().split("\\s+", 3);
        String subComando = (partes.length > 1) ? partes[1].toLowerCase() : "";

        if (subComando.equals("lista") || subComando.equals("list") || partes.length == 1) {
            enviarMensaje(GroupManager.obtenerListadoGrupos());
            enviarMensaje("[INFO GRUPO] Tu grupo actual es: " + GroupManager.obtenerNombreGrupoActual(displayName));
        } else if (subComando.equals("ver")) {
            enviarMensaje(GroupManager.actualizarVistosYMostrar(displayName));
        } else if (partes.length == 3) {
            procesarAccionGrupoConNombre(subComando, partes[2].trim());
        } else {
            enviarMensaje("[ERROR GRUPO] Formato incorrecto. Uso: /grupo [lista|crear <nombre>|borrar <nombre>|unir <nombre>|cambiar <nombre>|ver]");
        }
    }

    private void procesarAccionGrupoConNombre(String subComando, String nombreGrupo) throws IOException {
        if (subComando.equals("crear")) {
            enviarMensaje(GroupManager.crearGrupo(nombreGrupo, displayName));
        } else if (subComando.equals("borrar")) {
            enviarMensaje(GroupManager.borrarGrupo(nombreGrupo, displayName));
        } else if (subComando.equals("unir") || subComando.equals("cambiar")) {
            enviarMensaje(GroupManager.unirGrupo(displayName, nombreGrupo));
        } else {
            enviarMensaje("[ERROR GRUPO] Comando no reconocido o formato incorrecto.");
        }
    }

    private void manejarComandoRanking(String comando) throws IOException {
        String argumento = MensajeUtil.obtenerArgumento(comando, 2);

        if (argumento == null) {
            enviarMensaje("[ERROR RANKING] Formato incorrecto. Uso: /ranking general, /ranking yo o /ranking <user1> vs <user2>");
            return;
        }

        String subComando = argumento.split("\\s+")[0].toLowerCase();
        if (subComando.equals("general")) {
            enviarMensaje(RankingManager.generarRankingGeneral(displayName));
        } else if (subComando.equals("yo")) {
            if (!estaAutenticado) {
                enviarMensaje("[ERROR RANKING] Debes iniciar sesión para ver tus estadísticas.");
                return;}
            enviarMensaje(RankingManager.generarRankingPersonal(displayName));
        } else if (argumento.toLowerCase().contains("vs")) {
            procesarRankingDuelo(argumento);
        } else {
            enviarMensaje("[ERROR RANKING] Subcomando no reconocido.");}
    }
    private void manejarComandoAutenticacion(String comando) throws IOException {
        String[] partes = comando.split(" ", 3);
        if (partes.length != 3) {
            enviarMensaje("[ERROR] Formato incorrecto. Uso: /comando <usuario> <pass>");
            return;}

        String user = partes[1];
        String pass = partes[2];

        if (comando.startsWith("/register")) {
            procesarRegistro(user, pass);
        } else if (comando.startsWith("/login")) {
            procesarLogin(user, pass);}
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
            enviarMensaje("[ERROR] Credenciales incorrectas.");}
    }

    private void establecerAutenticacion(String user) throws IOException {
        if (ClienteManager.obtenerClientePorNombre(user) != null) {
            enviarMensaje("[ERROR] El usuario ya está conectado.");
            return;}

        String oldDisplayName = this.displayName;
        this.estaAutenticado = true;
        this.displayName = user;
        this.accionesRealizadas = 0;

        ClienteManager.transferirCliente(oldDisplayName, this);
        GroupManager.asignarGrupoInicial(user, false);
        enviarMensaje("[INFO] ¡Autenticación exitosa! Conectado como: " + user + ". Grupo actual: " + GroupManager.obtenerNombreGrupoActual(user));
    }

    private void manejarComandoBloqueo(String comando) throws IOException {
        if (!estaAutenticado) {
            enviarMensaje("[ERROR] Debes iniciar sesión para usar los comandos /block y /unblock.");
            return;}

        String objetivo = MensajeUtil.obtenerArgumento(comando, 2);
        if (objetivo == null) {
            enviarMensaje("[ERROR] Formato incorrecto. Uso: /comando <usuario_objetivo>");
            return;}

        ejecutarAccionBloqueo(comando, objetivo);
    }

    private void ejecutarAccionBloqueo(String comando, String objetivo) throws IOException {
        if (objetivo.equals(displayName)) {
            enviarMensaje("[ERROR] No puedes bloquearte a ti mismo.");
            return;}
        if (!ClienteAuthManager.existeUsuario(objetivo)) {
            enviarMensaje("[ERROR] El usuario '" + objetivo + "' no existe.");
            return;}

        if (comando.startsWith("/block")) {
            ejecutarBloqueo(objetivo);
        } else if (comando.startsWith("/unblock")) {
            ejecutarDesbloqueo(objetivo);}
    }

    private void ejecutarBloqueo(String objetivo) throws IOException {
        if (BlockListManager.bloquearUsuario(displayName, objetivo)) {
            enviarMensaje("[INFO] Bloqueaste a " + objetivo + ". Se ha silenciado la comunicación mutua.");
        } else {
            enviarMensaje("[ADVERTENCIA] " + objetivo + " ya estaba bloqueado.");}
    }

    private void ejecutarDesbloqueo(String objetivo) throws IOException {
        if (BlockListManager.desbloquearUsuario(displayName, objetivo)) {
            enviarMensaje("[INFO] Desbloqueaste a " + objetivo + ". La comunicación mutua ha sido restaurada.");
        } else {
            enviarMensaje("[ADVERTENCIA] " + objetivo + " no estaba en tu lista de bloqueo.");}
    }
    private void manejarComandoJuego(String comando) throws IOException {
        String objetivo = MensajeUtil.obtenerArgumento(comando, 2);
        String accion = comando.split(" ")[0];

        if (objetivo == null) {
            enviarMensaje("[ERROR GATO] Formato incorrecto. Uso: /comando <usuario_objetivo>");
            return;}

        if (!estaAutenticado) {
            enviarMensaje("[ERROR GATO] Debes iniciar sesión para jugar.");
            return;}
        if (!ClienteAuthManager.existeUsuario(objetivo)) {
            enviarMensaje("[ERROR GATO] El usuario '" + objetivo + "' no existe.");
            return;}
        if (objetivo.equals(displayName)) {
            enviarMensaje("[ERROR GATO] No puedes jugar contra ti mismo.");
            return;}

        if (accion.equals("/gato")) {
            JuegoManager.invitar(displayName, objetivo);
        } else if (accion.equals("/aceptar")) {
            JuegoManager.iniciarPartida(objetivo, displayName);
        } else if (accion.equals("/rechazar")) {
            JuegoManager.rechazar(objetivo, displayName);}
    }

    private boolean esMovimientoGato(String mensaje) {
        return JuegoManager.obtenerPartida(displayName) != null &&
                Pattern.compile("^\\s*\\d+\\s+contra\\s+\\w+\\s*$").matcher(mensaje).matches();}

    private void manejarMovimientoGato(String mensaje) throws IOException {
        String[] partes = mensaje.trim().split("\\s+contra\\s+");

        if (partes.length != 2) {
            enviarMensaje("[ERROR GATO] Formato de movimiento incorrecto. Usa: [posicion] contra [oponente]");
            return;}

        try {
            int posicion = Integer.parseInt(partes[0].trim());
            String oponente = partes[1].trim();

            if (posicion >= 0 && posicion <= 8) {
                JuegoManager.procesarMovimiento(displayName, oponente, posicion);
            } else {
                enviarMensaje("[ERROR GATO] La posición debe ser entre 0 y 8.");}
        } catch (NumberFormatException e) {
            enviarMensaje("[ERROR GATO] La posición debe ser un número (0-8).");}
    }
    private void manejarMensajePublico(String mensaje) throws IOException {
        GroupManager.enviarMensajeGrupo(displayName, mensaje);
    }

    private void manejarMensajePrivado(String mensaje) throws IOException {
        String mensajeCompleto = "[PRIVADO de " + displayName + "]: " + mensaje;
        String[] partes = mensaje.substring(1).split(" ", 2);

        if (partes.length < 2) {
            enviarMensaje("[ERROR] Formato privado incorrecto. Uso: @<user1,user2> mensaje");
            return;}

        String[] destinatarios = partes[0].split(",");
        enviarADestinatarios(destinatarios, mensajeCompleto);
    }

    private void enviarADestinatarios(String[] destinatarios, String mensaje) throws IOException {
        for (String nombre : destinatarios) {
            String nombreLimpio = nombre.trim();
            UnCliente cliente = ClienteManager.obtenerClientePorNombre(nombreLimpio);

            if (cliente != null && BlockListManager.estaBloqueado(cliente.displayName, this.displayName)) {
                enviarMensaje("[ADVERTENCIA] No se pudo enviar mensaje a " + nombreLimpio + ". Te tiene bloqueado.");
            } else if (cliente != null) {
                cliente.enviarMensaje(mensaje);}
        }
    }

    private void procesarRankingDuelo(String dueloStr) throws IOException {
        String[] jugadores = dueloStr.split("\\s+vs\\s+");
        if (jugadores.length != 2) {
            enviarMensaje("[ERROR RANKING] Formato de duelo incorrecto. Uso: /ranking <user1> vs <user2>");
            return;
        }

        String user1 = jugadores[0].trim();
        String user2 = jugadores[1].trim();

        if (user1.isEmpty() || user2.isEmpty()) {
            enviarMensaje("[ERROR RANKING] Los nombres de usuario no pueden estar vacíos.");
            return;}

        enviarMensaje(RankingManager.generarRankingDuelo(user1, user2));
    }
    private void enviarBroadcast(String mensaje) throws IOException {
        for (UnCliente cliente : ClienteManager.obtenerTodos().values()) {
            enviarSiNoEstaBloqueadoYNoEsRemitente(cliente, mensaje);}
    }

    private void enviarSiNoEstaBloqueadoYNoEsRemitente(UnCliente receptor, String mensaje) throws IOException {
        if (receptor.id.equals(this.id)) return;

        if (BlockListManager.estaBloqueado(receptor.displayName, this.displayName) ||
                BlockListManager.estaBloqueado(this.displayName, receptor.displayName)) {
            return;}

        receptor.enviarMensaje(mensaje);
    }
}