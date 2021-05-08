package com.mce.recepcion;

import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author alexjcm
 */
public class TestServicioDocFirmado {

    @Test
    public void testArchivoInvalido() throws Exception {
        ServicioDocFirmado servicioDocFirmado = new ServicioDocFirmado();
        try {
            // la dirección no existe
            servicioDocFirmado.existeArchivoFirmado("/opt/-static/certificado1.pdf");
            System.out.println(" --> " + servicioDocFirmado.toString());
            //assertEquals("SI", servicioDocFirmado);
            fail();
        } catch (Exception e) {
        }
        try {
            // no se envia la dirección
            servicioDocFirmado.existeArchivoFirmado("");
            System.out.println(" --> " + servicioDocFirmado.toString());
            //assertEquals("SI", servicioDocFirmado);
            fail();
        } catch (Exception e) {
        }
    }
    
    @Test
    public void testJsonInvalido() throws Exception {
        ServicioDocFirmado servicioDocFirmado = new ServicioDocFirmado();
        try {
            // no es json
            servicioDocFirmado.grabarArchivoFirmado("");
            System.out.println(" --> " + servicioDocFirmado.toString());
            //assertEquals("SI", servicioDocFirmado);
            fail();
        } catch (Exception e) {
        }
        try {
            // json imcompleto
            servicioDocFirmado.grabarArchivoFirmado("{\"archivo\":\"WDWFE3SGY8kqFD\"}");
            System.out.println(" --> " + servicioDocFirmado.toString());
            //assertEquals("SI", servicioDocFirmado);
            fail();
        } catch (Exception e) {
        }
    }
}
