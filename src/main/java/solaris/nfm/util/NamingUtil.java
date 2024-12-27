package solaris.nfm.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NamingUtil {
    public static JsonNode xmlTagNameConvertToCamelCase(JsonNode input) {
        Matcher matchKey = Pattern.compile("\"(.+?)\" ?: ?").matcher(input.toPrettyString());
        String result = matchKey.replaceAll(s -> {
            Matcher matchDash = Pattern.compile("-[a-zA-Z]").matcher(s.group().toLowerCase());
            return matchDash.replaceAll(d -> d.group().substring(1).toUpperCase());
        });
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readTree(result);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JsonNode removeNamespaceInKey(JsonNode input) {
        if (input.isArray()) {
            ArrayNode arrayNode = (ArrayNode) input;
            for (JsonNode jsonNode : arrayNode) {
                removeNamespaceInKeyInObject(jsonNode);
            }
        } else if (input.isObject()) {
            removeNamespaceInKeyInObject(input);
        }
        return input;
    }

    private static JsonNode removeNamespaceInKeyInObject(JsonNode input) {
        if (!input.isObject()) {
            return input;
        }
        ObjectNode objectNode = (ObjectNode) input;
        Iterator<String> fieldNames = objectNode.fieldNames();
        List<String> fieldNameList = new ArrayList<>();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            fieldNameList.add(fieldName);
        }
        for (String fieldName : fieldNameList) {
            JsonNode value = objectNode.get(fieldName);
            if (value.isArray() || value.isObject()) {
                removeNamespaceInKey(value);
            }
            if (fieldName.contains(":")) {
                String[] split = fieldName.split(":");
                objectNode.set(split[1], value);
                objectNode.remove(fieldName);
            }
        }
        return objectNode;
    }
}