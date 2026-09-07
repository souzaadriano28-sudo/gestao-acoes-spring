package com.trabalho.gestao_acoes.integrations.cvm;

import com.trabalho.gestao_acoes.services.ports.RegulatoryRegistryPort;
import com.trabalho.gestao_acoes.services.ports.RegulatoryRegistrySnapshot;
import com.trabalho.gestao_acoes.services.ports.RegulatoryRegistrySnapshot.RegulatoryEntry;
import java.io.*;
import java.nio.charset.Charset;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class CvmIntermediaryRegistryAdapter implements RegulatoryRegistryPort {
    public static final String SOURCE = "CVM_DADOS_ABERTOS_INTERMED_CAD";
    private static final Charset CSV_CHARSET = Charset.forName("windows-1252");
    private final CvmRegistryClient client;
    private final Clock clock;

    public CvmIntermediaryRegistryAdapter(CvmRegistryClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    @Override
    public RegulatoryRegistrySnapshot load() {
        ResponseEntity<byte[]> response = client.download();
        byte[] body = response.getBody();
        long lastModified = response.getHeaders().getLastModified();
        if (body == null || body.length == 0 || lastModified <= 0) {
            throw new IllegalStateException("Official CVM dataset has no verifiable body/reference");
        }
        Instant fetchedAt = clock.instant();
        Instant referenceAt = Instant.ofEpochMilli(lastModified);
        if (referenceAt.isAfter(fetchedAt)) throw new IllegalStateException("Official CVM reference is in the future");
        return new RegulatoryRegistrySnapshot(SOURCE, referenceAt, fetchedAt, parse(body));
    }

    static Map<String, RegulatoryEntry> parse(byte[] zipBytes) {
        Map<String, SortedSet<String>> categories = new HashMap<>();
        Map<String, SortedSet<String>> evidenceIds = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes), CSV_CHARSET)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().equalsIgnoreCase("cad_intermed.csv")) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zip, CSV_CHARSET));
                    List<String> headers = csv(reader.readLine());
                    int type = requiredIndex(headers, "TP_PARTIC");
                    int cnpj = requiredIndex(headers, "CNPJ");
                    int status = requiredIndex(headers, "SIT");
                    int code = requiredIndex(headers, "CD_CVM");
                    String line;
                    while ((line = reader.readLine()) != null) {
                        List<String> values = csv(line);
                        if (values.size() <= Math.max(Math.max(type, cnpj), Math.max(status, code))) continue;
                        if (!"EM FUNCIONAMENTO NORMAL".equalsIgnoreCase(values.get(status).trim())) continue;
                        String canonical = values.get(cnpj).replaceAll("\\D", "");
                        String category = values.get(type).trim();
                        String evidenceId = values.get(code).trim();
                        if (canonical.length() != 14 || category.isBlank() || evidenceId.isBlank()) continue;
                        categories.computeIfAbsent(canonical, ignored -> new TreeSet<>()).add(category);
                        evidenceIds.computeIfAbsent(canonical, ignored -> new TreeSet<>()).add(evidenceId);
                    }
                    Map<String, RegulatoryEntry> result = new HashMap<>();
                    categories.forEach((cnpjValue, values) -> result.put(cnpjValue,
                            new RegulatoryEntry(String.join(" | ", values), String.join(",", evidenceIds.get(cnpjValue)))));
                    return result;
                }
            }
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Invalid official CVM dataset", error);
        }
        throw new IllegalStateException("cad_intermed.csv is absent from official CVM archive");
    }

    private static int requiredIndex(List<String> headers, String name) {
        int index = headers.indexOf(name);
        if (index < 0) throw new IllegalStateException("Required CVM column is missing: " + name);
        return index;
    }

    static List<String> csv(String line) {
        if (line == null) throw new IllegalStateException("CVM CSV has no header");
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { current.append('"'); i++; }
                else quoted = !quoted;
            } else if (character == ';' && !quoted) { values.add(current.toString()); current.setLength(0); }
            else current.append(character);
        }
        if (quoted) throw new IllegalStateException("Unclosed quoted CVM field");
        values.add(current.toString());
        return values;
    }
}
