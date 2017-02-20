package org.onepointltd.json.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.function.BiConsumer;

import static java.util.stream.IntStream.range;


/**
 * Converts RFC 6902 JSON style patches into the Mongo update format.
 */
public class JsonPatchToMongoUpdate {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Converts the JSON patch to a set of data structures. Note that the add method might aggregate
     * multiple add patches
     * @param reader Used to read the JSON patch.
     * @return a data structure which can be used to generate the Mongo update format statements.
     * @throws IOException In case reading fails.
     */
    public MongoConversion convert(Reader reader) throws IOException {
        JsonNode patch = mapper.readTree(reader);
        MongoConversion mongoConversion = new MongoConversion();
        Set<String> unset = mongoConversion.getUnset();
        Map<String, JsonNode> set = mongoConversion.getSet();
        Map<String, List<JsonNode>> push = mongoConversion.getPush();
        patch.forEach(p -> {
            String text = p.get("op").asText();
            String path = toDot(p.get("path").asText());
            JsonNode value = p.get("value");
            switch (text) {
                case "add": {
                    path = path.replaceFirst("\\.\\d+$", "");
                    push.computeIfAbsent(path, (key) -> new ArrayList<>()).add(value);
                    break;
                }
                case "remove": {
                    unset.add(path);
                    break;
                }
                case "replace": {
                    set.put(path, value);
                }
            }
        });
        return mongoConversion;
    }

    /**
     * Converts from the JSON patch format directly to a MongoDB query.
     * @param reader The reader used to read the JSON patch.
     * @param query The query used to find all the elements in the database, like e.g: ""_id": ObjectId("58a46cc6f7076692b7693c4e")"
     * @param collection The name of the collection which is being updated.
     * @param prefixes The prefixes for the json elements. If a field is called "notes.note", then with the prefix "customer"
     *                 the field will be converted to "customer.notes.note"
     * @return an object with the MongoDB commands.
     * @throws IOException the JSON cannot be read.
     */
    public MongoCommands convertToMongoUpdates(Reader reader, String query, String collection, String ... prefixes)
            throws IOException {
        MongoConversion mongoConversion = convert(reader);
        MongoCommands rawCommands = convertToMongo(mongoConversion, prefixes);
        return createMongoUpdate(rawCommands, query, collection);
    }

    MongoCommands convertToMongo(MongoConversion mongoConversion, String... prefixes) {
        String prefixStr = prefixes == null || prefixes.length == 0 ? "" : String.join(".", prefixes);
        prefixStr = prefixStr.isEmpty() ? "" : prefixStr + ".";
        String set = createSet(mongoConversion.getSet(), prefixStr);
        String unset = createUnset(mongoConversion.getUnset(), prefixStr);
        String push = createPush(mongoConversion.getPush(), prefixStr);
        return new MongoCommands(unset, set, push);
    }

    private MongoCommands createMongoUpdate(MongoCommands mongoCommands, String query, String collection) {
        String set = formatMongoUpdate(collection, query, mongoCommands.getSet());
        String unset = formatMongoUpdate(collection, query, mongoCommands.getUnset());
        String push = formatMongoUpdate(collection, query, mongoCommands.getPush());
        return new MongoCommands(unset, set, push);
    }

    private String formatMongoUpdate(String collection, String query, String mongoCommand) {
        return TextUtil.hasText(mongoCommand) ? String.format("db.%s.update ( %s, %s )", collection, query, mongoCommand)
                    : "";
    }

    private String createPush(Map<String, List<JsonNode>> push, String prefixStr) {
        if (MapUtils.isEmpty(push)) {
            return "";
        }
        StringBuilder temp = push.entrySet().stream().collect(StringBuilder::new, (sb, entry) -> {
            List<JsonNode> values = entry.getValue();
            sb.append(createKey(prefixStr, entry.getKey()));
            if (values.size() == 1) {
                sb.append(values.get(0));
            } else if (values.size() > 1) {
                sb.append("{ $each:[");
                range(0, values.size() - 1).forEach(i -> sb.append(values.get(i)).append(","));
                sb.append(values.get(values.size() - 1));
                sb.append("] }");
            }
            sb.append(",");
        }, (sb, l) -> {
        });
        return wrapCommand("$push", temp);
    }

    String toDot(String s) {
        s = s.replaceAll("^/", "");
        return String.join(".", s.split("/"));
    }

    private String createSet(Map<String, JsonNode> set, String prefixStr) {
        if (MapUtils.isEmpty(set)) {
            return "";
        }
        BiConsumer<StringBuilder, Map.Entry<String, JsonNode>> stringBuilderEntryBiConsumer = (sb, entry) -> {
            sb.append(createKey(prefixStr, entry.getKey()));
            JsonNode value = entry.getValue();
            sb.append(String.format("%s,", value));
        };
        StringBuilder res = set.entrySet().stream()
                .collect(StringBuilder::new, stringBuilderEntryBiConsumer, (sb1, entry1) -> {
                });
        return wrapCommand("$set", res);
    }

    private String createUnset(Set<String> unset, String prefixStr) {
        if (CollectionUtils.isEmpty(unset)) {
            return "";
        }
        StringBuilder temp = unset.stream().collect(StringBuilder::new,
                (sb1, s) -> sb1.append(createKey(prefixStr, s)).append("\"\","),
                (sb, entry) -> {
                });
        return wrapCommand("$unset", temp);
    }

    private String createKey(String prefixStr, String key) {
        return String.format("\"%s%s\":", prefixStr, key);
    }

    private String wrapCommand(String command, StringBuilder res) {
        return res.deleteCharAt(res.length() - 1)
                .insert(0, String.format("{ %s: {", command))
                .append(" } }").toString();
    }

}
