package com.mce.recepcion;

import java.io.File;
import java.io.StringReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Base64;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.GET;
import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonObject;
import javax.json.stream.JsonParsingException;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 *
 * @author alexjcm
 */
@Path("/rest")
public class ServicioDocFirmado {

    //En esta carpeta se almacenarán todos los pdfs firmados
    private static final String DIR_DOCS_FIRMADOS = "/opt/wildfly-static";

    private static final Logger logger = Logger.getLogger(ServicioDocFirmado.class.getName());

    /**
     * Permite recibir un json con el pdf firmado el cual es devuelto por el
     * sistema de firma-digital para luego ser obtenido desde el sistema MCE.
     *
     * Se debe recibir un json similar al siguiente: { "nombreDocumento":
     * "test.pdf", "archivo": "GxW3Rhj...", ...... }
     *
     * @param parametrosJson
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response grabarArchivoFirmado(String parametrosJson) {
        if (parametrosJson == null || parametrosJson.isEmpty()) {
            logger.log(Level.SEVERE, "Se debe incluir un JSON!");
            return Response.status(Status.BAD_REQUEST).entity("Se debe incluir JSON!").build();
        }

        JsonReader jsonReader = Json.createReader(new StringReader(parametrosJson));
        JsonObject json;
        try {
            json = (JsonObject) jsonReader.read();
            //json = jsonReader.readObject();
        } catch (JsonParsingException e) {
            logger.log(Level.SEVERE, "Error al decodificar JSON: ", e.getMessage());
            return Response.status(Status.BAD_REQUEST).entity("Error al decodificar JSON: " + e.getMessage()).build();
        }

        String nombreDoc;
        String docEnBase64;
        try {
            nombreDoc = json.getString("nombreDocumento");
            docEnBase64 = json.getString("archivo");
        } catch (NullPointerException e) {
            logger.log(Level.SEVERE, "Error al decodificar JSON: Se debe incluir nombreDocumento y archivo. ", e.getMessage());
            return Response.status(Status.BAD_REQUEST).entity("Error al decodificar JSON: Se debe incluir nombreDocumento y archivo.").build();
        }

        byte[] archivo = Base64.getDecoder().decode(docEnBase64);

        // Establecemos la carpeta y si no existe la creamos, recuerde que para 
        // que el servidor cree directorios debe tener permisos asignados.
        String carpeta = DIR_DOCS_FIRMADOS;
        File directorio = new File(carpeta);
        if (!directorio.exists()) {
            if (directorio.mkdirs()) {
                System.out.println("Directorio creado");
            } else {
                System.out.println("Error al crear directorio");
                logger.log(Level.WARNING, "Error al crear directorio: {0}", carpeta);
            }
        }

        String rutaDestinoArchivo = carpeta + "/" + nombreDoc; //Ruta completa del pdf
        try (OutputStream out = new FileOutputStream(rutaDestinoArchivo);) {
            // Grabamos el archivo en el servidor
            out.write(archivo);
            // Retorno de bandera para el servicio web de firma-digital         
            System.out.println("--> OK");
            return Response.ok("OK").build();

        } catch (IOException ex) {
            logger.log(Level.SEVERE, "ERROR -->{0}", ex.toString());
            return Response.ok("ERROR").build();
        }
    }

    /**
     * Permite verificar si el pdf firmado devuelto por el sistema firma-digital
     * existe. Para ello se debe enviar en un parámetro la ruta o url del pdf
     * firmado:
     *
     * ../recepcion/rest?dirpdf=/opt/wildfly-static/certificado1.pdf
     * ../recepcion/rest?dirpdf=http://0.0.0.0:8180/static/certificado1.pdf
     *
     * @param dirPdf
     * @return
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response existeArchivoFirmado(@QueryParam("dirpdf") String dirPdf) {
        // Primero verificamos si la direccion del pdf es: una Url o una Ruta.
        String aux = dirPdf.trim().toLowerCase();
        boolean esUrl = aux.startsWith("http://") || aux.startsWith("https://");
        if (esUrl) {
            try {
                System.out.println("dirPdf es una url.");
                HttpURLConnection.setFollowRedirects(false);
                HttpURLConnection con;
                con = (HttpURLConnection) new URL(dirPdf).openConnection();
                con.setRequestMethod("HEAD");

                //return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
                if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    return Response.ok("SI").build();
                }
                return Response.ok("NO").build();

            } catch (MalformedURLException ex) {
                logger.log(Level.SEVERE, ex.toString());
                return Response.ok("MalformedURLException --> ").build();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, ex.toString());
                return Response.ok("IOException --> ").build();
            }
        }

        // es una ruta
        System.out.println("dirPdf es una ruta.");
        File file = new File(dirPdf);
        if (file.isFile()) {
            return Response.ok("SI").build();
        }
        logger.log(Level.SEVERE, "El Pdf firmado no existe en el directorio: " + DIR_DOCS_FIRMADOS);
        return Response.ok("NO").build();
    }
}
