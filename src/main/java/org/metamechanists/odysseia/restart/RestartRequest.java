package org.metamechanists.odysseia.restart;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Deja constancia en disco de que hace falta un reinicio.
 *
 * Pterodactyl no deja que el servidor se reinicie a si mismo: el {@code /restart} de vanilla
 * depende de un script que no existe en el contenedor, y una parada limpia deja el servidor caido
 * en vez de volver. La señal tiene que venir de fuera, por la API del panel.
 *
 * La API key vive en Star y **no** se copia aqui a proposito: la caja del juego ejecuta codigo de
 * decenas de plugins de terceros, y una clave que puede apagar el servidor no pinta nada ahi. Asi
 * que Odysseia solo deja esta peticion y Star, que ya entra por SFTP, la recoge y llama al panel.
 *
 * El archivo lleva la hora para que Star pueda descartarlo si es viejo: si Star estuvo caido dos
 * dias, al volver no debe reiniciar por una peticion de anteayer.
 */
public final class RestartRequest {

    public static final String NOMBRE_ARCHIVO = "restart-request.json";

    private RestartRequest() {
    }

    /**
     * Escribe la peticion en la carpeta del plugin.
     *
     * @return la ruta escrita
     */
    public static Path escribir(Path carpetaPlugin, String solicitante, String motivo) throws IOException {
        Path destino = carpetaPlugin.resolve(NOMBRE_ARCHIVO);
        Files.createDirectories(carpetaPlugin);
        Files.writeString(destino, contenido(Instant.now(), solicitante, motivo), StandardCharsets.UTF_8);
        return destino;
    }

    /** Visible para pruebas: el JSON exacto, sin depender del reloj ni del disco. */
    static String contenido(Instant cuando, String solicitante, String motivo) {
        return "{\"solicitado\":\"" + cuando + "\","
                + "\"por\":\"" + escapar(solicitante) + "\","
                + "\"motivo\":\"" + escapar(motivo) + "\"}";
    }

    /** Escapa lo justo para que un nick o un motivo raro no rompan el JSON. */
    static String escapar(String texto) {
        if (texto == null) return "";
        StringBuilder salida = new StringBuilder(texto.length());
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '"' -> salida.append("\\\"");
                case '\\' -> salida.append("\\\\");
                case '\n', '\r', '\t' -> salida.append(' ');
                default -> {
                    if (c >= 0x20) salida.append(c);
                }
            }
        }
        return salida.toString();
    }
}
