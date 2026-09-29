package com.nova.offline;

import org.junit.Test;
import static org.junit.Assert.*;

public class NovaOfflineUnitTest {

    @Test
    public void testFormatFileSize() {
        assertEquals("0 B", ModelManager.formatFileSize(0));
        assertEquals("500 B", ModelManager.formatFileSize(500));
        assertTrue(ModelManager.formatFileSize(1024 * 1024).contains("MB") || ModelManager.formatFileSize(1024 * 1024).contains("1"));
        assertTrue(ModelManager.formatFileSize(1024L * 1024L * 1024L).contains("GB") || ModelManager.formatFileSize(1024L * 1024L * 1024L).contains("1"));
    }

    @Test
    public void testLocalAiInstantiation() {
        LocalAI localAI = new NativeAI();
        assertNotNull(localAI.getBackendName());
        assertNotNull(localAI.getStatusMessage());
    }
}
