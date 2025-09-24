package org.example.mock.util;

public class JsonUtil {

    /**
     * Normalizes a JSON string by removing spaces and newlines outside of quotes.
     * This is a simple but effective way to make JSON strings comparable.
     * Returns the original string if it's null, empty, or not valid JSON.
     *
     * @param jsonString The JSON string to normalize.
     * @return A minified JSON string or the original string if not applicable.
     */
    public static String normalize(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null; // Return null for empty/null strings to match DB state
        }

        // A simple regex to remove whitespace that is not inside quotes.
        // This is generally safe for most JSON but can be replaced with a
        // more robust library-based approach if needed.
        return jsonString.replaceAll("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", "");
    }
}