package de.gmaf.api.mcp;

import de.swa.gc.GraphCode;
import de.swa.gmaf.extensions.defaults.GeneralDictionary;
import de.swa.gmaf.extensions.defaults.Word;
import de.swa.mmfg.GeneralMetadata;
import de.swa.mmfg.MMFG;
import de.swa.ui.MMFGCollection;

import java.util.StringJoiner;
import java.util.Vector;
import java.util.stream.Collectors;

public class GmafQueryConfig {
    private final String queryTerm;
    private final float minConfidence;
    private final String sessiontoken;

    public GmafQueryConfig(String sessiontoken, String queryTerm, float minConfidence) {
        this.sessiontoken = sessiontoken;
        this.queryTerm = queryTerm;
        this.minConfidence = minConfidence;
    }

    public String getQuery() { return queryTerm; }
    public float getMinConfidence() { return minConfidence; }

    public String search(String id) {
        String matchAsset;
        String[] matchAssets = this.queryByKeyword(sessiontoken, queryTerm);
        if(matchAssets == null || matchAssets.length==0){
            matchAsset = "";
        } else {
            matchAsset = matchAssets.toString();
        }
        //String matchAsset = "test";
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

    public String[] queryByKeyword( String auth_token, String keywords) {
        GraphCode gc = new GraphCode();
        Vector<String> dict = new Vector<String>();
        keywords = keywords.replace(";", ",");
        //keywords = keywords.replace(" ", ",");
        String[] str = keywords.split(",");
        for (String s : str) dict.add(s.trim());
        gc.setDictionary(dict);

        System.out.println("query by keyword " + keywords + " with token " + auth_token);

        try {

            Vector<String> ids = new Vector<String>();
            MMFGCollection coll = MMFGCollection.getInstance(auth_token);
            for (MMFG m : coll.getSimilarAssets(gc)) {
                ids.add(m.getId().toString());
            }

            if (ids.size() == 0) {
                Vector<MMFG> mmfgs = MMFGCollection.getInstance(auth_token).getCollection();
                for (MMFG m : mmfgs) ids.add(m.getId().toString());
            }

            System.out.println("found " + ids.size() + " results");

            String[] strx = new String[ids.size()];
            for (int i = 0; i < strx.length; i++) {
                String s = ids.get(i);
                strx[i] = s;
            }
            return strx;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}