package com.mce.recepcion;

import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * @author alexjcm
 */
public class TestServicioDocFirmado {

    @Test
    public void testInvalidFile() throws Exception {
        ServicioDocFirmado servicioDocFirmado = new ServicioDocFirmado();
        try {
            // la dirección no existe
            servicioDocFirmado.checkSignedFileExists("/opt/-static/certificado1.pdf");
            System.out.println(" --> " + servicioDocFirmado.toString());
            //assertEquals("SI", servicioDocFirmado);
            fail();
        } catch (Exception e) {
        }
        try {
            // no se envia la dirección
            servicioDocFirmado.checkSignedFileExists("");
            System.out.println(" --> " + servicioDocFirmado.toString());
            //assertEquals("SI", servicioDocFirmado);
            fail();
        } catch (Exception e) {
        }
    }

    @Test
    public void testInvalidJsonWithoutDocumentName() throws Exception {
        ServicioDocFirmado servicioDocFirmado = new ServicioDocFirmado();
        try {
            // no es json
            servicioDocFirmado.saveSignedFile("");
            System.out.println(" --> " + servicioDocFirmado.toString());
            //assertEquals("SI", servicioDocFirmado);
            fail();

            // json imcompleto
            servicioDocFirmado.saveSignedFile("{\"archivo\":\"WDWFE3SGY8kqFD\"}");
            System.out.println(" --> " + servicioDocFirmado.toString());
            //assertEquals("SI", servicioDocFirmado);
            fail();
        } catch (Exception e) {
        }
    }

    @Test
    public void testInvalidJsonWithoutFile() throws Exception {
        ServicioDocFirmado servicioDocFirmado = new ServicioDocFirmado();
        try {
            // no es json
            servicioDocFirmado.saveSignedFile("");
            System.out.println(" --> " + servicioDocFirmado.toString());
            //assertEquals("SI", servicioDocFirmado);
            fail();

            // json imcompleto
            servicioDocFirmado.saveSignedFile("{\"nombreDocumento\":\"test.pdf\"}");
            System.out.println(" --> " + servicioDocFirmado.toString());
            //assertEquals("SI", servicioDocFirmado);
            fail();
        } catch (Exception e) {
        }
    }
}
