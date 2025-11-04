package ServidorMulti.Grupos;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Grupo implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String nombre;
    private final String creador;
    private final Set<String> miembros;
    private final List<String> historialMensajes;

    private final ConcurrentHashMap<String, Integer> indiceUltimoVisto;

    public Grupo(String nombre, String creador) {
        this.nombre = nombre;
        this.creador = creador;
        this.miembros = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.historialMensajes = Collections.synchronizedList(new ArrayList<>());
        this.indiceUltimoVisto = new ConcurrentHashMap<>();
        if (!nombre.equals("Todos")) {
            this.miembros.add(creador);}
    }

    public String getNombre() { return nombre; }
    public String getCreador() { return creador; }
    public Set<String> getMiembros() { return miembros; }
    public List<String> getHistorialMensajes() { return historialMensajes; }

    public void agregarMiembro(String nombreUsuario) {
        miembros.add(nombreUsuario);}

    public void eliminarMiembro(String nombreUsuario) {
        miembros.remove(nombreUsuario);}

    public boolean esCreador(String nombreUsuario) {
        return nombreUsuario.equals(creador);}

    public void agregarMensaje(String mensajeCompleto) {
        historialMensajes.add(mensajeCompleto);}

    public int getTotalMensajes() {
        return historialMensajes.size();}

    public int getUltimoIndiceVisto(String usuario) {
        return indiceUltimoVisto.getOrDefault(usuario, 0);}

    public void actualizarUltimoVisto(String usuario) {
        indiceUltimoVisto.put(usuario, historialMensajes.size());}

    public List<String> obtenerMensajesNoVistos(String usuario) {
        int indiceInicio = getUltimoIndiceVisto(usuario);
        int indiceFin = historialMensajes.size();

        if (indiceInicio >= indiceFin) {
            return Collections.emptyList();}

        List<String> noVistos = historialMensajes.subList(indiceInicio, indiceFin);
        actualizarUltimoVisto(usuario);

        return noVistos;
    }
}