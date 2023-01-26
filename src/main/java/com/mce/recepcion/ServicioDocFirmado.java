package com.mce.recepcion;

import static com.mce.recepcion.BaseConstants.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.mce.recepcion.BaseConstants.ACCESS_CONTROL_ALLOW_ORIGIN_VALUE;
import static com.mce.recepcion.BaseConstants.DIR_DOCS_FIRMADOS;
import static com.mce.recepcion.BaseConstants.DOCUMENT_NAME_PARAM;
import static com.mce.recepcion.BaseConstants.FILE_PARAM;
import static com.mce.recepcion.BaseConstants.HEAD_METHOD;
import static com.mce.recepcion.BaseConstants.HTTPS_PROTOCOL;
import static com.mce.recepcion.BaseConstants.HTTP_PROTOCOL;
import static com.mce.recepcion.BaseConstants.RESP_ERROR;
import static com.mce.recepcion.BaseConstants.RESP_NOT_FOUND;
import static com.mce.recepcion.BaseConstants.RESP_OK;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Servicio web REST que permite recibir y guardar en un directorio el documento firmado devuelto por
 * el sistema de firmadigital-servicio para luego ser recuperado desde el Módulo de Certificación
 * Electrónica (MCE).
 *
 * @author alexjcm
 */
@Path("/receiveDocument")
public class ServicioDocFirmado {

    private static final Logger logger = Logger.getLogger(ServicioDocFirmado.class.getName());

    /**
     * Permite recibir un json con el pdf firmado el cual es devuelto por el sistema de
     * firmadigital-servicio para luego ser obtenido desde el sistema MCE.
     * <p>
     * Se debe recibir un json similar al siguiente:
     * {
     *     "nombreDocumento": "test.pdf",
     *     "archivo": "GxW3Rhj...",
     *     ......
     * }
     *
     * @param dataJson
     * @return String con OK o ERROR
     */
    @POST
    @Path("/saveSignedFile")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response saveSignedFile(String dataJson) {
        if (dataJson == null || dataJson.isEmpty()) {
            logger.log(Level.SEVERE, "Se debe incluir un JSON!");
            return Response.status(Status.BAD_REQUEST).entity("Se debe incluir JSON!").build();
        }

        JsonReader jsonReader = Json.createReader(new StringReader(dataJson));
        JsonObject json;
        try {
            json = (JsonObject) jsonReader.read();
        } catch (JsonParsingException e) {
            logger.log(Level.SEVERE, "Error al decodificar JSON: {0}", e.getMessage());
            return Response.status(Status.BAD_REQUEST)
                .entity("Error al decodificar JSON: " + e.getMessage()).build();
        } finally {
            jsonReader.close();
        }

        String documentName;
        String documentInString;
        try {
            documentName = json.getString(DOCUMENT_NAME_PARAM);
            documentInString = json.getString(FILE_PARAM);
        } catch (NullPointerException e) {
            logger.log(Level.SEVERE,
                "Error al decodificar JSON: Se debe incluir nombreDocumento y archivo. {0} ",
                e.getMessage());
            return Response.status(Status.BAD_REQUEST)
                .entity("Error al decodificar JSON: Se debe incluir nombreDocumento y archivo.")
                .build();
        }

        byte[] archivo = Base64.getDecoder().decode(documentInString);

        // Establecemos la carpeta y si no existe la creamos, recuerde que para 
        // que el servidor cree directorios debe tener permisos asignados.
        String pathNameDcumentsSigned = DIR_DOCS_FIRMADOS;
        File directory = new File(pathNameDcumentsSigned);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                logger.log(Level.INFO, "Directorio creado con éxito");
            } else {
                logger.log(Level.SEVERE, "Error al crear directorio: {0}", pathNameDcumentsSigned);
            }
        }

        //Ruta completa del pdf
        String finalDestinationPath = pathNameDcumentsSigned + "/" + documentName;
        try (OutputStream out = new FileOutputStream(finalDestinationPath);) {
            // Grabamos el archivo en el servidor
            out.write(archivo);
            // Retorno de bandera para el servicio web de firmadigital-servicio
            return Response.status(Status.OK).entity(RESP_OK).build();

        } catch (IOException ex) {
            logger.log(Level.SEVERE, "ERROR -->{0}", ex.toString());
            return Response.status(Status.BAD_REQUEST).entity(RESP_ERROR).build();
        }
    }

    /**
     * Permite verificar si el pdf firmado devuelto por el sistema firmadigital-servicio existe.
     * Para ello se debe enviar en un parámetro la ruta en el servidor o url del pdf firmado.
     * Por ejemplo:
     *  Ruta del pdf en el servidor:
     *  /rest?dirpdf=/opt/wildfly-static/certificado1.pdf
     *
     *  Url del pdf:
     *  /rest?dirpdf=http://pruebasmce.info/firmaec/static/certificado1.pdf
     *
     * @param pdfLocation
     * @return
     */
    @GET
    @Path("/checkSignedFileExists")
    @Produces(MediaType.TEXT_PLAIN)
    public Response checkSignedFileExists(@QueryParam("location") String pdfLocation) {
        logger.log(Level.INFO, "pdfLocation: {}", pdfLocation);
        // Verificamos si la direccion del pdf es: una Url o una Ruta.
        String urlOrPathConvertedTemp = pdfLocation.trim().toLowerCase();
        boolean isAUrl = urlOrPathConvertedTemp.startsWith(HTTP_PROTOCOL) || urlOrPathConvertedTemp.startsWith(
                HTTPS_PROTOCOL);

        if (isAUrl) {
            try {
                HttpURLConnection.setFollowRedirects(false);
                HttpURLConnection con;
                con = (HttpURLConnection) new URL(pdfLocation).openConnection();
                con.setRequestMethod(HEAD_METHOD);
                if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    return Response.status(Status.OK)
                        .header(ACCESS_CONTROL_ALLOW_ORIGIN, ACCESS_CONTROL_ALLOW_ORIGIN_VALUE)
                        .entity(RESP_OK).build();
                }

                logger.log(Level.WARNING, "La url del pdf firmado no existe: {}", pdfLocation);
                return Response.status(Status.NOT_FOUND)
                    .header(ACCESS_CONTROL_ALLOW_ORIGIN, ACCESS_CONTROL_ALLOW_ORIGIN_VALUE)
                    .entity(RESP_NOT_FOUND).build();

            } catch (Exception ex) {
                logger.log(Level.SEVERE, ex.toString());
                return Response.status(Status.BAD_REQUEST)
                    .header(ACCESS_CONTROL_ALLOW_ORIGIN, ACCESS_CONTROL_ALLOW_ORIGIN_VALUE)
                    .entity("Error: " + ex).build();
            }
        }

        File file = new File(pdfLocation);
        if (file.isFile()) {
            return Response.status(Status.OK)
                .header(ACCESS_CONTROL_ALLOW_ORIGIN, ACCESS_CONTROL_ALLOW_ORIGIN_VALUE)
                .entity(RESP_OK).build();
        }
        logger.log(Level.SEVERE, "El pdf firmado no existe en el directorio: " + DIR_DOCS_FIRMADOS);
        return Response.status(Status.NOT_FOUND)
            .header(ACCESS_CONTROL_ALLOW_ORIGIN, ACCESS_CONTROL_ALLOW_ORIGIN_VALUE)
            .entity(RESP_NOT_FOUND).build();
    }

    @GET
    @Path("/test")
    public Response testService() {
        return Response.ok("Hello world!").build();
    }
}
