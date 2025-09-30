package org.example.mock.rest;

import org.example.mock.entity.GatewayConfig;
import org.example.mock.entity.GatewayLog;
import org.example.mock.service.GatewayService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/gateway")
public class GatewayResource {

    @Inject
    private GatewayService gatewayService;

    // --- JAX-RS Resource Methods ---
    // Each HTTP verb gets its own method, which then delegates to the private proxyRequest method.

    @GET
    @Path("/{sourceUrlPattern:.+}")
    public Response handleGet(
            @PathParam("sourceUrlPattern") String sourceUrlPattern,
            @Context HttpHeaders headers,
            @Context UriInfo uriInfo) {
        // For GET requests, the body is always null.
        return proxyRequest("GET", sourceUrlPattern, headers, uriInfo, null);
    }

    @POST
    @Path("/{sourceUrlPattern:.+}")
    public Response handlePost(
            @PathParam("sourceUrlPattern") String sourceUrlPattern,
            @Context HttpHeaders headers,
            @Context UriInfo uriInfo,
            String requestBody) {
        return proxyRequest("POST", sourceUrlPattern, headers, uriInfo, requestBody);
    }

    @PUT
    @Path("/{sourceUrlPattern:.+}")
    public Response handlePut(
            @PathParam("sourceUrlPattern") String sourceUrlPattern,
            @Context HttpHeaders headers,
            @Context UriInfo uriInfo,
            String requestBody) {
        return proxyRequest("PUT", sourceUrlPattern, headers, uriInfo, requestBody);
    }

    @DELETE
    @Path("/{sourceUrlPattern:.+}")
    public Response handleDelete(
            @PathParam("sourceUrlPattern") String sourceUrlPattern,
            @Context HttpHeaders headers,
            @Context UriInfo uriInfo) {
        // DELETE requests typically don't have a body, but we pass null for consistency.
        return proxyRequest("DELETE", sourceUrlPattern, headers, uriInfo, null);
    }

    @OPTIONS
    @Path("/{sourceUrlPattern:.+}")
    public Response handleOptions(
            @PathParam("sourceUrlPattern") String sourceUrlPattern,
            @Context HttpHeaders headers,
            @Context UriInfo uriInfo) {
        return proxyRequest("OPTIONS", sourceUrlPattern, headers, uriInfo, null);
    }

    @HEAD
    @Path("/{sourceUrlPattern:.+}")
    public Response handleHead(
            @PathParam("sourceUrlPattern") String sourceUrlPattern,
            @Context HttpHeaders headers,
            @Context UriInfo uriInfo) {
        return proxyRequest("HEAD", sourceUrlPattern, headers, uriInfo, null);
    }


    /**
     * Private method containing the core proxy logic. It's called by the public JAX-RS methods.
     */
    private Response proxyRequest(
            String method,
            String sourceUrlPattern,
            HttpHeaders headers,
            UriInfo uriInfo,
            String requestBody) {

        GatewayConfig config = gatewayService.findActiveConfigBySourcePattern(sourceUrlPattern);

        if (config == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"No active gateway configuration found for pattern: " + sourceUrlPattern + "\"}")
                    .build();
        }

        long startTime = System.currentTimeMillis();
        GatewayLog log = new GatewayLog();
        log.setGatewayConfig(config);
        log.setTimestamp(Timestamp.from(Instant.now()));
        log.setRequestBody(requestBody);
        log.setRequestMethod(method);

        HttpURLConnection connection = null;
        try {
            // 1. Construct the Target URL
            String targetUrl = buildTargetUrl(config.getTargetBaseUrl(), uriInfo);
            log.setRequestUrl(targetUrl);

            URL url = new URL(targetUrl);
            connection = (HttpURLConnection) url.openConnection();

            // 2. Set the method and basic properties
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10000); // 10 seconds
            connection.setReadTimeout(10000);    // 10 seconds
            connection.setInstanceFollowRedirects(false); // Important for proxying

