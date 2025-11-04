package ServidorMulti.Juego;
import ServidorMulti.ClienteManager;
import ServidorMulti.UnCliente;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JuegoManager {
    private static final Map<String, JuegoGato> partidasActivas = new ConcurrentHashMap<>();
    private static final Map<String, String> invitacionesPendientes = new ConcurrentHashMap<>();
    private static final Map<String, String> revanchasPendientes = new ConcurrentHashMap<>();

    private JuegoManager() {}

    private static String generarIdPartida(String j1, String j2) {
        return j1.compareTo(j2) < 0 ? j1 + ":" + j2 : j2 + ":" + j1;}

    public static JuegoGato obtenerPartida(String jugador) {
        for (String id : partidasActivas.keySet()) {
            if (id.startsWith(jugador + ":") || id.endsWith(":" + jugador)) {
                return partidasActivas.get(id);}
        }
        return null;
    }

    private static JuegoGato obtenerPartidaCon(String j1, String j2) {
        String id = generarIdPartida(j1, j2);
        return partidasActivas.get(id);}
    public static boolean invitar(String proponente, String oponente) throws IOException {
        if (obtenerPartidaCon(proponente, oponente) != null) {
            return manejarError(proponente, "Ya tienes una partida activa con " + oponente + ". Termina esa primero.");}

        if (invitacionesPendientes.containsKey(proponente)) {
            return manejarError(proponente, "Ya enviaste una invitación a " + invitacionesPendientes.get(proponente) + ". Espera su respuesta.");
        }

        UnCliente clienteOponente = ClienteManager.obtenerClientePorNombre(oponente);
        if (clienteOponente == null) {
            return manejarError(proponente, "El usuario " + oponente + " no está conectado.");}

        registrarYNotificarInvitacion(proponente, oponente);
        return true;
    }

    private static void registrarYNotificarInvitacion(String proponente, String oponente) throws IOException {
        invitacionesPendientes.put(proponente, oponente);
        enviarMensaje(oponente, "[JUEGO GATO] Has sido invitado por " + proponente + ". Usa /aceptar " + proponente + " o /rechazar " + proponente + ".");
        enviarMensaje(proponente, "[JUEGO GATO] Invitación enviada a " + oponente + ". Esperando respuesta...");}

    public static void rechazar(String proponente, String rechazante) throws IOException {
        invitacionesPendientes.remove(proponente);
        enviarMensaje(proponente, "[JUEGO GATO] " + rechazante + " ha rechazado tu invitación.");
        enviarMensaje(rechazante, "[JUEGO GATO] Has rechazado la invitación de " + proponente + ".");}
    public static JuegoGato iniciarPartida(String proponente, String aceptante) throws IOException {
        String oponenteInvitado = invitacionesPendientes.remove(proponente);

        if (oponenteInvitado == null || !oponenteInvitado.equals(aceptante)) {
            manejarError(aceptante, "Invitación expirada o inexistente.");
            return null;}

        if (obtenerPartidaCon(proponente, aceptante) != null) {
            manejarError(aceptante, "Ya hay una partida activa con " + proponente + ".");
            return null;}

        String empieza = Math.random() < 0.5 ? proponente : aceptante;
        JuegoGato juego = new JuegoGato(proponente, aceptante, empieza);
        partidasActivas.put(generarIdPartida(proponente, aceptante), juego);

        notificarInicio(proponente, aceptante, empieza, juego);
        return juego;
    }

    private static void notificarInicio(String j1, String j2, String empieza, JuegoGato juego) throws IOException {
        String turnoMsg = "[JUEGO GATO] ¡Tu turno! Ingresa la posición (0-8) para jugar: " + juego.getSimbolo(empieza) + " contra " + juego.getOponente(empieza) + ".";
        String msgJ1 = "[JUEGO GATO] Partida iniciada con " + j2 + ". Eres " + juego.getSimbolo(j1) + ". **Tú eres " + juego.getSimbolo(j1) + "**. " + juego.obtenerRepresentacionTablero();
        String msgJ2 = "[JUEGO GATO] Partida iniciada con " + j1 + ". Eres " + juego.getSimbolo(j2) + ". **Tú eres " + juego.getSimbolo(j2) + "**. " + juego.obtenerRepresentacionTablero();

        enviarMensaje(j1, msgJ1);
        enviarMensaje(j2, msgJ2);

        enviarMensaje(empieza, turnoMsg);
    }

    private static void terminarPartida(String j1, String j2) {
        partidasActivas.remove(generarIdPartida(j1, j2));}
    public static void procesarMovimiento(String jugador, String oponente, int posicion) throws IOException {
        JuegoGato juego = obtenerPartidaCon(jugador, oponente);

        if (juego == null) {
            manejarError(jugador, "No tienes una partida activa con " + oponente + ".");
            return;}

        if (!juego.realizarMovimiento(jugador, posicion)) {
            manejarError(jugador, "Movimiento inválido contra " + oponente + ". No es tu turno o la posición está ocupada (0-8).");
            return;}

        String oponenteReal = juego.getOponente(jugador);
        notificarMovimiento(jugador, oponenteReal, juego);
        verificarFinJuego(jugador, oponenteReal, juego);
    }

    private static void notificarMovimiento(String jugador, String oponente, JuegoGato juego) throws IOException {
        String tablero = juego.obtenerRepresentacionTablero();
        enviarMensaje(jugador, "[JUEGO GATO vs " + oponente + "] Moviste a la posición " + juego.getSimbolo(jugador) + ".\n" + tablero);

        enviarMensaje(oponente, "[JUEGO GATO vs " + jugador + "] Turno de " + juego.getTurnoActual() + ". " + jugador + " movió a la posición " + juego.getSimbolo(jugador) + ".\n" + tablero);

        if (juego.getEstadoJuego().equals("ACTIVO")) {
            enviarMensaje(juego.getTurnoActual(), "[JUEGO GATO vs " + juego.getOponente(juego.getTurnoActual()) + "] ¡Tu turno! Ingresa la posición (0-8) para jugar: " + juego.getSimbolo(juego.getTurnoActual()) + " contra " + juego.getOponente(juego.getTurnoActual()) + ".");
        }
    }

    private static void verificarFinJuego(String j1, String j2, JuegoGato juego) throws IOException {
        String estado = juego.getEstadoJuego();
        if (estado.equals("ACTIVO")) return;

        String mensaje;
        String ganador = null;
        try {
            if (estado.startsWith("GANADOR")) {
                ganador = estado.split(":")[1];
                String perdedor = juego.getOponente(ganador);
                mensaje = "[JUEGO GATO vs " + perdedor + "] ¡Felicidades " + ganador + "! Ganaste contra " + perdedor + ".";
                RankingManager.actualizarEstadisticas(ganador, perdedor);
            } else {
                mensaje = "[JUEGO GATO vs " + j2 + "] ¡Empate! Partida terminada.";
                RankingManager.actualizarEstadisticasEmpate(j1, j2);
            }

            String tableroFinal = juego.obtenerRepresentacionTablero();
            enviarMensaje(j1, mensaje + "\n" + tableroFinal);
            enviarMensaje(j2, mensaje + "\n" + tableroFinal);

            terminarPartida(j1, j2);

            solicitarRevancha(j1, j2);

        } catch (IOException e) {
            System.err.println("Error de I/O al finalizar el juego o actualizar el ranking: " + e.getMessage());
            terminarPartida(j1, j2);
        }
    }
    private static void solicitarRevancha(String j1, String j2) throws IOException {
        String idRevancha = generarIdPartida(j1, j2);

        revanchasPendientes.put(idRevancha, "PENDIENTE");

        String mensajeRevancha = "[REVANCHA vs " + j2 + "] ¿Deseas la revancha? Responde /si o /no.";

        enviarMensaje(j1, mensajeRevancha);
        enviarMensaje(j2, mensajeRevancha.replace(j2, j1));
    }

    public static void procesarRevancha(String jugador, String comando) throws IOException {
        String oponente = buscarOponenteRevancha(jugador);

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

    private static String buscarOponenteRevancha(String jugador) {
        for (Map.Entry<String, String> entry : revanchasPendientes.entrySet()) {
            if (entry.getKey().contains(jugador)) {
                String[] jugadores = entry.getKey().split(":");
                return jugadores[0].equals(jugador) ? jugadores[1] : jugadores[0];
            }
        }
        return null;
    }

    private static void aceptarRevancha(String idRevancha, String jugadorQueAcepta, String oponente) throws IOException {
        String estadoActual = revanchasPendientes.get(idRevancha);

        if (estadoActual == null) {
            manejarError(jugadorQueAcepta, "La solicitud de revancha ha expirado.");
            return;}

        manejarDobleAceptacion(idRevancha, jugadorQueAcepta, oponente, estadoActual);
    }

    private static void manejarDobleAceptacion(String idRevancha, String jAcepta, String oponente, String estadoActual) throws IOException {
        if (estadoActual.equals("PENDIENTE")) {
            revanchasPendientes.put(idRevancha, jAcepta);
            enviarMensaje(jAcepta, "[REVANCHA vs " + oponente + "] Aceptaste. Esperando la respuesta de " + oponente + ".");
            enviarMensaje(oponente, "[REVANCHA vs " + jAcepta + "] " + jAcepta + " ha aceptado. Responde /si o /no para iniciar.");

        } else if (estadoActual.equals(oponente)) {
            iniciarNuevaPartida(idRevancha, jAcepta, oponente);

        } else if (estadoActual.equals(jAcepta)) {
            enviarMensaje(jAcepta, "[ADVERTENCIA] Ya habías aceptado la revancha. Esperando la respuesta de " + oponente + ".");
        }
    }

    private static void iniciarNuevaPartida(String idRevancha, String j1, String j2) throws IOException {
        revanchasPendientes.remove(idRevancha);

        if (obtenerPartidaCon(j1, j2) != null) {
            manejarError(j1, "Ya hay una partida activa entre tú y " + j2 + ".");
            manejarError(j2, "Ya hay una partida activa entre tú y " + j1 + ".");
            return;}

        String empieza = j2;
        JuegoGato juego = new JuegoGato(j1, j2, empieza);
        partidasActivas.put(generarIdPartida(j1, j2), juego);

        enviarMensaje(j1, "[REVANCHA ACEPTADA vs " + j2 + "] Ambos dijeron /si. ¡Comienza la nueva partida! **" + j2 + "** va primero.");
        enviarMensaje(j2, "[REVANCHA ACEPTADA vs " + j1 + "] Ambos dijeron /si. ¡Comienza la nueva partida! **Tú** vas primero.");
        notificarInicio(j1, j2, empieza, juego);
    }

    private static void rechazarRevancha(String idRevancha, String jugadorQueRechaza, String oponente) throws IOException {
        revanchasPendientes.remove(idRevancha);

        enviarMensaje(jugadorQueRechaza, "[REVANCHA vs " + oponente + " RECHAZADA] Has rechazado la revancha. Vuelves al chat normal.");
        enviarMensaje(oponente, "[REVANCHA vs " + jugadorQueRechaza + " RECHAZADA] " + jugadorQueRechaza + " ha rechazado la revancha. Vuelven al chat normal.");
    }
    public static void forzarRendicion(String jugador) {
        invitacionesPendientes.remove(jugador);
        revanchasPendientes.entrySet().removeIf(entry -> entry.getKey().contains(jugador) || entry.getValue().equals(jugador));
        partidasActivas.entrySet().removeIf(entry -> {
            String id = entry.getKey();
            if (id.contains(jugador)) {
                JuegoGato juego = entry.getValue();
                String oponente = juego.getOponente(jugador);

                try {
                    if (!juego.getEstadoJuego().startsWith("GANADOR")) {
                        RankingManager.actualizarEstadisticas(oponente, jugador);
                        enviarMensaje(oponente, "[JUEGO GATO vs " + jugador + "] ¡ATENCIÓN! " + jugador + " se ha desconectado. Has ganado la partida por rendición.");
                    }
                } catch (IOException e) {
                    System.err.println("Error al actualizar ranking o notificar rendición: " + e.getMessage());}
                return true;
            }
            return false;
        });
    }
    private static void enviarMensaje(String nombre, String mensaje) throws IOException {
        UnCliente cliente = ClienteManager.obtenerClientePorNombre(nombre);
        if (cliente != null) {
            cliente.enviarMensaje(mensaje);}
    }

    private static boolean manejarError(String jugador, String mensaje) throws IOException {
        enviarMensaje(jugador, "[ERROR GATO] " + mensaje);
        return false;}
}