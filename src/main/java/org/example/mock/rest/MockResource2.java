package org.example.mock.rest;

import org.example.mock.entity.MockConfig2;
import org.example.mock.service.MockConfigService2;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/mock2")
public class MockResource2 {

    private static final Logger LOGGER = Logger.getLogger(MockResource2.class.getName());

    @Inject
    private MockConfigService2 mockConfigService;

    @GET
    @Path("/{urlPattern:.+}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response handleMockGet(@PathParam("urlPattern") String urlPattern) {
        return handleMockRequest("GET", urlPattern);
    }

    @POST
    @Path("/{urlPattern:.+}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response handleMockPost(@PathParam("urlPattern") String urlPattern) {
        return handleMockRequest("POST", urlPattern);
    }

    @PUT
    @Path("/{urlPattern:.+}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response handleMockPut(@PathParam("urlPattern") String urlPattern) {
        return handleMockRequest("PUT", urlPattern);
    }

    @DELETE
    @Path("/{urlPattern:.+}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response handleMockDelete(@PathParam("urlPattern") String urlPattern) {
        return handleMockRequest("DELETE", urlPattern);
    }

    /**
     * Central logic to find and process a mock request.
     */
    private Response handleMockRequest(String httpMethod, String urlPattern) {
        MockConfig2 mock = mockConfigService.findMockConfig(httpMethod, urlPattern);

        if (mock != null) {
            // Delay Logic
            Integer delay = mock.getDelayMs();
            if (delay != null && delay > 0) {
                try {
                    LOGGER.info(String.format("Delaying response for %dms for %s %s", delay, httpMethod, urlPattern));
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return Response.serverError().entity("{\"error\": \"Thread interrupted during delay\"}").build();
                }
            }

            // HTTP Status Code Logic
            LOGGER.info(String.format("Returning status %d for %s %s", mock.getHttpStatusCode(), httpMethod, urlPattern));
            return Response.status(mock.getHttpStatusCode())
                    .entity(mock.getResponsePayload())
                    .build();
        } else {
            // No mock found
            String errorMessage = String.format(
                    "{\"error\": \"No mock configuration found for method '%s' and URL pattern '%s'.\"}",
                    httpMethod, urlPattern
            );
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorMessage)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}