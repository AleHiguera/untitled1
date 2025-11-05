package ServidorMulti.Util;
public class MensajeUtil {

    public static boolean esComando(String mensaje) {
        return mensaje.startsWith("/") && (esComandoAutenticacion(mensaje) || esComandoBloqueo(mensaje) || esComandoJuego(mensaje) || esComandoRevancha(mensaje) || mensaje.startsWith("/ranking") || mensaje.startsWith("/grupo"));}

    public static boolean esComandoAutenticacion(String mensaje) {
        return mensaje.startsWith("/login") || mensaje.startsWith("/register");}

    public static boolean esComandoBloqueo(String mensaje) {
        return mensaje.startsWith("/block") || mensaje.startsWith("/unblock");}

    public static boolean esComandoJuego(String mensaje) {
        return mensaje.startsWith("/gato") || mensaje.startsWith("/aceptar") || mensaje.startsWith("/rechazar");}

    public static boolean esComandoRevancha(String mensaje) {
        return mensaje.equalsIgnoreCase("/si") || mensaje.equalsIgnoreCase("/no");}

    public static String obtenerArgumento(String comando, int parte) {
        String[] partes = comando.split(" ", parte);
        return (partes.length == parte) ? partes[parte - 1].trim() : null;}
}
