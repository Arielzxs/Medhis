package com.neusoft.his.common.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class JwtTokenProviderTest {

    @Test
    void shouldGenerateAndParseToken() {
        JwtTokenProvider provider = new JwtTokenProvider("12345678901234567890123456789012", 3600);
        String token = provider.generate(1L, "doctorA", Set.of(RoleCode.DOCTOR));
        SecurityUser user = provider.parse(token);
        Assertions.assertEquals(1L, user.userId());
        Assertions.assertEquals("doctorA", user.username());
        Assertions.assertTrue(user.roles().contains(RoleCode.DOCTOR));
    }
}
