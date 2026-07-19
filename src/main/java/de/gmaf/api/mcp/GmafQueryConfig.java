package de.gmaf.api.mcp;

public class GmafQueryConfig {
    private final String queryTerm;
    private final float minConfidence;

    public GmafQueryConfig(String queryTerm, float minConfidence) {
        this.queryTerm = queryTerm;
        this.minConfidence = minConfidence;
    }

    public String getQuery() { return queryTerm; }
    public float getMinConfidence() { return minConfidence; }
    public String search(String id) {
        String matchAsset = "test";
        String simulatedWeight = "testWeight";
        String bBox ="testBBox";
        // Deterministische Rückgabe an das LLM zur Halluzinationsvermeidung
        return "{"
                + "\"jsonrpc\": \"2.0\","
                + "\"id\": " + id + ","
                + "\"result\": {"
                + "  \"content\": ["
                + "    {"
                + "      \"type\": \"text\","
                + "      \"text\": \"Match Found: '" + this.getQuery() + "' in Asset '" + matchAsset + "'. Confidence: " + simulatedWeight + ". BoundingBox: " + bBox + ".\""
                + "    }"
                + "  ]"
                + "}"
                + "}";

    } //catch (Exception e) {
        // Kapselung leere Ergebnismenge (Erweiterung 4a)
        //return "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"No matching objects found.\"}]}}";
    //}
   // }
}