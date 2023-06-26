package com.cmd;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.jboss.resteasy.reactive.RestSseElementType;

@Path("/customer")
public class CustomerMasterResource {

    @Inject
    CustomerMasterService service;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/info/{name}")
    public Uni<String> info(String name) {
        return service.info(name);
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/info/{count}/{name}")
    public Multi<String> info(int count, String name) {
        return service.info(count, name);
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestSseElementType(MediaType.TEXT_PLAIN)
    @Path("/stream/{count}/{name}")
    public Multi<String> greetingsAsStream(int count, String name) {
        return service.info(count, name);
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }
}
