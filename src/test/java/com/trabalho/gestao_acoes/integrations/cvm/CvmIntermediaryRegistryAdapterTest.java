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
    void rejectsIncompleteArchiveMissingReferenceAndFutureReference() throws Exception {
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
            zip.putNextEntry(new ZipEntry("cad_intermed.csv")); zip.write(content.getBytes(Charset.forName("windows-1252"))); zip.closeEntry();
        }
        return bytes.toByteArray();
    }
}
