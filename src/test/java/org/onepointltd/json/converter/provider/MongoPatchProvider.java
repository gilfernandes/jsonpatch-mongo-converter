package org.onepointltd.json.converter.provider;

import org.onepointltd.json.converter.JsonPatchToMongoUpdate;
import org.onepointltd.json.converter.MongoConversion;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Provides test data for the MongoDB patches.
 */
public class MongoPatchProvider {

    public static MongoConversion createPatches1(JsonPatchToMongoUpdate jsonPatchToMongoUpdate) throws IOException {
        MongoConversion res;
        try(Reader reader = new InputStreamReader(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("patch/patchSample.json"))) {
            return jsonPatchToMongoUpdate.convert(reader);
        }
    }

    public static MongoConversion createPatches2(JsonPatchToMongoUpdate jsonPatchToMongoUpdate) throws IOException {
        try(Reader reader = getPatch2Reader()) {
            return jsonPatchToMongoUpdate.convert(reader);
        }
    }

    public static InputStreamReader getCustomerPatchUpdate() {
        return new InputStreamReader(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("patch/customerPatchUpdate.json"));
    }

    public static InputStreamReader getPatch2Reader() {
        return new InputStreamReader(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("patch/patchSample2.json"));
    }

    public static InputStreamReader getPatchNoRemove() {
        return new InputStreamReader(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("patch/patchSampleNoUnset.json"));
    }
}
