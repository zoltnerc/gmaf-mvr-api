package de.gmaf.api.mcp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class McpToolAdapter {

    public static void main(String[] args) {
        // 1. Den echten System.out-Kanal für die Claude-JSON-Kommunikation sichern
        PrintStream claudeJsonOut = System.out;

        // 2. Den globalen System.out auf System.err umbiegen.
        // Falls GMAF-Module intern System.out.println nutzen, stört es Claude nicht mehr!
        System.setOut(System.err);

        System.err.println("[GMAF-MCP] Embedded Controller initialisiert (In-Memory-Modus).");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Verarbeitungsschleife aufrufen
                String response = handleProtokollSchleife(line);

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

    private static String handleProtokollSchleife(String jsonRequest) {
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
                    + "      \"name\": \"gmaf_object_search\","
                    + "      \"description\": \"Führt eine Objektsuche in Multimedia-Kollektionen durch.\","
                    + "      \"inputSchema\": {"
                    + "        \"type\": \"object\","
                    + "        \"properties\": {"
                    + "          \"query\": {\"type\": \"string\", \"description\": \"Gesuchter Objektbegriff\"},"
                    + "          \"min_confidence\": {\"type\": \"number\", \"description\": \"Filterung der Vertrauenswürdigkeit\"}"
                    + "        },"
                    + "        \"required\": [\"query\"]"
                    + "      }"
                    + "    }"
                    + "  ]"
                    + "}"
                    + "}";
        }

        // In-Memory-Weiterleitung & Datenmapping (MSS Schritt 3-6)
        if ("tools/call".equals(method)) {
            System.err.println("[GMAF-MCP] Verarbeite atomare Suchanfrage via JSON-RPC...");

            String queryParam = extractJsonValue(jsonRequest, "query");
            String confParam = extractJsonValue(jsonRequest, "min_confidence");
            float minConf = confParam.isEmpty() ? 0.0f : Float.parseFloat(confParam);

            try {
                GmafQueryConfig config = new GmafQueryBuilder()
                        .setQuery(queryParam)
                        .setMinConfidence(minConf)
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
}