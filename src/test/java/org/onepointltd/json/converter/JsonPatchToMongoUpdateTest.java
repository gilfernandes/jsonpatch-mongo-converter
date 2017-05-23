package org.onepointltd.json.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.onepointltd.json.converter.provider.MongoPatchProvider;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Java6Assertions.assertThat;

class JsonPatchToMongoUpdateTest {

    private ObjectMapper mapper = new ObjectMapper();

    private JsonPatchToMongoUpdate jsonPatchToMongoUpdate;

    @BeforeEach
    void setUp() {
        jsonPatchToMongoUpdate = new JsonPatchToMongoUpdate();
    }

    @Test
    void whenConvertToMongo_ShouldProduceRightCommands() throws IOException {
        checkMongoConversion(MongoPatchProvider.createPatches1(jsonPatchToMongoUpdate));
    }

    @Test
    void whenConvertToMongo2_ShouldProduceRightCommands() throws IOException {
        checkMongoConversion(MongoPatchProvider.createPatches2(jsonPatchToMongoUpdate));
    }

    @Test
    void whenConvertToMongoUpdates_ShouldProduceRightCommands() throws IOException {
        MongoCommands mongoQueries = jsonPatchToMongoUpdate.convertToMongoUpdates(MongoPatchProvider.getPatch2Reader(),
                "{ _id: ObjectId(\"58a46cc6f7076692b7693c4e\") }", "customer", "customer");
        assertThat(mongoQueries.getUnset()).isEqualTo("db.customer.update ( { _id: ObjectId(\"58a46cc6f7076692b7693c4e\") }, { $unset: {\"customer.preferences.preferences.0\":\"\" } } )");
        assertThat(mongoQueries.getSet()).isEqualTo("db.customer.update ( { _id: ObjectId(\"58a46cc6f7076692b7693c4e\") }, { $set: {\"customer.lastName\":\"Morales\" } } )");
        assertThat(mongoQueries.getPush()).isEqualTo("db.customer.update ( { _id: ObjectId(\"58a46cc6f7076692b7693c4e\") }, { $push: {\"customer.contactPoints.contactPoints\":{ $each:[{\"priorityType\":\"high\",\"qualityLevel\":\"good\",\"lastUpdateSource\":\"C4C\",\"contactAccount\":null,\"contactPhoneNumber\":null,\"contactAddress\":null,\"contactEmailAddress\":{\"emailCategory\":\"Business\",\"emailAddress\":{\"email\":\"arianna.morales@gmail.com\"}},\"relatedContactPointId\":null,\"lastValidated\":1369184569000,\"effectiveFrom\":1485734400000,\"effectiveTo\":1533769200000,\"correlationID\":\"9692b484-3b5f-45fd-b514-1c146e5a3295\"},{\"priorityType\":\"high\",\"qualityLevel\":\"good\",\"lastUpdateSource\":\"C4C\",\"contactAccount\":null,\"contactPhoneNumber\":null,\"contactAddress\":null,\"contactEmailAddress\":{\"emailCategory\":\"Business\",\"emailAddress\":{\"email\":\"arianna.morales@yahoo.de\"}},\"relatedContactPointId\":null,\"lastValidated\":1369184569000,\"effectiveFrom\":1485734400000,\"effectiveTo\":1533769200000,\"correlationID\":\"9692b484-3b5f-45fd-b514-1c146e5a3295\"}] },\"customer.notes.notes\":{\"note\":\"9QjNDYHNx\",\"noteType\":\"ID card details\",\"noteCode\":\"ID Card\",\"noteSource\":\"8910470\",\"test\":\"123\"} } } )");
    }

    @Test
    void whenConvertToMongoUpdates_2_ShouldProduceRightCommands() throws IOException {
        MongoCommands mongoQueries = jsonPatchToMongoUpdate.convertToMongoUpdates(MongoPatchProvider.getPatch2Reader(),
                "{ _id: ObjectId(\"58a46cc6f7076692b7693c4e\") }", "customer", "customers", "customer");
        assertThat(mongoQueries.getSet().contains("\"customers.customer\""));
        assertThat(mongoQueries.getUnset().contains("\"customers.customer\""));
        assertThat(mongoQueries.getPush().contains("\"customers.customer\""));
        System.out.println(mongoQueries.asJavascript(true));
    }

