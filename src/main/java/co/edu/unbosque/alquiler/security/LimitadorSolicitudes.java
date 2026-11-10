package co.edu.unbosque.alquiler.security;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita cuántas solicitudes de recuperación se aceptan por correo en una ventana de tiempo
 * (evita que alguien use el sistema para llenar de correos la bandeja de otra persona).
 * Vive en memoria, así que se reinicia al reiniciar el backend.
 */
@Component
public class LimitadorSolicitudes {

    private static final int MAXIMO = 3;
    private static final long VENTANA_MS = 15 * 60 * 1000L;
    private static final int TAMANO_PARA_LIMPIAR = 10_000;

    private final Map<String, Deque<Long>> solicitudes = new ConcurrentHashMap<>();

    /** Registra la solicitud y dice si se acepta (false = se superó el máximo en la ventana). */
    public boolean permitir(String clave) {
        if (solicitudes.size() > TAMANO_PARA_LIMPIAR) {
            solicitudes.values().removeIf(h -> {
                synchronized (h) {
                    descartarVencidas(h);
                    return h.isEmpty();
                }
            });
        }
        Deque<Long> historial = solicitudes.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (historial) {
            descartarVencidas(historial);
            if (historial.size() >= MAXIMO) {
                return false;
            }
            historial.addLast(System.currentTimeMillis());
            return true;
        }
    }

    private void descartarVencidas(Deque<Long> historial) {
        long limite = System.currentTimeMillis() - VENTANA_MS;
        while (!historial.isEmpty() && historial.peekFirst() < limite) {
            historial.removeFirst();
        }
    }
}