            // 3. Copy Headers from incoming request to outgoing request
            String allRequestHeaders = copyHeaders(headers, connection);
            log.setRequestHeaders(allRequestHeaders);

            // 4. Write request body for methods that support it (e.g., POST, PUT)
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                if (requestBody != null && !requestBody.isEmpty()) {
                    connection.setDoOutput(true);
                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }
                }
            }

            // 5. Get the response from the real server
            int responseCode = connection.getResponseCode();
            log.setResponseStatusCode(responseCode);

            // 6. Read the response body
            InputStream responseStream = (responseCode < 400) ? connection.getInputStream() : connection.getErrorStream();
            String responseBody = "";
            if (responseStream != null) {
                // For HEAD requests, there is no body, so avoid trying to read it.
                if (!"HEAD".equalsIgnoreCase(method)) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream, StandardCharsets.UTF_8))) {
                        responseBody = reader.lines().collect(Collectors.joining("\n"));
                    }
                }
            }
            log.setResponseBody(responseBody);

            // 7. Log response details
            log.setResponseHeaders(formatHeaders(connection.getHeaderFields()));
            log.setDurationMs(System.currentTimeMillis() - startTime);

            // 8. Build the final response to send back to the original client
            Response.ResponseBuilder clientResponseBuilder = Response.status(responseCode).entity(responseBody);
            connection.getHeaderFields().forEach((key, values) -> {
                // The key can be null for the HTTP status line, which we should ignore.
                // Also filter out headers that can cause issues.
                if (key != null && !key.equalsIgnoreCase("transfer-encoding")) {
                    values.forEach(value -> clientResponseBuilder.header(key, value));
                }
            });

            return clientResponseBuilder.build();

        } catch (Exception e) {
            log.setResponseStatusCode(500);
            log.setResponseBody("Gateway Error: " + e.getMessage());
            log.setDurationMs(System.currentTimeMillis() - startTime);
            // In case of an exception, always save the log before returning an error
            gatewayService.saveLog(log);
            return Response.serverError().entity("{\"error\":\"Gateway failed to process request.\", \"details\":\"" + e.getMessage() + "\"}").build();
        } finally {
            // 9. Save the log entry (if not already saved in an exception) and disconnect
            if (log.getResponseStatusCode() != 500) { // Avoid double-saving on error
                gatewayService.saveLog(log);
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String buildTargetUrl(String targetBase, UriInfo uriInfo) {
        // This logic is more robust to get the path after the source pattern
        String fullPathFromRequest = uriInfo.getPath();
        String sourcePattern = uriInfo.getPathParameters().getFirst("sourceUrlPattern");
        int patternIndex = fullPathFromRequest.indexOf(sourcePattern);
        String remainingPath = fullPathFromRequest.substring(patternIndex + sourcePattern.length());

        String targetUrl = targetBase.endsWith("/") ? targetBase.substring(0, targetBase.length() - 1) : targetBase;
        targetUrl += remainingPath;

        String query = uriInfo.getRequestUri().getQuery();
        return (query != null && !query.isEmpty()) ? targetUrl + "?" + query : targetUrl;
    }

    private String copyHeaders(HttpHeaders incomingHeaders, HttpURLConnection connection) {
        StringBuilder headerLog = new StringBuilder();
        incomingHeaders.getRequestHeaders().forEach((key, values) -> {
            // Don't copy the Host header; the HttpURLConnection sets it from the URL.
            // Content-Length is also managed automatically when we write the body.
            if (!key.equalsIgnoreCase("host") && !key.equalsIgnoreCase("content-length")) {
                values.forEach(value -> {
                    connection.addRequestProperty(key, value);
                    headerLog.append(key).append(": ").append(value).append("\n");
                });
            }
        });
        return headerLog.toString();
    }

    private String formatHeaders(Map<String, List<String>> headers) {
        if (headers == null) {
            return "";
        }
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey() != null) // The status line can have a null key
                .map(entry -> entry.getKey() + ": " + String.join(", ", entry.getValue()))
                .collect(Collectors.joining("\n"));
    }
}