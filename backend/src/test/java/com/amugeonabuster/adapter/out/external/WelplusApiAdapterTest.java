package com.amugeonabuster.adapter.out.external;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class WelplusApiAdapterTest {

    @Test
    void refreshesThirtySecondsBeforeTheJwtExpires() {
        WelplusApiAdapter adapter = new WelplusApiAdapter();
        long expiresAt = Instant.now().plusSeconds(600).getEpochSecond();
        String claims = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"exp\":" + expiresAt + "}").getBytes(StandardCharsets.UTF_8));

        long resolvedExpiry = adapter.resolveTokenExpiry("Bearer e30." + claims + ".signature");

        assertThat(resolvedExpiry).isEqualTo(expiresAt * 1000 - 30_000);
        adapter.stopMenuExecutor();
    }
}
