package ServidorMulti.Grupos;
import ServidorMulti.ClienteAuthManager;
import ServidorMulti.ClienteManager;
import ServidorMulti.UnCliente;
import java.io.*;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List; // <<< IMPORT FALTANTE AÑADIDO
import java.util.Set;

public class GroupManager {
    private static final String GROUPS_FILE = "groups.dat";
    private static Map<String, Grupo> grupos = new ConcurrentHashMap<>();

    // Mapa para saber el grupo actual de cada usuario
    private static final Map<String, String> grupoActualUsuario = new ConcurrentHashMap<>();

    static {
        cargarGrupos();
        inicializarGrupoTodos();
    }

    private GroupManager() {}

    // --- PERSISTENCIA (SRP) ---
    private static void cargarGrupos() {
        File file = new File(GROUPS_FILE);
        if (!file.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(GROUPS_FILE))) {
            grupos = (Map<String, Grupo>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error al cargar grupos: " + e.getMessage());
            grupos = new ConcurrentHashMap<>();
        }
    }

    private static void guardarGrupos() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(GROUPS_FILE))) {
            oos.writeObject(grupos);
        } catch (IOException e) {
            throw e;
        }
    }

    // --- LÓGICA DE INICIALIZACIÓN ---
    private static void inicializarGrupoTodos() {
        if (!grupos.containsKey("Todos")) {
            Grupo todos = new Grupo("Todos", "SYSTEM");
            grupos.put("Todos", todos);
        }
    }

    // Método llamado al iniciar sesión (o al conectarse para invitados)
    public static void asignarGrupoInicial(String nombreUsuario, boolean esInvitado) {
        if (!grupoActualUsuario.containsKey(nombreUsuario)) {
            unirGrupo(nombreUsuario, "Todos");
        }

        // Asegurar que 'Todos' siempre tenga a todos sus miembros actuales
        Grupo todos = grupos.get("Todos");
        todos.agregarMiembro(nombreUsuario);

        // Si no está autenticado, siempre debe estar en 'Todos'
        if (esInvitado && grupoActualUsuario.containsKey(nombreUsuario) && !grupoActualUsuario.get(nombreUsuario).equals("Todos")) {
            unirGrupo(nombreUsuario, "Todos");
        }
    }

    // Método llamado al desconectarse
    public static void removerMiembroDeTodos(String nombreUsuario) {
        Grupo todos = grupos.get("Todos");
        if (todos != null) {
            todos.eliminarMiembro(nombreUsuario);
        }
        grupoActualUsuario.remove(nombreUsuario);
    }

    // --- LÓGICA DE COMANDOS DE GRUPO ---

    public static String crearGrupo(String nombreGrupo, String nombreCreador) throws IOException {
        if (nombreGrupo.equalsIgnoreCase("Todos")) {
            return "[ERROR GRUPO] El nombre 'Todos' está reservado.";
        }
        if (grupos.containsKey(nombreGrupo)) {
            return "[ERROR GRUPO] El grupo '" + nombreGrupo + "' ya existe.";
        }
        if (!ClienteAuthManager.existeUsuario(nombreCreador)) {
            return "[ERROR GRUPO] Debes ser un usuario registrado para crear grupos.";
        }

        Grupo nuevoGrupo = new Grupo(nombreGrupo, nombreCreador);
        grupos.put(nombreGrupo, nuevoGrupo);
        guardarGrupos();
        unirGrupo(nombreCreador, nombreGrupo);
        return String.format("[INFO GRUPO] Grupo '%s' creado y te has unido.", nombreGrupo);
    }

    public static String borrarGrupo(String nombreGrupo, String nombreUsuario) throws IOException {
        if (nombreGrupo.equalsIgnoreCase("Todos")) {
            return "[ERROR GRUPO] El grupo 'Todos' no puede ser eliminado.";
        }
        Grupo grupo = grupos.get(nombreGrupo);
        if (grupo == null) {
            return "[ERROR GRUPO] El grupo '" + nombreGrupo + "' no existe.";
        }
        if (!grupo.esCreador(nombreUsuario)) {
            return "[ERROR GRUPO] Solo el creador (" + grupo.getCreador() + ") puede borrar el grupo.";
        }

        // Mover a todos los miembros a 'Todos' antes de borrar.
        grupo.getMiembros().forEach(miembro -> unirGrupo(miembro, "Todos"));

        grupos.remove(nombreGrupo);
        guardarGrupos();
        return String.format("[INFO GRUPO] Grupo '%s' eliminado con éxito.", nombreGrupo);
    }

    public static String unirGrupo(String nombreUsuario, String nombreGrupo) {
        Grupo grupoDestino = grupos.get(nombreGrupo);
        if (grupoDestino == null) {
            return "[ERROR GRUPO] El grupo '" + nombreGrupo + "' no existe.";
        }

        // Lógica de restricción de invitado
        boolean esInvitado = !ClienteAuthManager.existeUsuario(nombreUsuario);
        if (esInvitado && !nombreGrupo.equalsIgnoreCase("Todos")) {
            return "[ERROR GRUPO] Los invitados solo pueden estar en el grupo 'Todos'.";
        }

        String grupoAnterior = grupoActualUsuario.get(nombreUsuario);

        if (grupoAnterior != null && grupoAnterior.equals(nombreGrupo)) {
            return "[INFO GRUPO] Ya te encuentras en el grupo '" + nombreGrupo + "'.";
        }

        // 1. Remover del grupo anterior
        if (grupoAnterior != null) {
            Grupo gAnterior = grupos.get(grupoAnterior);
            if (gAnterior != null) {
                gAnterior.eliminarMiembro(nombreUsuario);
            }
        }

        // 2. Unirse al nuevo grupo y actualizar el estado
        grupoDestino.agregarMiembro(nombreUsuario);
        grupoActualUsuario.put(nombreUsuario, nombreGrupo);

        // 3. Obtener y marcar como vistos los mensajes no leídos
        try {
            UnCliente cliente = ClienteManager.obtenerClientePorNombre(nombreUsuario);
            if (cliente != null) {
                // Notificación de cambio
                cliente.enviarMensaje(String.format("[INFO GRUPO] Te has unido a '%s'.", nombreGrupo));
                // Mostrar mensajes no vistos
                // El método obtenerYActualizarNoVistos ya no lanza IOException
                cliente.enviarMensaje(obtenerYActualizarNoVistos(nombreUsuario, grupoDestino));
            }
        } catch (IOException e) {
            System.err.println("Error I/O al notificar cambio de grupo: " + e.getMessage());
        }

        return String.format("[INFO GRUPO] Grupo cambiado a '%s'.", nombreGrupo);
    }

    // --- LÓGICA DE MENSAJERÍA Y BROADCAST (SRP) ---

    public static String obtenerNombreGrupoActual(String nombreUsuario) {
        return grupoActualUsuario.getOrDefault(nombreUsuario, "Todos");
    }

    public static void enviarMensajeGrupo(String remitente, String mensaje) throws IOException {
        String nombreGrupo = obtenerNombreGrupoActual(remitente);
        Grupo grupo = grupos.get(nombreGrupo);

        if (grupo == null) return;

        String mensajeCompleto = String.format("[%s] [%s]: %s", nombreGrupo, remitente, mensaje);
        grupo.agregarMensaje(mensajeCompleto);

        // Broadcast solo a los miembros conectados
        for (String miembro : grupo.getMiembros()) {
            UnCliente cliente = ClienteManager.obtenerClientePorNombre(miembro);
            if (cliente != null) {
                // Actualizar índice si el usuario ya vio el mensaje (ej: si son el remitente o ya lo vio)
                if (miembro.equals(remitente)) {
                    grupo.actualizarUltimoVisto(remitente);
                }

                cliente.enviarMensaje(mensajeCompleto);
            }
        }

        // Se guarda el estado después de enviar un mensaje (podría ser menos frecuente para optimizar)
        guardarGrupos();
    }

    public static String obtenerListadoGrupos() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- GRUPOS DISPONIBLES ---\n");
        grupos.keySet().stream()
                .sorted()
                .forEach(nombre -> {
                    Grupo g = grupos.get(nombre);
                    sb.append(String.format(" -> %s (Creador: %s, Miembros: %d)\n",
                            nombre, g.getCreador(), g.getMiembros().size()));
                });
        return sb.toString();
    }

    // CLÁUSULA THROWS IOException ELIMINADA: Corrige el error de compilación.
    private static String obtenerYActualizarNoVistos(String usuario, Grupo grupo) {
        List<String> noVistos = grupo.obtenerMensajesNoVistos(usuario);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("--- %d MENSAJES NO VISTOS EN '%s' ---\n", noVistos.size(), grupo.getNombre()));

        for (String msg : noVistos) {
            sb.append(msg).append("\n");
        }

        if (noVistos.isEmpty()) {
            sb.append("[INFO GRUPO] No hay mensajes nuevos en este grupo.\n");
        }

        return sb.toString();
    }

    public static String actualizarVistosYMostrar(String nombreUsuario) {
        String nombreGrupo = obtenerNombreGrupoActual(nombreUsuario);
        Grupo grupo = grupos.get(nombreGrupo);

        return obtenerYActualizarNoVistos(nombreUsuario, grupo);
    }
}