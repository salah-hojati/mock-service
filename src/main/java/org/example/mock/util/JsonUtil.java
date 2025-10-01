package org.example.mock.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonUtil {

    /**
     * Normalizes a JSON string by removing spaces and newlines outside of quotes.
     * This is a simple but effective way to make JSON strings comparable.
     * Returns the original string if it's null, empty, or not valid JSON.
     *
     * @param jsonString The JSON string to normalize.
     * @return A minified JSON string or the original string if not applicable.
     */
    private static final Logger LOGGER = Logger.getLogger(JsonUtil.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String normalize(String jsonString) {


        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null; // Return null for empty/null strings to match DB state
        }

        // A simple regex to remove whitespace that is not inside quotes.
        // This is generally safe for most JSON but can be replaced with a
        // more robust library-based approach if needed.

        try {
            // 1. Read the string into a generic JSON tree
            jsonString=jsonString.replaceAll("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", "").replace("\n","");
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            // 2. Write the tree back to a string. This naturally removes insignificant whitespace.
            return objectMapper.writeValueAsString(jsonNode);
        } catch (IOException e) {
            // This will catch malformed JSON, like your example
            LOGGER.log(Level.WARNING, "Failed to normalize JSON. String is likely not valid JSON. Returning original string. Input: " + jsonString, e.getMessage());
            // Return the original string if it's not valid JSON, so no data is lost.
            // Or you could return null if you prefer to discard invalid formats.
            return jsonString;
        }
       // return jsonString.replaceAll("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", "");
    }
}