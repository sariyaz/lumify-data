package com.altamiracorp.lumify.core.util;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ThreadedInputStreamProcessTest {
    @Test
    public void testDoWork() throws Exception {
        ArrayList<ThreadedTeeInputStreamWorker<byte[], String>> workers = new ArrayList<ThreadedTeeInputStreamWorker<byte[], String>>();
        workers.add(new TestThreadedTeeInputStreamWorker("1"));
        workers.add(new TestThreadedTeeInputStreamWorker("2"));
        ThreadedInputStreamProcess process = new ThreadedInputStreamProcess<byte[], String>("test", workers);

        byte[] data = createMockData(10);

        // first run
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        List<ThreadedTeeInputStreamWorker.WorkResult<byte[]>> results = process.doWork(in, "test1");
        assertEquals(2, results.size());

        assertEquals(null, results.get(0).getError());
        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        expected.write("1".getBytes());
        expected.write("test1".getBytes());
        expected.write(data);
        assertArrayEquals(expected.toByteArray(), results.get(0).getResult());

        assertEquals(null, results.get(1).getError());
        expected = new ByteArrayOutputStream();
        expected.write("2".getBytes());
        expected.write("test1".getBytes());
        expected.write(data);
        assertArrayEquals(expected.toByteArray(), results.get(1).getResult());

        // second run
        in = new ByteArrayInputStream(data);
        results = process.doWork(in, "test2");
        assertEquals(2, results.size());

        assertEquals(null, results.get(0).getError());
        expected = new ByteArrayOutputStream();
        expected.write("1".getBytes());
        expected.write("test2".getBytes());
        expected.write(data);
        assertArrayEquals(expected.toByteArray(), results.get(0).getResult());

        assertEquals(null, results.get(1).getError());
        expected = new ByteArrayOutputStream();
        expected.write("2".getBytes());
        expected.write("test2".getBytes());
        expected.write(data);
        assertArrayEquals(expected.toByteArray(), results.get(1).getResult());
    }

    private byte[] createMockData(int len) {
        byte[] data = new byte[len];
        for (int i = 0; i < len; i++) {
            data[i] = (byte) i;
        }
        return data;
    }

    private static class TestThreadedTeeInputStreamWorker extends ThreadedTeeInputStreamWorker<byte[], String> {
        private final String prefix;

        public TestThreadedTeeInputStreamWorker(String prefix) {
            this.prefix = prefix;
        }

        @Override
        protected byte[] doWork(InputStream work, String s) throws Exception {
            ByteArrayOutputStream temp = new ByteArrayOutputStream();
            temp.write(prefix.getBytes());
            temp.write(s.getBytes());
            IOUtils.copy(work, temp);
            return temp.toByteArray();
        }
    }
}
