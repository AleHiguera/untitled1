package ServidorMulti;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ClienteManager {
    private static final Map<String, UnCliente> clientes = new HashMap<>();
    private static final AtomicInteger contador = new AtomicInteger(0);

    public static String asignarNuevoCliente(UnCliente cliente) {
        String id = String.valueOf(contador.getAndIncrement());
        clientes.put(id, cliente);
        return id;
    }

    public static void eliminarCliente(String id) {
        clientes.remove(id);
        System.out.println("Se desconectó el cliente #" + id);
    }

    public static Map<String, UnCliente> obtenerTodos() {
        return clientes;
    }

    public static UnCliente obtenerCliente(String id) {
        return clientes.get(id);
    }

    public static UnCliente obtenerClientePorNombre(String displayName) {
        for (UnCliente cliente : clientes.values()) {
            if (cliente.getDisplayName().equals(displayName)) {
                return cliente;
            }
        }
        return null;
    }
}