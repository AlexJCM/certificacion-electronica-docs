/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
 * Permite recibir el documento firmado devuelto por el sistema de firmadigital 
 * para luego ser recuperado desde el Módulo de certificación electrónica.
 * 
 * @author alexjcm
 */
@Path("/rest")
public class ServicioDocFirmado {

    //En esta carpeta se almacenarán todos los pdfs firmados
    private static final String DIR_DOCS_FIRMADOS = "/opt/wildfly-static";
    private static final String RESP_OK = "OK";
    private static final String RESP_NOT_FOUND = "NOT_FOUND";
    private static final String RESP_ERROR = "ERROR";

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
            return Response.status(Status.OK).entity(RESP_OK).build();

        } catch (IOException ex) {
            logger.log(Level.SEVERE, "ERROR -->{0}", ex.toString());
            return Response.status(Status.BAD_REQUEST).entity(RESP_ERROR).build();
        }
    }

    /**
     * Permite verificar si el pdf firmado devuelto por el sistema firma-digital
     * existe. Para ello se debe enviar en un parámetro la ruta o url del pdf
     * firmado:
     *
     * ../rest?dirpdf=/opt/wildfly-static/certificado1.pdf
     * ../rest?dirpdf=http://0.0.0.0:8180/static/certificado1.pdf
     *
     * @param dirPdf
     * @return
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response existeArchivoFirmado(@QueryParam("dirpdf") String dirPdf) {
        // Verificamos si la direccion del pdf es: una Url o una Ruta.
        String aux = dirPdf.trim().toLowerCase();
        boolean esUrl = aux.startsWith("http://") || aux.startsWith("https://");
        if (esUrl) {
            try {
                HttpURLConnection.setFollowRedirects(false);
                HttpURLConnection con;
                con = (HttpURLConnection) new URL(dirPdf).openConnection();
                con.setRequestMethod("HEAD");
                //return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
                if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    return Response.status(Status.OK).header("Access-Control-Allow-Origin", "*").entity(RESP_OK).build();
                }
                return Response.status(Status.NOT_FOUND).header("Access-Control-Allow-Origin", "*").entity(RESP_NOT_FOUND).build();

            } catch (MalformedURLException ex) {
                logger.log(Level.SEVERE, ex.toString());
                return Response.status(Status.BAD_REQUEST).header("Access-Control-Allow-Origin", "*")
                        .entity("ex" + ex.toString()).build();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, ex.toString());
                // BAD_REQUEST = 400
                return Response.status(Status.BAD_REQUEST).header("Access-Control-Allow-Origin", "*")
                        .entity("ex" + ex.toString()).build();
            }
        }

        File file = new File(dirPdf);
        if (file.isFile()) {
            return Response.status(Status.OK).header("Access-Control-Allow-Origin", "*").entity(RESP_OK).build();
        }
        logger.log(Level.SEVERE, "El Pdf firmado no existe en el directorio: " + DIR_DOCS_FIRMADOS);
        return Response.status(Status.NOT_FOUND).header("Access-Control-Allow-Origin", "*").entity(RESP_NOT_FOUND).build();
    }
}
