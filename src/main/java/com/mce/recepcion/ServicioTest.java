package com.mce.recepcion;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author alexjcm
 */
@Path("/test")
public class ServicioTest {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response testServicio() {
        return Response.ok("Test desde rest-recepcion").build();
    }
}
