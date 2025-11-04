package ServidorMulti.Juego;
import ServidorMulti.ClienteAuthManager;
import java.io.*;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RankingManager {
    private static final String RANKING_FILE = "ranking.dat";
    private static Map<String, RankingData> stats = new ConcurrentHashMap<>();
    private static final int PUNTOS_VICTORIA = 2;
    private static final int PUNTOS_EMPATE = 1;
    private static final int PUNTOS_DERROTA = 0;

    static {cargarEstadisticas();}

    private RankingManager() {}
    private static void cargarEstadisticas() {
        File file = new File(RANKING_FILE);
        if (!file.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(RANKING_FILE))) {
            stats = (Map<String, RankingData>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error al cargar el ranking: " + e.getMessage());
            stats = new ConcurrentHashMap<>();
        }
    }

    private static void guardarEstadisticas() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(RANKING_FILE))) {
            oos.writeObject(stats);
        } catch (IOException e) {
            throw e;
        }
    }

    private static RankingData obtenerOCrearEstadisticas(String nombre) {
        if (!ClienteAuthManager.existeUsuario(nombre)) {
            return stats.computeIfAbsent(nombre, RankingData::new);
        }
        return stats.computeIfAbsent(nombre, RankingData::new);
    }
    public static void actualizarEstadisticas(String ganador, String perdedor) throws IOException {
        RankingData dataGanador = obtenerOCrearEstadisticas(ganador);
        RankingData dataPerdedor = obtenerOCrearEstadisticas(perdedor);

        dataGanador.registrarVictoria(PUNTOS_VICTORIA);
        dataPerdedor.registrarDerrota(PUNTOS_DERROTA);
        guardarEstadisticas();
    }

    public static void actualizarEstadisticasEmpate(String j1, String j2) throws IOException {
        RankingData dataJ1 = obtenerOCrearEstadisticas(j1);
        RankingData dataJ2 = obtenerOCrearEstadisticas(j2);

        dataJ1.registrarEmpate(PUNTOS_EMPATE);
        dataJ2.registrarEmpate(PUNTOS_EMPATE);
        guardarEstadisticas();
    }

    public static String generarRankingGeneral(String jugadorActual) {
        List<RankingData> topPlayers = stats.values().stream()
                .sorted(Comparator.comparingInt(RankingData::getPuntos).reversed())
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("--- RANKING GENERAL (Top 10) ---\n");
        sb.append(String.format("%-4s %-15s %-6s %-6s %-6s %s\n", "Pos", "Usuario", "Pts", "Gan", "Per", "Emp"));

        for (int i = 0; i < Math.min(10, topPlayers.size()); i++) {
            RankingData data = topPlayers.get(i);
            sb.append(String.format("%-4d %-15s %-6d %-6d %-6d %d\n",
                    i + 1, data.getNombreUsuario(), data.getPuntos(), data.getVictorias(), data.getDerrotas(), data.getEmpates()));
        }
        return sb.toString();
    }

    public static String generarRankingPersonal(String nombre) {
        RankingData data = obtenerOCrearEstadisticas(nombre);

        StringBuilder sb = new StringBuilder();
        sb.append("--- TUS ESTADÍSTICAS ---\n");
        sb.append(String.format("%-15s %-6s %-6s %-6s %-6s\n", "Usuario", "Pts", "Gan", "Per", "Emp"));
        sb.append(String.format("%-15s %-6d %-6d %-6d %-6d\n",
                data.getNombreUsuario(), data.getPuntos(), data.getVictorias(), data.getDerrotas(), data.getEmpates()));
        return sb.toString();
    }

    public static String generarRankingDuelo(String j1, String j2) {
        // Obtenemos o creamos los datos para asegurar que al menos están en el mapa
        RankingData dataJ1 = obtenerOCrearEstadisticas(j1);
        RankingData dataJ2 = obtenerOCrearEstadisticas(j2);

        if (!ClienteAuthManager.existeUsuario(j1) || !ClienteAuthManager.existeUsuario(j2)) {
            return "[ERROR RANKING] Uno o ambos usuarios no existen en el sistema.";
        }

        return formatDueloStats(dataJ1, dataJ2);
    }

    // Formatea las estadísticas para el duelo (SRP del formato)
    private static String formatDueloStats(RankingData d1, RankingData d2) {
        int v1 = d1.getVictorias();
        int d1_perdidas = d1.getDerrotas();
        int e1 = d1.getEmpates();

        int v2 = d2.getVictorias();
        int d2_perdidas = d2.getDerrotas();
        int e2 = d2.getEmpates();

        int total = v1 + d1_perdidas + e1 + v2 + d2_perdidas + e2;
        if (total == 0) return String.format("--- DUELO: %s vs %s ---\n[DUELO] Nunca han jugado partidas registradas.", d1.getNombreUsuario(), d2.getNombreUsuario());

        double winRate1 = (v1 + d2_perdidas) * 100.0 / total;
        double winRate2 = (v2 + d1_perdidas) * 100.0 / total;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("--- DUELO: %s vs %s ---\n", d1.getNombreUsuario(), d2.getNombreUsuario()));
        sb.append(String.format("Partidas totales registradas: %d\n\n", total));

        sb.append(String.format("%-15s | %-8s | %-8s | %-8s\n", "Estadística", d1.getNombreUsuario(), d2.getNombreUsuario(), "% Total"));
        sb.append("----------------------------------------------------\n");
        sb.append(String.format("%-15s | %-8d | %-8d | %-8.1f%%\n", "Victorias", v1, v2, winRate1));
        sb.append(String.format("%-15s | %-8d | %-8d | %-8.1f%%\n", "Derrotas", d1_perdidas, d2_perdidas, winRate2));
        sb.append(String.format("%-15s | %-8d | %-8d |\n", "Empates", e1, e2));
        sb.append(String.format("%-15s | %-8d | %-8d |\n", "Puntos", d1.getPuntos(), d2.getPuntos()));

        return sb.toString();
    }
}
