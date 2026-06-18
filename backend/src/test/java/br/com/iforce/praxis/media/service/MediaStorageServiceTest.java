package br.com.iforce.praxis.media.service;

import br.com.iforce.praxis.config.ObjectStorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaStorageServiceTest {

    @Test
    void rejectsUnsupportedImageSubtypeBeforeStorageWrite() {
        MediaStorageService service = new MediaStorageService(new ObjectStorageProperties(
                "http://localhost:9000",
                "http://localhost:9000",
                "us-east-1",
                "access",
                "secret",
                "praxis-media",
                true
        ));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "payload.svg",
                "image/svg+xml",
                "<svg><script>alert(1)</script></svg>".getBytes()
        );

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("415 UNSUPPORTED_MEDIA_TYPE");
    }
}
