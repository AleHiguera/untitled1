package ServidorMulti;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ClienteAuthManager {
    private static final String ARCHIVO_USUARIOS = "usuarios.txt";
    private static final Map<String, String> credenciales = new HashMap<>();

    static {
        cargarCredenciales();
    }

    private static void cargarCredenciales() {
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.contains(":")) {
                    String[] partes = linea.split(":", 2);
                    credenciales.put(partes[0], partes[1]);
                }
            }
        } catch (IOException e) {
            System.out.println("No se encontró el archivo de usuarios. Se creará uno nuevo.");
        }
    }

    private static void guardarCredencial(String usuario, String password) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ARCHIVO_USUARIOS, true))) {
            bw.write(usuario + ":" + password);
            bw.newLine();
        }
    }

    public static boolean registrarUsuario(String usuario, String password) throws IOException {
        if (credenciales.containsKey(usuario)) {
            return false;
        }
        credenciales.put(usuario, password);
        guardarCredencial(usuario, password);
        return true;
    }

    public static boolean verificarCredenciales(String usuario, String password) {
        return password.equals(credenciales.get(usuario));
    }

    public static boolean existeUsuario(String usuario) {
        return credenciales.containsKey(usuario);
    }
}