package ServidorMulti.Juego;
public class TableroUtil {

    private TableroUtil() {}
    public static String obtenerRepresentacion(String[] tablero) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- JUEGO DEL GATO ---\n");
        for (int i = 0; i < 9; i++) {
            String valor = tablero[i].isEmpty() ? String.valueOf(i) : tablero[i];
            sb.append(" [").append(valor).append("] ");
            if ((i + 1) % 3 == 0) {
                sb.append("\n");
            }
        }
        sb.append("----------------------\n");
        return sb.toString();
    }

    public static String verificarGanador(String[] tablero) {
        int[][] winCombos = {
                {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
                {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
                {0, 4, 8}, {2, 4, 6}
        };

        for (int[] combo : winCombos) {
            String p1 = tablero[combo[0]];
            String p2 = tablero[combo[1]];
            String p3 = tablero[combo[2]];

            if (!p1.isEmpty() && p1.equals(p2) && p2.equals(p3)) {
                return p1; // Devuelve 'X' o 'O'
            }
        }
        return null;
    }

    public static boolean verificarEmpate(String[] tablero) {
        for (String cell : tablero) {
            if (cell.isEmpty()) {
                return false;
            }
        }
        return verificarGanador(tablero) == null;
    }
}