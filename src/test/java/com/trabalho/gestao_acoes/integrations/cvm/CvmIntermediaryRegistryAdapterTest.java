package com.trabalho.gestao_acoes.integrations.cvm;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.time.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CvmIntermediaryRegistryAdapterTest {
    private static final Instant NOW = Instant.parse("2026-09-06T15:00:00Z");

    @Test
    void acceptsOnlyExactCnpjWithActiveOfficialRowsAndPreservesDatasetReference() throws Exception {
        CvmRegistryClient client = mock(CvmRegistryClient.class);
        HttpHeaders headers = new HttpHeaders(); headers.setLastModified(Instant.parse("2026-09-05T21:00:00Z"));
        when(client.download()).thenReturn(new ResponseEntity<>(archive(
                "TP_PARTIC;CNPJ;SIT;CD_CVM\n"
                + "CORRETORA;12.345.678/0001-99;EM FUNCIONAMENTO NORMAL;123\n"
                + "DISTRIBUIDORA;12.345.678/0001-99;EM FUNCIONAMENTO NORMAL;456\n"
                + "CORRETORA;98.765.432/0001-10;CANCELADA;999\n"), headers, HttpStatus.OK));

        var snapshot = new CvmIntermediaryRegistryAdapter(client, Clock.fixed(NOW, ZoneOffset.UTC)).load();

        assertThat(snapshot.referenceAt()).isEqualTo(Instant.parse("2026-09-05T21:00:00Z"));
        assertThat(snapshot.fetchedAt()).isEqualTo(NOW);
        assertThat(snapshot.activeByCnpj()).containsOnlyKeys("12345678000199");
        assertThat(snapshot.activeByCnpj().get("12345678000199").category()).isEqualTo("CORRETORA | DISTRIBUIDORA");
        assertThat(snapshot.activeByCnpj().get("12345678000199").evidenceId()).isEqualTo("123,456");
    }

    @Test
    void rejectsInvalidArchiveMissingReferenceAndFutureReference() throws Exception {
        CvmRegistryClient client = mock(CvmRegistryClient.class);
        when(client.download()).thenReturn(ResponseEntity.ok(archive("TP_PARTIC;CNPJ;SIT;CD_CVM\n")));
        var adapter = new CvmIntermediaryRegistryAdapter(client, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThatThrownBy(adapter::load).isInstanceOf(IllegalStateException.class);

        HttpHeaders future = new HttpHeaders(); future.setLastModified(NOW.plusSeconds(1));
        when(client.download()).thenReturn(new ResponseEntity<>(archive("TP_PARTIC;CNPJ;SIT;CD_CVM\n"), future, HttpStatus.OK));
        assertThatThrownBy(adapter::load).isInstanceOf(IllegalStateException.class);
    }

    private static byte[] archive(String content) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, Charset.forName("windows-1252"))) {
            zip.putNextEntry(new ZipEntry("cad_intermed.csv"));
            zip.write(content.getBytes(Charset.forName("windows-1252"))); zip.closeEntry();
        }
        return bytes.toByteArray();
    }
}