    @Test
    void whenConvertToMongoUpdates_3_ShouldProduceRightCommands() throws IOException {
        MongoCommands mongoQueries = jsonPatchToMongoUpdate.convertToMongoUpdates(
                MongoPatchProvider.getPatchNoRemove(),
                "{ _id: ObjectId(\"58a46cc6f7076692b7693c4e\") }", "customer", "customers", "customer");
        assertThat(mongoQueries.getSet().contains("\"customers.customer\""));
        assertThat(mongoQueries.getUnset().contains("\"customers.customer\""));
        assertThat(mongoQueries.getPush().contains("\"customers.customer\""));
        String javascript = mongoQueries.asJavascript(true);
        System.out.println(javascript);
        assertThat(javascript.matches("(?m)(?s).+,\\s*\\).+")).isFalse();
    }

    @Test
    void whenCustomerPatchUpdate_ShouldHaveNoPush() throws IOException {
        MongoConversion mongoConversion = jsonPatchToMongoUpdate.convert(MongoPatchProvider.getCustomerPatchUpdate());
        assertThat(mongoConversion.getPush().isEmpty()).isTrue();
        assertThat(mongoConversion.getSet().size()).isEqualTo(2);
        assertThat(mongoConversion.getUnset().size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Tests the pull command generation")
    void convertComplexPullJson() throws IOException {
        try(Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("patch/complexPull.json"))) {
            MongoCommands mongoQueries = jsonPatchToMongoUpdate.convertToMongoUpdates(reader,
                    "{ _id: ObjectId(\"58a46cc6f7076692b7693c4e\") }", "customer", "customers");
            assertThat(mongoQueries.getUnset()).isNotEmpty();
            String javascript = mongoQueries.asJavascript(true);
            assertThat(javascript).contains("$pull: {\"customers.contactPoints.contactPoints\": null }");
        }
    }

    private void checkMongoConversion(MongoConversion res) throws IOException {
        MongoCommands mongoCommands = jsonPatchToMongoUpdate.convertToMongo(res, "customer");
        assertThat(mongoCommands).isNotNull();
        String unset = mongoCommands.getUnset();
        assertThat(unset).isNotNull();
        String set = mongoCommands.getSet();
        assertThat(set).isNotNull();
        String push = mongoCommands.getPush();
        assertThat(push).isNotNull();
        assertThat(balancedBrackets(unset)).isTrue();
        assertThat(balancedBrackets(set)).isTrue();
        assertThat(balancedBrackets(push)).isTrue();
        checkJson(convertToJson(unset));
        checkJson(convertToJson(set));
        if(!push.isEmpty()) {
            checkJson(convertToJson(push));
        }
        System.out.println(unset);
        System.out.println(set);
        System.out.println(push);
    }

    private void checkJson(String unsetJson) throws IOException {
        mapper.readTree(unsetJson);
    }

    private String convertToJson(String unset) {
        return unset.replaceAll("(\\$[^:]+)", "\"$1\"");
    }

    @Test
    void whenConvert_ShouldHaveTheRightSizes() throws IOException {
        createMongConversion();
    }

    private boolean balancedBrackets(String str) {
        return checkBracket(str, '{', '}') && checkBracket(str, '[', ']');
    }

    private boolean checkBracket(String str, char openBracket, char closeBracket) {
        return str.chars().filter(c -> openBracket == c).count() == str.chars().filter(c -> closeBracket == c).count();
    }

    private MongoConversion createMongConversion() throws IOException {
        MongoConversion res = MongoPatchProvider.createPatches1(jsonPatchToMongoUpdate);
        Set<String> unset = res.getUnset();
        assertThat(unset.size()).isEqualTo(1);
        assertThat(res.getSet().size()).isEqualTo(11);
        Map<String, List<JsonNode>> push = res.getPush();
        assertThat(push.size()).isEqualTo(1);
        List<JsonNode> notes = push.get("notes.notes");
        assertThat(notes.size()).isEqualTo(2);
        return res;
    }

    @Test
    void toDot() {
        assertThat(jsonPatchToMongoUpdate.toDot("/version1")).isEqualTo("version1");
        assertThat(jsonPatchToMongoUpdate.toDot("/version2")).isEqualTo("version2");
        assertThat(jsonPatchToMongoUpdate.toDot("/notes/notes/1")).isEqualTo("notes.notes.1");
        assertThat(jsonPatchToMongoUpdate.toDot("/extraNames/extraNames/0/nameType"))
                .isEqualTo("extraNames.extraNames.0.nameType");
    }

}