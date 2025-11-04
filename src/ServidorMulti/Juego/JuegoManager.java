package ServidorMulti.Juego;
import ServidorMulti.ClienteManager;
import ServidorMulti.UnCliente;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JuegoManager {
    private static final Map<String, JuegoGato> partidasActivas = new ConcurrentHashMap<>();
    private static final Map<String, String> invitacionesPendientes = new ConcurrentHashMap<>();

    // Clave: ID de la partida (J1:J2), Valor: Nombre del jugador que YA DIJO /si. "PENDIENTE" si nadie ha dicho /si.
    private static final Map<String, String> revanchasPendientes = new ConcurrentHashMap<>();

    private JuegoManager() {}

    private static String generarIdPartida(String j1, String j2) {
        return j1.compareTo(j2) < 0 ? j1 + ":" + j2 : j2 + ":" + j1;
    }

    public static JuegoGato obtenerPartida(String jugador) {
        for (String id : partidasActivas.keySet()) {
            if (id.startsWith(jugador + ":") || id.endsWith(":" + jugador)) {
                return partidasActivas.get(id);
            }
        }
        return null;
    }

    private static boolean existePartidaCon(String j1, String j2) {
        return partidasActivas.containsKey(generarIdPartida(j1, j2));
    }

    // --- LÓGICA DE INVITACIÓN ---
    public static boolean invitar(String proponente, String oponente) throws IOException {
        if (existePartidaCon(proponente, oponente)) {
            return manejarError(proponente, "Ya tienes una partida activa con " + oponente + ".");
        }
        if (invitacionesPendientes.containsKey(proponente)) {
            return manejarError(proponente, "Ya enviaste una invitación a " + invitacionesPendientes.get(proponente) + ". Espera su respuesta.");
        }

        UnCliente clienteOponente = ClienteManager.obtenerClientePorNombre(oponente);
        if (clienteOponente == null) {
            return manejarError(proponente, "El usuario " + oponente + " no está conectado.");
        }

        registrarYNotificarInvitacion(proponente, oponente);
        return true;
    }

    private static void registrarYNotificarInvitacion(String proponente, String oponente) throws IOException {
        invitacionesPendientes.put(proponente, oponente);
        enviarMensaje(oponente, "[JUEGO GATO] Has sido invitado por " + proponente + ". Usa /aceptar " + proponente + " o /rechazar " + proponente + ".");
        enviarMensaje(proponente, "[JUEGO GATO] Invitación enviada a " + oponente + ". Esperando respuesta...");
    }

    // Método que causaba el error de compilación si no era public static
    public static void rechazar(String proponente, String rechazante) throws IOException {
        invitacionesPendientes.remove(proponente);
        enviarMensaje(proponente, "[JUEGO GATO] " + rechazante + " ha rechazado tu invitación.");
        enviarMensaje(rechazante, "[JUEGO GATO] Has rechazado la invitación de " + proponente + ".");
    }

    // --- INICIO DE PARTIDA ---
    public static JuegoGato iniciarPartida(String proponente, String aceptante) throws IOException {
        String oponenteInvitado = invitacionesPendientes.remove(proponente);

        if (oponenteInvitado == null || !oponenteInvitado.equals(aceptante)) {
            manejarError(aceptante, "Invitación expirada o inexistente.");
            return null;
        }

        String empieza = Math.random() < 0.5 ? proponente : aceptante;
        JuegoGato juego = new JuegoGato(proponente, aceptante, empieza);
        partidasActivas.put(generarIdPartida(proponente, aceptante), juego);

        notificarInicio(proponente, aceptante, empieza, juego);
        return juego;
    }

    private static void notificarInicio(String j1, String j2, String empieza, JuegoGato juego) throws IOException {
        String turnoMsg = "[JUEGO GATO] ¡Tu turno! Ingresa la posición (0-8) para jugar.";
        String msgJ1 = "[JUEGO GATO] Partida iniciada con " + j2 + ". Eres " + juego.getSimbolo(j1) + ". **Tú eres " + juego.getSimbolo(j1) + "**. " + juego.obtenerRepresentacionTablero();
        String msgJ2 = "[JUEGO GATO] Partida iniciada con " + j1 + ". Eres " + juego.getSimbolo(j2) + ". **Tú eres " + juego.getSimbolo(j2) + "**. " + juego.obtenerRepresentacionTablero();

        enviarMensaje(j1, msgJ1);
        enviarMensaje(j2, msgJ2);

        enviarMensaje(empieza, turnoMsg);
    }

    private static void terminarPartida(String j1, String j2) {
        partidasActivas.remove(generarIdPartida(j1, j2));
    }

    // --- LÓGICA DE MOVIMIENTO ---
    public static void procesarMovimiento(String jugador, int posicion) throws IOException {
        JuegoGato juego = obtenerPartida(jugador);
        if (juego == null) {
            manejarError(jugador, "No estás en ninguna partida activa.");
            return;
        }

        if (!juego.realizarMovimiento(jugador, posicion)) {
            manejarError(jugador, "Movimiento inválido. No es tu turno o la posición está ocupada (0-8).");
            return;
        }

        String oponente = juego.getOponente(jugador);
        notificarMovimiento(jugador, oponente, juego);
        verificarFinJuego(jugador, oponente, juego);
    }

    private static void notificarMovimiento(String jugador, String oponente, JuegoGato juego) throws IOException {
        String tablero = juego.obtenerRepresentacionTablero();
        // Notificación al jugador que movió
        enviarMensaje(jugador, "[JUEGO GATO] Moviste a la posición " + juego.getSimbolo(jugador) + ".\n" + tablero);

        // Notificación al oponente
        enviarMensaje(oponente, "[JUEGO GATO] Turno de " + juego.getTurnoActual() + ". " + jugador + " movió a la posición " + juego.getSimbolo(jugador) + ".\n" + tablero);

        // Mensaje de turno al jugador al que le toca (si la partida sigue activa)
        if (juego.getEstadoJuego().equals("ACTIVO")) {
            enviarMensaje(juego.getTurnoActual(), "[JUEGO GATO] ¡Tu turno! Ingresa la posición (0-8) para jugar.");
        }
    }

    private static void verificarFinJuego(String j1, String j2, JuegoGato juego) throws IOException {
        String estado = juego.getEstadoJuego();
        if (estado.equals("ACTIVO")) return;

        String mensaje;
        String ganador = null;
        if (estado.startsWith("GANADOR")) {
            ganador = estado.split(":")[1];
            mensaje = "[JUEGO GATO] ¡Felicidades " + ganador + "! Ganaste contra " + juego.getOponente(ganador) + ".";
        } else { // EMPATE
            mensaje = "[JUEGO GATO] ¡Empate! Partida terminada.";
        }

        String tableroFinal = juego.obtenerRepresentacionTablero();
        enviarMensaje(j1, mensaje + "\n" + tableroFinal);
        enviarMensaje(j2, mensaje + "\n" + tableroFinal);

        // Limpiamos la partida activa
        terminarPartida(j1, j2);

        // Iniciamos el proceso de doble confirmación de revancha
        solicitarRevancha(j1, j2);
    }

    // --- LÓGICA DE REVANCHA CORREGIDA PARA DOBLE CONFIRMACIÓN ---

    private static void solicitarRevancha(String j1, String j2) throws IOException {
        String idRevancha = generarIdPartida(j1, j2);

        // Registramos la solicitud como PENDIENTE
        revanchasPendientes.put(idRevancha, "PENDIENTE");

        String mensajeRevancha = "[REVANCHA] ¿Deseas la revancha? Responde /si o /no.";

        enviarMensaje(j1, mensajeRevancha);
        enviarMensaje(j2, mensajeRevancha);
    }

    public static void procesarRevancha(String jugador, String comando) throws IOException {
        String oponente = null;

        // Buscar al oponente
        for (Map.Entry<String, String> entry : revanchasPendientes.entrySet()) {
            if (entry.getKey().contains(jugador)) {
                String[] jugadores = entry.getKey().split(":");
                oponente = jugadores[0].equals(jugador) ? jugadores[1] : jugadores[0];
                break;
            }
        }

        if (oponente == null) {
            enviarMensaje(jugador, "[ERROR] No estás involucrado en una solicitud de revancha pendiente.");
            return;
        }

        String idRevancha = generarIdPartida(jugador, oponente);

        if (comando.equalsIgnoreCase("/no")) {
            rechazarRevancha(idRevancha, jugador, oponente);
        } else if (comando.equalsIgnoreCase("/si")) {
            aceptarRevancha(idRevancha, jugador, oponente);
        } else {
            manejarError(jugador, "Comando de revancha inválido. Usa /si o /no.");
        }
    }

    private static void aceptarRevancha(String idRevancha, String jugadorQueAcepta, String oponente) throws IOException {
        String estadoActual = revanchasPendientes.get(idRevancha);

        if (estadoActual == null) {
            manejarError(jugadorQueAcepta, "La solicitud de revancha ha expirado.");
            return;
        }

        if (estadoActual.equals("PENDIENTE")) {
            // Primer jugador en decir /si
            revanchasPendientes.put(idRevancha, jugadorQueAcepta);
            enviarMensaje(jugadorQueAcepta, "[REVANCHA] Aceptaste. Esperando la respuesta de " + oponente + ".");
            enviarMensaje(oponente, "[REVANCHA] " + jugadorQueAcepta + " ha aceptado. Responde /si o /no para iniciar.");

        } else if (estadoActual.equals(oponente)) {
            // Segundo jugador en decir /si. ¡Iniciamos!
            revanchasPendientes.remove(idRevancha);

            String j1 = jugadorQueAcepta; // Jugador que aceptó de segundo
            String j2 = oponente; // Jugador que aceptó de primero (estaba en el estado)

            // Iniciamos la partida. El que respondió primero (j2/oponente) empieza para variar.
            String empieza = j2;
            JuegoGato juego = new JuegoGato(j1, j2, empieza);
            partidasActivas.put(generarIdPartida(j1, j2), juego);

            enviarMensaje(j1, "[REVANCHA ACEPTADA] Ambos dijeron /si. ¡Comienza la nueva partida! **" + j2 + "** va primero.");
            enviarMensaje(j2, "[REVANCHA ACEPTADA] Ambos dijeron /si. ¡Comienza la nueva partida! **Tú** vas primero.");
            notificarInicio(j1, j2, empieza, juego);

        } else if (estadoActual.equals(jugadorQueAcepta)) {
            // El jugador ya había dicho /si
            enviarMensaje(jugadorQueAcepta, "[ADVERTENCIA] Ya habías aceptado la revancha. Esperando la respuesta de " + oponente + ".");
        }
    }

    private static void rechazarRevancha(String idRevancha, String jugadorQueRechaza, String oponente) throws IOException {
        revanchasPendientes.remove(idRevancha);

        enviarMensaje(jugadorQueRechaza, "[REVANCHA RECHAZADA] Has rechazado la revancha. Vuelves al chat normal.");
        enviarMensaje(oponente, "[REVANCHA RECHAZADA] " + jugadorQueRechaza + " ha rechazado la revancha. Vuelven al chat normal.");
    }

    // --- LÓGICA DE RENDICIÓN POR DESCONEXIÓN ---
    public static void forzarRendicion(String jugador) throws IOException {
        invitacionesPendientes.remove(jugador);

        // Limpiar revanchas: remueve entradas donde el jugador es parte del ID o el estado
        revanchasPendientes.entrySet().removeIf(entry -> entry.getKey().contains(jugador) || entry.getValue().equals(jugador));

        JuegoGato juego = obtenerPartida(jugador);
        if (juego == null) return;

        String oponente = juego.getOponente(jugador);
        enviarMensaje(oponente, "[JUEGO GATO] ¡ATENCIÓN! " + jugador + " se ha desconectado. Has ganado la partida por rendición.");
        terminarPartida(jugador, oponente);
    }

    // --- HELPERS ---
    private static void enviarMensaje(String nombre, String mensaje) throws IOException {
        UnCliente cliente = ClienteManager.obtenerClientePorNombre(nombre);
        if (cliente != null) {
            cliente.enviarMensaje(mensaje);
        }
    }

    private static boolean manejarError(String jugador, String mensaje) throws IOException {
        enviarMensaje(jugador, "[ERROR GATO] " + mensaje);
        return false;
    }
}