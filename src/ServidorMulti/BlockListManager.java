package ServidorMulti;
import java.io.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BlockListManager {
    private static final Map<String, Set<String>> bloqueos = new ConcurrentHashMap<>();
    private static final String ARCHIVO_BLOQUEOS = "bloqueos.txt";

    static {
        cargarBloqueos();
    }

    private static void cargarBloqueos() {
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_BLOQUEOS))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.contains(":")) {
                    String[] partes = linea.split(":", 2);
                    Set<String> bloqueados = new HashSet<>();
                    if (partes.length > 1 && !partes[1].isEmpty()) {
                        String[] nombres = partes[1].split(",");
                        for (String nombre : nombres) {
                            bloqueados.add(nombre.trim());
                        }
                    }
                    bloqueos.put(partes[0], bloqueados);
                }
            }
        } catch (IOException e) {
            System.out.println("No se encontró el archivo de bloqueos. Se creará uno nuevo.");
        }
    }

    private static void guardarBloqueos() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ARCHIVO_BLOQUEOS))) {
            for (Map.Entry<String, Set<String>> entrada : bloqueos.entrySet()) {
                String bloqueador = entrada.getKey();
                String lista = String.join(",", entrada.getValue());
                bw.write(bloqueador + ":" + lista);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("[ERROR] No se pudo guardar el archivo de bloqueos.");
        }
    }

    public static boolean bloquearUsuario(String bloqueador, String bloqueado) {
        Set<String> lista = bloqueos.computeIfAbsent(bloqueador, k -> new HashSet<>());
        if (lista.add(bloqueado)) {
            guardarBloqueos();
            return true;
        }
        return false;
    }

    public static boolean desbloquearUsuario(String bloqueador, String bloqueado) {
        Set<String> lista = bloqueos.get(bloqueador);
        if (lista != null && lista.remove(bloqueado)) {
            guardarBloqueos();
            return true;
        }
        return false;
    }

    public static boolean estaBloqueado(String receptor, String remitente) {
        Set<String> lista = bloqueos.get(receptor);
        return lista != null && lista.contains(remitente);
    }
}