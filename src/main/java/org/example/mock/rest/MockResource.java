package org.example.mock.rest;

import org.example.mock.entity.MockConfig;
import org.example.mock.service.MockConfigService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gson.Gson;

@Path("/mock")
public class MockResource {

    private static final Logger LOGGER = Logger.getLogger(MockResource.class.getName());
    private static final Gson gson = new Gson();

    @Inject
    private MockConfigService mockConfigService;

    @GET
    @Path("/{urlPattern:.+}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN,MediaType.APPLICATION_FORM_URLENCODED})
    public Response handleMockGet(@PathParam("urlPattern") String urlPattern) {
        return handleMockRequest("GET", urlPattern, null);
    }

    @POST
    @Path("/{urlPattern:.+}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN,MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN,MediaType.APPLICATION_FORM_URLENCODED})
    public Response handleMockPost(@PathParam("urlPattern") String urlPattern, @Context HttpHeaders headers, String requestBody) {


        // بررسی Content-Type
        MediaType contentType = headers.getMediaType();
        String processedBody = requestBody;

        // اگر فرم-urlencoded بود، به JSON تبدیل کن
        if (contentType != null && MediaType.APPLICATION_FORM_URLENCODED_TYPE.isCompatible(contentType)) {
            processedBody = convertFormToJson(requestBody);
            LOGGER.info("Converted form-urlencoded to JSON: " + processedBody);
        }



        return handleMockRequest("POST", urlPattern, requestBody);
    }

    @PUT
    @Path("/{urlPattern:.+}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN,MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN,MediaType.APPLICATION_FORM_URLENCODED})
    public Response handleMockPut(@PathParam("urlPattern") String urlPattern, @Context HttpHeaders headers,String requestBody) {




        MediaType contentType = headers.getMediaType();
        String processedBody = requestBody;

        if (contentType != null && MediaType.APPLICATION_FORM_URLENCODED_TYPE.isCompatible(contentType)) {
            processedBody = convertFormToJson(requestBody);
            LOGGER.info("Converted form-urlencoded to JSON: " + processedBody);
        }


        return handleMockRequest("PUT", urlPattern, requestBody);
    }

    @DELETE
    @Path("/{urlPattern:.+}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN,MediaType.APPLICATION_FORM_URLENCODED})
    public Response handleMockDelete(@PathParam("urlPattern") String urlPattern) {
        return handleMockRequest("DELETE", urlPattern, null);
    }

    /**
     * Central logic to find and process a mock request.
     */
    private Response handleMockRequest(String httpMethod, String urlPattern, String requestBody) {
        MockConfig mock = mockConfigService.findMockConfig(httpMethod, urlPattern, requestBody);

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
                    "{\"error\": \"No mock configuration found for method '%s', URL pattern '%s', and the provided request body.\"}",
                    httpMethod, urlPattern
            );
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorMessage)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }


    // متد کمکی برای تبدیل
    private String convertFormToJson(String formData) {
        if (formData == null || formData.isEmpty()) {
            return null;
        }

        Map<String, String> params = new HashMap<>();
        String[] pairs = formData.split("&");

        for (String pair : pairs) {
            if (pair.contains("=")) {
                String[] keyValue = pair.split("=", 2);
                try {
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.name());
                    String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name()) : "";
                    params.put(key, value);
                } catch (Exception e) {
                    LOGGER.warning("Error decoding form parameter: " + e.getMessage());
                }
            }
        }

        return gson.toJson(params);
    }

}