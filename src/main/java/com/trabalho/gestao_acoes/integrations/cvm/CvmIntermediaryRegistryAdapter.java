package com.trabalho.gestao_acoes.integrations.cvm;

import com.trabalho.gestao_acoes.services.ports.RegulatoryRegistryPort;
import com.trabalho.gestao_acoes.services.ports.RegulatoryRegistrySnapshot;
import com.trabalho.gestao_acoes.services.ports.RegulatoryRegistrySnapshot.RegulatoryEntry;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
        if (body == null || body.length == 0) throw new IllegalStateException("Official CVM dataset has no body");
        Instant fetchedAt = clock.instant();
        Instant referenceAt = lastModified > 0 ? Instant.ofEpochMilli(lastModified) : fetchedAt;
        if (referenceAt.isAfter(fetchedAt)) referenceAt = fetchedAt;
        return new RegulatoryRegistrySnapshot(SOURCE, referenceAt, fetchedAt, parse(body));
    }

    static Map<String, List<RegulatoryEntry>> parse(byte[] zipBytes) {
        Map<String, List<RegulatoryEntry>> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes), CSV_CHARSET)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().equalsIgnoreCase("cad_intermed.csv")) {
                    byte[] csvBytes = zip.readAllBytes();
                    BufferedReader reader = new BufferedReader(new StringReader(decode(csvBytes)));
                    List<String> headers = csv(reader.readLine());
                    int type = requiredIndex(headers, "TPPARTIC", "TPPARTICIPANTE");
                    int cnpj = requiredIndex(headers, "CNPJ");
                    int status = requiredIndex(headers, "SIT", "SITUACAO");
                    int code = requiredIndex(headers, "CDCVM", "CODCVM");
                    String line;
                    while ((line = reader.readLine()) != null) {
                        List<String> values = csv(line);
                        if (values.size() <= Math.max(Math.max(type, cnpj), Math.max(status, code))) continue;
                        String canonical = values.get(cnpj).replaceAll("\\D", "");
                        String category = values.get(type).trim();
                        String registryStatus = values.get(status).trim();
                        String evidenceId = values.get(code).trim();
                        if (canonical.length() != 14) continue;
                        if (category.isBlank() || registryStatus.isBlank() || evidenceId.isBlank()) {
                            // The official archive contains historical rows without CD_CVM. They are not evidence.
                            continue;
                        }
                        entries.computeIfAbsent(canonical, ignored -> new ArrayList<>())
                                .add(new RegulatoryEntry(category, registryStatus, evidenceId));
                    }
                    entries.replaceAll((ignored, values) -> values.stream()
                            .sorted(Comparator.comparing(RegulatoryEntry::category).thenComparing(RegulatoryEntry::evidenceId))
                            .toList());
                    if (entries.isEmpty()) throw new IllegalStateException("Official CVM dataset has no complete entries");
                    return entries;
                }
            }
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Invalid official CVM dataset", error);
        }
        throw new IllegalStateException("cad_intermed.csv is absent from official CVM archive");
    }

    private static String decode(byte[] bytes) {
        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }
        return new String(bytes, CSV_CHARSET);
    }

    private static int requiredIndex(List<String> headers, String... names) {
        for (int index = 0; index < headers.size(); index++) {
            String header = canonicalHeader(headers.get(index));
            for (String name : names) if (header.equals(name)) return index;
        }
        throw new IllegalStateException("Required CVM column is missing: " + String.join("/", names));
    }

    private static String canonicalHeader(String value) { return value.replace("\uFEFF", "").replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT); }

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
