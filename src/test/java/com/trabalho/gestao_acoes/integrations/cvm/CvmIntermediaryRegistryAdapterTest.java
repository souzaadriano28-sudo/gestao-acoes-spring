package com.trabalho.gestao_acoes.integrations.cvm;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CvmIntermediaryRegistryAdapterTest {
    private static final Instant NOW = Instant.parse("2026-09-07T15:00:00Z");

    @Test
    void preservesActiveAndInactiveRowsWithOfficialReference() throws Exception {
        CvmRegistryClient client = mock(CvmRegistryClient.class); HttpHeaders headers = new HttpHeaders();
        headers.setLastModified(Instant.parse("2026-09-05T21:00:00Z"));
        when(client.download()).thenReturn(new ResponseEntity<>(archive("TP_PARTIC;CNPJ;SIT;CD_CVM\n"
                + "CORRETORAS;11.222.333/0001-81;EM FUNCIONAMENTO NORMAL;123\n"
                + "DISTRIBUIDORAS;11.222.333/0001-81;CANCELADA;456\n"), headers, HttpStatus.OK));
        var snapshot = new CvmIntermediaryRegistryAdapter(client, Clock.fixed(NOW, ZoneOffset.UTC)).load();
        assertThat(snapshot.referenceAt()).isEqualTo(Instant.parse("2026-09-05T21:00:00Z"));
        assertThat(snapshot.entriesByCnpj().get("11222333000181")).hasSize(2);
        assertThat(snapshot.entriesByCnpj().get("11222333000181")).extracting("status")
                .containsExactlyInAnyOrder("EM FUNCIONAMENTO NORMAL", "CANCELADA");
    }

    @Test
    void acceptsCurrentCvmShapeWithBomAndNoLastModifiedHeader() throws Exception {
        CvmRegistryClient client = mock(CvmRegistryClient.class);
        when(client.download()).thenReturn(ResponseEntity.ok(utf8Archive("\uFEFFTP_PARTICIPANTE;CNPJ;SITUACAO;COD_CVM\n"
                + "CORRETORAS;02.332.886/0001-04;EM FUNCIONAMENTO NORMAL;123\n")));
        var adapter = new CvmIntermediaryRegistryAdapter(client, Clock.fixed(NOW, ZoneOffset.UTC));
        var snapshot = adapter.load();
        assertThat(snapshot.referenceAt()).isEqualTo(NOW);
        assertThat(snapshot.entriesByCnpj().get("02332886000104")).hasSize(1);
    }

    @Test
    void parsesOfficialCvmArchiveWhileExcludingRowsWithoutMandatoryEvidence() throws Exception {
        byte[] archive;
        try (InputStream input = getClass().getResourceAsStream("/cvm/cad_intermed-official.zip")) {
            assertThat(input).as("official CVM archive fixture").isNotNull();
            archive = input.readAllBytes();
        }

        var entries = CvmIntermediaryRegistryAdapter.parse(archive);

        assertThat(entries).isNotEmpty().containsKey("02332886000104");
        assertThat(entries.values().stream().flatMap(java.util.Collection::stream))
                .allSatisfy(evidence -> {
                    assertThat(evidence.category()).isNotBlank();
                    assertThat(evidence.status()).isNotBlank();
                    assertThat(evidence.evidenceId()).isNotBlank();
                });
    }

    @Test
    void rejectsIncompleteArchiveAndFutureReference() throws Exception {
        CvmRegistryClient client = mock(CvmRegistryClient.class);
        when(client.download()).thenReturn(ResponseEntity.ok(archive("TP_PARTIC;CNPJ;SIT;CD_CVM\n")));
        var adapter = new CvmIntermediaryRegistryAdapter(client, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThatThrownBy(adapter::load).isInstanceOf(IllegalStateException.class);
        HttpHeaders future = new HttpHeaders(); future.setLastModified(NOW.plusSeconds(1));
        when(client.download()).thenReturn(new ResponseEntity<>(archive("TP_PARTIC;CNPJ;SIT;CD_CVM\n"
                + "CORRETORAS;02.332.886/0001-04;EM FUNCIONAMENTO NORMAL;123\n"), future, HttpStatus.OK));
        assertThat(adapter.load().referenceAt()).isEqualTo(NOW);
    }

    private static byte[] archive(String content) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, Charset.forName("windows-1252"))) {
            zip.putNextEntry(new ZipEntry("cad_intermed.csv")); zip.write(content.getBytes(Charset.forName("windows-1252"))); zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    private static byte[] utf8Archive(String content) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("cad_intermed.csv")); zip.write(content.getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
        }
        return bytes.toByteArray();
    }
}
