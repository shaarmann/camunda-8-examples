package com.camunda.consulting.rest;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/orderform")
public class OrderFormResource {

  @Inject
  Template base;

  @GET
  @Produces(MediaType.TEXT_HTML)
  public TemplateInstance get() {
    return base.instance();
  }
}
