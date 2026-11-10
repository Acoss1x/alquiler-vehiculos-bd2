package co.edu.unbosque.alquiler.security;

import co.edu.unbosque.alquiler.exception.DemasiadosIntentosException;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita los intentos fallidos por clave (aquí, el correo): al llegar al máximo dentro de
 * la ventana de tiempo se rechaza todo intento hasta que los fallos más viejos expiren.
 * Vive en memoria, así que se reinicia al reiniciar el backend.
 */
@Component
public class LimitadorIntentos {

    private static final int MAXIMO_FALLOS = 5;
    private static final long VENTANA_MS = 15 * 60 * 1000L;
    private static final int TAMANO_PARA_LIMPIAR = 10_000;

    private final Map<String, Deque<Long>> fallos = new ConcurrentHashMap<>();

    /** Lanza {@link DemasiadosIntentosException} si la clave ya agotó sus intentos. */
    public void verificar(String clave) {
        Deque<Long> historial = fallos.get(clave);
        if (historial == null) {
            return;
        }
        synchronized (historial) {
            descartarVencidos(historial);
            if (historial.size() >= MAXIMO_FALLOS) {
                throw new DemasiadosIntentosException();
            }
        }
    }

    public void registrarFallo(String clave) {
        if (fallos.size() > TAMANO_PARA_LIMPIAR) {
            fallos.values().removeIf(h -> {
                synchronized (h) {
                    descartarVencidos(h);
                    return h.isEmpty();
                }
            });
        }
        Deque<Long> historial = fallos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (historial) {
            historial.addLast(System.currentTimeMillis());
        }
    }

    public void limpiar(String clave) {
        fallos.remove(clave);
    }

    private void descartarVencidos(Deque<Long> historial) {
        long limite = System.currentTimeMillis() - VENTANA_MS;
        while (!historial.isEmpty() && historial.peekFirst() < limite) {
            historial.removeFirst();
        }
    }
}