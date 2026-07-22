package de.gmaf.api.mcp;

import de.swa.gmaf.GMAF;
import de.swa.ui.MMFGCollection;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class McpToolAdapter {
    private static Hashtable<String, GMAF> sessions;

    static {
        sessions = new Hashtable<String, GMAF>();
    }

    public static void main(String[] args) {
        // 1. Den echten System.out-Kanal für die Claude-JSON-Kommunikation sichern
        System.err.println("[GMAF-MCP] Aktuelles Arbeitsverzeichnis: " + new java.io.File(".").getAbsolutePath()); //nur für logging
        java.io.File configFile = new java.io.File("conf/gmaf.config");
        System.err.println("[GMAF-MCP] Suche Config unter: " + configFile.getAbsolutePath());
        System.err.println("[GMAF-MCP] Datei existiert: " + configFile.exists());

        PrintStream claudeJsonOut = System.out;

        // 2. Den globalen System.out auf System.err umbiegen.
        // Falls GMAF-Module intern System.out.println nutzen, stört es Claude nicht mehr!
        System.setOut(System.err);

        System.err.println("[GMAF-MCP] Embedded Controller initialisiert.");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Verarbeitungsschleife aufrufen
                String response = handleLine(line);

                if (response != null) {
                    // 3. NUR die reine JSON-Antwort über den gesicherten Kanal senden
                    claudeJsonOut.println(response);
                    claudeJsonOut.flush();
                }
            }
        } catch (Exception e) {
            System.err.println("[GMAF-MCP] Kritischer Fehler im Controller: " + e.getMessage());
        }
    }

    private static String handleLine(String jsonRequest) {
        String method = extractJsonValue(jsonRequest, "method");
        String id = extractJsonValue(jsonRequest, "id");

        // Handshake (Initialize)
        if ("initialize".equals(method)) {
            return "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{\"tools\":{}},\"serverInfo\":{\"name\":\"GMAF-Framework-MCP\",\"version\":\"1.0.0\"}}}";
        }

        // API-Spezifikation des Werkzeug-Schemas laut Kapitel 3.3
        if ("tools/list".equals(method)) {
            return "{"
                    + "\"jsonrpc\":\"2.0\","
                    + "\"id\":" + id + ","
                    + "\"result\":{"
                    + "  \"tools\": ["
                    + "    {"
                    + "      \"name\": \"getauthtoken\","
                    + "      \"description\": \"Liefert den Authentifizierungstoken für diese Session. Er wird für die Nutzung aller anderer Tools benötigt.\","
                    + "      \"inputSchema\": {"
                    + "        \"type\": \"object\","
                    + "        \"properties\": {"
                    + "        },"
                    + "        \"required\": []"
                    + "      }"
                    + "    }"
                    + "    ,"
                    + "    {"
                    + "      \"name\": \"gmaf_object_search\","
                    + "      \"description\": \"Führt eine Objektsuche in Multimedia-Kollektionen durch.\","
                    + "      \"inputSchema\": {"
                    + "        \"type\": \"object\","
                    + "        \"properties\": {"
                    + "          \"authtoken\": {\"type\": \"string\", \"description\": \"Authentifizierungstoken dieser Session\"},"
                    + "          \"query\": {\"type\": \"string\", \"description\": \"Gesuchter Objektbegriff auf Englisch\"},"
                    + "          \"min_confidence\": {\"type\": \"number\", \"description\": \"Filterung der Vertrauenswürdigkeit\"}"
                    + "        },"
                    + "        \"required\": [\"query\",\"authtoken\"]"
                    + "      }"
                    + "    }"
                    + "  ]"
                    + "}"
                    + "}";
        }

        // In-Memory-Weiterleitung & Datenmapping (MSS Schritt 3-6)
        if ("tools/call".equals(method)) {
            String toolName = extractJsonValue(jsonRequest, "name");

            // Fall 1: Authentifizierungstoken anfragen
            if ("getauthtoken".equals(toolName)|| toolName.isEmpty()) {
                System.err.println("[GMAF-MCP] Generiere Authentifizierungstoken...");
                //String dummyToken = "gmaf_session_token_12345";
                String token = getNewAuthToken();
                return "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"" + token + "\"}]}}";
            }
            // Fall 2: Objektsuche ausführen
            if ("gmaf_object_search".equals(toolName) ) {
                System.err.println("[GMAF-MCP] Verarbeite atomare Suchanfrage via JSON-RPC...");

                String queryParam = extractJsonValue(jsonRequest, "query");
                String confParam = extractJsonValue(jsonRequest, "min_confidence");
                String tokenParam = extractJsonValue(jsonRequest, "authtoken");
                float minConf = confParam.isEmpty() ? 0.0f : Float.parseFloat(confParam);

                try {
                    GmafQueryConfig config = new GmafQueryBuilder()
                            .setQuery(queryParam)
                            .setMinConfidence(minConf)
                            .setSessionToken(tokenParam)
                            .buildQuery();

                    // Simulation des MMIR-Indexabgleichs (Kapitel 3.4)
                    //String matchAsset = "IMG_2026.jpg";
                    //double simulatedWeight = 0.945;
                    //String bBox = "[52, 28, 231, 211]";
                    return config.search(id);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    private static String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"?([^,\"}]+)\"?");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    /**
     * returns a new session token
     **/
    public static String getNewAuthToken() {
        String uuid = UUID.randomUUID().toString();
        sessions.put(uuid, new GMAF());
        System.out.println("KEY: " + uuid);
        return uuid;
    }
}