package postman.importer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

public final class PostmanCollectionImporter {

    public void importCollection(File collection) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(collection);
        // Parse items and generate Java service/test stubs
    }
}