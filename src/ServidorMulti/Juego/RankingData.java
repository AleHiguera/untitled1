package ServidorMulti.Juego;
import java.io.Serializable;

public class RankingData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String nombreUsuario;
    private int victorias;
    private int derrotas;
    private int empates;
    private int puntos;

    public RankingData(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
        this.victorias = 0;
        this.derrotas = 0;
        this.empates = 0;
        this.puntos = 0;
    }

    public String getNombreUsuario() { return nombreUsuario; }
    public int getVictorias() { return victorias; }
    public int getDerrotas() { return derrotas; }
    public int getEmpates() { return empates; }
    public int getPuntos() { return puntos; }
    public int getTotalPartidas() { return victorias + derrotas + empates; }

    public void registrarVictoria(int puntos) {
        this.victorias++;
        this.puntos += puntos;
    }

    public void registrarDerrota(int puntos) {
        this.derrotas++;
        this.puntos += puntos; // 0 puntos
    }

    public void registrarEmpate(int puntos) {
        this.empates++;
        this.puntos += puntos;
    }
}
