package com.mce.recepcion;

/**
 * Constantes principales del proyecto
 * @author alexjcm
 */
public class BaseConstants {

    private BaseConstants() {
    }

    /**
     * Directorio en el que se almacenarán todos los pdfs firmados recibidos.
     */
    public static final String DIR_DOCS_FIRMADOS = "/opt/wildfly-static";

    /**
     * Valores retornados en las respuestas
     */
    public static final String RESP_OK = "OK";
    public static final String RESP_NOT_FOUND = "NOT_FOUND";
    public static final String RESP_ERROR = "ERROR";
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN_VALUE = "*";

    /**
     * Protocolos HTTP
     */
    public static final String HTTP_PROTOCOL = "http://";

    public static final String HTTPS_PROTOCOL = "https://";

    public static final String HEAD_METHOD = "HEAD";

    public static final String DOCUMENT_NAME_PARAM = "nombreDocumento";

    public static final String FILE_PARAM = "archivo";

}
