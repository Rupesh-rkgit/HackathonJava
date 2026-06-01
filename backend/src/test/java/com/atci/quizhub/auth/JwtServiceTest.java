package com.atci.quizhub.auth;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwt = new JwtService(
        "test-secret-test-secret-test-secret-test-secret-123456", 3600000L);

    @Test
    void issuesAndParsesToken() {
        String token = jwt.generate("birendra.kumar.singh", "ADMIN");
        assertEquals("birendra.kumar.singh", jwt.extractUsername(token));
        assertTrue(jwt.isValid(token, "birendra.kumar.singh"));
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwt.generate("alice", "SME");
        assertFalse(jwt.isValid(token + "x", "alice"));
    }
}
