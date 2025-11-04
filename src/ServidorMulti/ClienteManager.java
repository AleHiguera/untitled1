package ServidorMulti;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ClienteManager {
    private static final Map<String, UnCliente> clientesConectados = new ConcurrentHashMap<>();
    private static final AtomicInteger contador = new AtomicInteger(0);

    public static String asignarNuevoCliente(UnCliente cliente) {
        String id = "Invitado #" + contador.getAndIncrement();
        clientesConectados.put(id, cliente);
        return id;
    }

    public static void eliminarCliente(String id) {
        clientesConectados.remove(id);
        System.out.println("Se desconectó el cliente #" + id);
    }

    public static Map<String, UnCliente> obtenerTodos() {
        return clientesConectados;
    }

    public static UnCliente obtenerCliente(String id) {
        return clientesConectados.get(id);
    }

    public static UnCliente obtenerClientePorNombre(String displayName) {
        if (clientesConectados.containsKey(displayName)) {
            return clientesConectados.get(displayName);
        }

        for (UnCliente cliente : clientesConectados.values()) {
            if (cliente.getDisplayName().equals(displayName)) {
                return cliente;
            }
        }
        return null;
    }

    public static void transferirCliente(String oldName, UnCliente newClientInstance) {
        clientesConectados.remove(oldName);
        clientesConectados.put(newClientInstance.getDisplayName(), newClientInstance);

        System.out.println("Cliente transferido: " + oldName + " -> " + newClientInstance.getDisplayName());
    }
}