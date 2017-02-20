package org.onepointltd.json.converter;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

/**
 * Contains the data structures with the context extracted from the Mongo patch.
 */
public class MongoConversion {

    private final Set<String> unset = new HashSet<>();

    private final Map<String, JsonNode> set = new HashMap<>();

    private final Map<String, List<JsonNode>> push = new HashMap<>();

    MongoConversion() {
    }

    public Set<String> getUnset() {
        return unset;
    }

    public Map<String, JsonNode> getSet() {
        return set;
    }

    public Map<String, List<JsonNode>> getPush() {
        return push;
    }
}
