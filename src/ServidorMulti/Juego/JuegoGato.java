package ServidorMulti.Juego;
public class JuegoGato {
    private final String[] tablero = new String[9];
    private final String jugadorX;
    private final String jugadorO;
    private String turnoActual;

    public JuegoGato(String j1, String j2, String empieza) {
        for (int i = 0; i < 9; i++) {
            tablero[i] = "";
        }
        this.jugadorX = j1.compareTo(j2) < 0 ? j1 : j2;
        this.jugadorO = j1.compareTo(j2) < 0 ? j2 : j1;
        this.turnoActual = empieza;
    }

    public boolean realizarMovimiento(String jugador, int posicion) {
        if (!validarTurno(jugador) || !validarPosicion(posicion)) {
            return false;
        }

        tablero[posicion] = getSimbolo(jugador);
        cambiarTurno();
        return true;
    }

    private boolean validarTurno(String jugador) {
        return jugador.equals(turnoActual);
    }

    private boolean validarPosicion(int posicion) {
        return posicion >= 0 && posicion < 9 && tablero[posicion].isEmpty();
    }

    private void cambiarTurno() {
        turnoActual = turnoActual.equals(jugadorX) ? jugadorO : jugadorX;
    }

    public String getSimbolo(String jugador) {
        return jugador.equals(jugadorX) ? "X" : "O";
    }
    public String getOponente(String jugador) {
        return jugador.equals(jugadorX) ? jugadorO : jugadorX;
    }
    public String getTurnoActual() { return turnoActual; }
    public String[] getTablero() { return tablero; }

    public String getEstadoJuego() {
        String ganadorSimbolo = TableroUtil.verificarGanador(tablero);
        if (ganadorSimbolo != null) return "GANADOR:" + obtenerNombreJugador(ganadorSimbolo);
        if (TableroUtil.verificarEmpate(tablero)) return "EMPATE";
        return "ACTIVO";
    }

    private String obtenerNombreJugador(String simbolo) {
        return simbolo.equals("X") ? jugadorX : jugadorO;
    }

    public String obtenerRepresentacionTablero() {
        return TableroUtil.obtenerRepresentacion(tablero);
    }
}