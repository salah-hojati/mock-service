import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class JsonToJavaClassConverter {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String INDENT = "    ";
    
    /**
     * Main function to convert JSON to Java class code
     * @param jsonString JSON string to convert
     * @param className Name of the Java class to generate
     * @return Java class code as string
     */
    public static String convertJsonToJavaClass(String jsonString, String className) throws Exception {
        JsonNode rootNode = objectMapper.readTree(jsonString);
        StringBuilder javaCode = new StringBuilder();
        
        // Generate package and imports
        javaCode.append("import com.fasterxml.jackson.annotation.JsonProperty;\n");
        javaCode.append("import java.util.List;\n\n");
        
        // Start class definition
        javaCode.append("public class ").append(className).append(" {\n\n");
        
        // Generate fields and methods
        generateFieldsAndMethods(rootNode, className, javaCode, new HashSet<>());
        
        javaCode.append("}\n");
        return javaCode.toString();
    }
    
    /**
     * Generate fields, getters, and setters for JSON nodes
     */
    private static void generateFieldsAndMethods(JsonNode node, String className, 
                                                  StringBuilder javaCode, Set<String> processedClasses) {
        if (!processedClasses.add(className)) {
            return;
        }
        
        Map<String, String> fields = new LinkedHashMap<>();
        
        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = node.fields();
        while (fieldsIterator.hasNext()) {
            Map.Entry<String, JsonNode> field = fieldsIterator.next();
            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();
            
            String javaType = getJavaType(fieldValue,fieldName);



            fields.put(fieldName, javaType);
            
            // Generate field declaration
            javaCode.append(INDENT).append("@JsonProperty(\"").append(fieldName).append("\")\n");
            javaCode.append(INDENT).append("private ").append(javaType).append(" ");
            javaCode.append(convertToCamelCase(fieldName)).append(";\n\n");
        }
        
        // Generate getters and setters
        for (Map.Entry<String, String> field : fields.entrySet()) {
            String fieldName = field.getKey();
            String javaType = field.getValue();
            String camelFieldName = convertToCamelCase(fieldName);
            String capitalizedFieldName = capitalize(camelFieldName);
            
            // Getter
            javaCode.append(INDENT).append("public ").append(javaType).append(" get")
                    .append(capitalizedFieldName).append("() {\n");
            javaCode.append(INDENT).append(INDENT).append("return ").append(camelFieldName).append(";\n");
            javaCode.append(INDENT).append("}\n\n");
            
            // Setter
            javaCode.append(INDENT).append("public void set").append(capitalizedFieldName)
                    .append("(").append(javaType).append(" ").append(camelFieldName).append(") {\n");
            javaCode.append(INDENT).append(INDENT).append("this.").append(camelFieldName)
                    .append(" = ").append(camelFieldName).append(";\n");
            javaCode.append(INDENT).append("}\n\n");
        }
        
        // Handle nested objects
        for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> field = it.next();
            JsonNode fieldValue = field.getValue();
            if (fieldValue.isObject()) {
                String nestedClassName = capitalize(convertToCamelCase(field.getKey()));
                StringBuilder nestedClassCode = new StringBuilder();
                generateFieldsAndMethods(fieldValue, nestedClassName, nestedClassCode, processedClasses);
                javaCode.append(INDENT).append("// Nested class ").append(nestedClassName).append("\n");
                javaCode.append(INDENT).append("public static class ").append(nestedClassName).append(" {\n");
                javaCode.append(nestedClassCode.toString());
                javaCode.append(INDENT).append("}\n\n");
            } else if (fieldValue.isArray() && fieldValue.size() > 0 && fieldValue.get(0).isObject()) {
                String nestedClassName = capitalize(convertToCamelCase(field.getKey())) + "Item";
                StringBuilder nestedClassCode = new StringBuilder();
                generateFieldsAndMethods(fieldValue.get(0), nestedClassName, nestedClassCode, processedClasses);
                javaCode.append(INDENT).append("// Nested class for array items\n");
                javaCode.append(INDENT).append("public static class ").append(nestedClassName).append(" {\n");
                javaCode.append(nestedClassCode.toString());
                javaCode.append(INDENT).append("}\n\n");
            }
        }
    }
    
    /**
     * Determine Java type from JSON node
     */
    private static String getJavaType(JsonNode node, String fieldName) {
        if (node.isTextual()) {
            return "String";
        } else if (node.isInt()) {
            return "Integer";
        } else if (node.isLong()) {
            return "Long";
        } else if (node.isDouble() || node.isFloat()) {
            return "Double";
        } else if (node.isBoolean()) {
            return "Boolean";
        } else if (node.isArray()) {
            if (node.size() > 0) {
                JsonNode firstElement = node.get(0);
                if (firstElement.isObject()) {
                    return "List<" + capitalize(convertToCamelCase("item")) + ">";
                } else {
                    return "List<" + getJavaType(firstElement, fieldName) + ">";
                }
            } else {
                return "List<Object>";
            }
        } else if (node.isObject()) {
            //return "Object";
            return capitalize(fieldName);
        } else if (node.isNull()) {
            return "Object";
        }
        return "Object";
    }
    
    /**
     * Convert snake_case or kebab-case to camelCase
     */
    private static String convertToCamelCase(String input) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '_' || c == '-') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(c);
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * Capitalize first letter of string
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
    
    /**
     * Convenience method to convert JSON file to Java class
     */
    public static String convertJsonFileToJavaClass(String filePath, String className) throws Exception {
        String jsonString = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)));
        return convertJsonToJavaClass(jsonString, className);
    }
    
    // Example usage
    public static void main(String[] args) {
        try {
            String json = "{\n" +
                    "  \"id\": 1,\n" +
                    "  \"name\": \"John Doe\",\n" +
                    "  \"email\": \"john@example.com\",\n" +
                    "  \"age\": 30,\n" +
                    "  \"active\": true,\n" +
                    "  \"address\": {\n" +
                    "    \"street\": \"123 Main St\",\n" +
                    "    \"city\": \"New York\",\n" +
                    "    \"zipCode\": 10001\n" +
                    "  },\n" +
                    "  \"hobbies\": [\"reading\", \"gaming\"],\n" +
                    "  \"scores\": [85, 90, 95]\n" +
                    "}";

            String json2="{\n" +
                    "    \"result\": {\n" +
                    "        \"advocacyEndDate\": \"1404/06/29\",\n" +
                    "        \"baseDocuments\": [],\n" +
                    "        \"caseClasifyNo\": \"35469\",\n" +
                    "        \"docDate\": \"1404/06/17\",\n" +
                    "        \"docType\": \"سند وكالت کاري وسايل نقليه\",\n" +
                    "        \"docTypeCode\": \"323\",\n" +
                    "        \"existDoc\": true,\n" +
                    "        \"followerDocuments\": [],\n" +
                    "        \"hasPermission\": true,\n" +
                    "        \"impotrantAnnexText\": \"دفترخانه 21 نهاوند\\nنوع سند: سند وكالت کاري وسايل نقليه\\nتاریخ سند: 1404/06/17\\nشماره سند: 35469\\n--------------------------------------------------\\n کد ملی: 0055580203 - وكيل : خسرو ملايداله فركي\\n کد ملی: 3950606297 - موكل : رضا سراقي\\n--------------------------------------------------\\nموضوع سند: یک  دستگاه سواري-هاچ بك - داخلی - پژو - 207I-MT - 1404شماره شاسی: NAAR03HFFSDJ52240\\n--------------------------------------------------\\nاین وکالتنامه، بلاعزل است.\\nوکیل حق توکیل به غیر ندارد.\\nمهلت اعتبار این وکالتنامه تا تاریخ 1404/06/29 بوده است.\\n--------------------------------------------------\\nمتن سند: \\nدرخصوص مراجعه به راهنمايي ورانندگي ومراكزتعويض پلاك در سراسر  كشور واقدام به تعويض پلاك وسيله نقليه فوق الذكر بنام هركس ولو بنام خود وكيل يا هركس  كه وكيل معرفي نمايد اعم از اداري وتشريفات مورد لزوم به نحويكه در كليه موارد فوق نيازي به حضور وامضاءمجدد موكل مرقوم نباشد .\\rحدود اختيارات : وكيل مرقوم در مورد وكالت داراي اختيار تام و مطلقه ميباشد و اقدام و امضا وكيل بمنزله اقدام و امضا موكل صحيح و معتبر ميباشد و مفاد اين سند فقط در نفس وكالت موثر است\",\n" +
                    "        \"lstFindPersonInQuery\": [\n" +
                    "            {\n" +
                    "                \"agentType\": \"\",\n" +
                    "                \"birthdate\": \"1382/03/20\",\n" +
                    "                \"family\": \"سراقي\",\n" +
                    "                \"familyMovakel\": \"\",\n" +
                    "                \"name\": \"رضا\",\n" +
                    "                \"nameMovakel\": \"\",\n" +
                    "                \"nationalNo\": \"3950606297\",\n" +
                    "                \"nationalNoMovakel\": \"\",\n" +
                    "                \"personType\": \"حقیقی\",\n" +
                    "                \"personTypeCode\": \"1\",\n" +
                    "                \"roleType\": \"موكل\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"agentType\": \"\",\n" +
                    "                \"birthdate\": \"1350/01/01\",\n" +
                    "                \"family\": \"ملايداله فركي\",\n" +
                    "                \"familyMovakel\": \"\",\n" +
                    "                \"name\": \"خسرو\",\n" +
                    "                \"nameMovakel\": \"\",\n" +
                    "                \"nationalNo\": \"0055580203\",\n" +
                    "                \"nationalNoMovakel\": \"\",\n" +
                    "                \"personType\": \"حقیقی\",\n" +
                    "                \"personTypeCode\": \"1\",\n" +
                    "                \"roleType\": \"وكيل\"\n" +
                    "            }\n" +
                    "        ],\n" +
                    "        \"nationalRegisterNo\": \"140432353497000267\",\n" +
                    "        \"regCases\": [],\n" +
                    "        \"scriptoriumName\": \"دفترخانه اسناد رسمي شماره 21 شهر نهاوند استان همدان\",\n" +
                    "        \"succseed\": true\n" +
                    "    },\n" +
                    "    \"message\": {\n" +
                    "        \"code\": \"200\",\n" +
                    "        \"desc\": \"OK!\"\n" +
                    "    }\n" +
                    "}";
            
            String javaClass = convertJsonToJavaClass(json2, "Person");
            System.out.println(javaClass);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}