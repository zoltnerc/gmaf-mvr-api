package de.gmaf.api.mcp;

import java.util.Map;

public class GmafQueryBuilder {
    private String query;
    private float minConfidence = 0.0f; // Standardwert laut Spezifikation

    public GmafQueryBuilder setQuery(String query) {
        this.query = query;
        return this;
    }

    public GmafQueryBuilder setMinConfidence(float minConfidence) {
        this.minConfidence = minConfidence;
        return this;
    }

    public GmafQueryConfig buildQuery() {
        if (this.query == null || this.query.trim().isEmpty()) {
            throw new IllegalArgumentException("Der Parameter 'query' ist obligatorisch.");
        }
        return new GmafQueryConfig(query, minConfidence);
    }
}