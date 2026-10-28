package co.edu.unbosque.alquiler.service;

import co.edu.unbosque.alquiler.dto.InspeccionResponse;
import co.edu.unbosque.alquiler.exception.RecursoNoEncontradoException;
import co.edu.unbosque.alquiler.exception.SolicitudInvalidaException;
import co.edu.unbosque.alquiler.repository.InspeccionConsultasRepository;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Fotos de las inspecciones. El archivo se guarda en GridFS y el documento de la
 * inspección solo recibe su identificador.
 */
@Service
public class InspeccionFotoService {

    public static final int MAX_FOTOS = 10;
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONES =
            Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");

    /** Contenido y tipo de una foto lista para descargar. */
    public record Archivo(byte[] datos, String tipoContenido) {
    }

    private final GridFsTemplate gridFsTemplate;
    private final InspeccionConsultasRepository consultasRepository;
    private final InspeccionService inspeccionService;

    public InspeccionFotoService(GridFsTemplate gridFsTemplate,
                                 InspeccionConsultasRepository consultasRepository,
                                 InspeccionService inspeccionService) {
        this.gridFsTemplate = gridFsTemplate;
        this.consultasRepository = consultasRepository;
        this.inspeccionService = inspeccionService;
    }

    public InspeccionResponse agregar(Long idVehiculo, String idInspeccion, MultipartFile archivo) {
        InspeccionResponse inspeccion = inspeccionService.obtener(idVehiculo, idInspeccion);
        if (inspeccion.fotos().size() >= MAX_FOTOS) {
            throw new SolicitudInvalidaException("Una inspección admite máximo " + MAX_FOTOS + " fotos");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new SolicitudInvalidaException("Debe enviar un archivo de imagen");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new SolicitudInvalidaException("La foto supera el tamaño máximo de 5 MB");
        }

        byte[] datos;
        try {
            datos = archivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String tipo = tipoPorContenido(datos);
        if (tipo == null) {
            throw new SolicitudInvalidaException("El archivo no es una imagen válida (use JPEG, PNG o WebP)");
        }

        ObjectId id = gridFsTemplate.store(new ByteArrayInputStream(datos),
                UUID.randomUUID() + "." + EXTENSIONES.get(tipo), tipo, new Document("idInspeccion", idInspeccion));

        boolean agregada;
        try {
            agregada = consultasRepository.agregarFoto(idInspeccion, id.toHexString(), MAX_FOTOS);
        } catch (RuntimeException ex) {
            borrarArchivo(id);
            throw ex;
        }
        if (!agregada) {   // otra subida simultánea llenó el cupo
            borrarArchivo(id);
            throw new SolicitudInvalidaException("Una inspección admite máximo " + MAX_FOTOS + " fotos");
        }
        return inspeccionService.obtener(idVehiculo, idInspeccion);
    }

    public InspeccionResponse quitar(Long idVehiculo, String idInspeccion, String idFoto) {
        InspeccionResponse inspeccion = inspeccionService.obtener(idVehiculo, idInspeccion);
        if (!inspeccion.fotos().contains(idFoto)) {
            throw new RecursoNoEncontradoException("La inspección no tiene la foto " + idFoto);
        }
        consultasRepository.quitarFoto(idInspeccion, idFoto);
        borrarArchivo(new ObjectId(idFoto));
        return inspeccionService.obtener(idVehiculo, idInspeccion);
    }

    public Archivo obtener(String idFoto) {
        if (!ObjectId.isValid(idFoto)) {
            throw new RecursoNoEncontradoException("No existe la foto " + idFoto);
        }
        GridFSFile archivo = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(new ObjectId(idFoto))));
        if (archivo == null) {
            throw new RecursoNoEncontradoException("No existe la foto " + idFoto);
        }
        GridFsResource recurso = gridFsTemplate.getResource(archivo);
        try (InputStream in = recurso.getInputStream()) {
            return new Archivo(in.readAllBytes(), recurso.getContentType());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void borrarArchivo(ObjectId id) {
        gridFsTemplate.delete(Query.query(Criteria.where("_id").is(id)));
    }

    /** Reconoce JPEG, PNG y WebP por sus primeros bytes; null si no es ninguno. */
    private String tipoPorContenido(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') {
            return "image/png";
        }
        if (d.length >= 12 && "RIFF".equals(new String(d, 0, 4, StandardCharsets.US_ASCII))
                && "WEBP".equals(new String(d, 8, 4, StandardCharsets.US_ASCII))) {
            return "image/webp";
        }
        return null;
    }
}