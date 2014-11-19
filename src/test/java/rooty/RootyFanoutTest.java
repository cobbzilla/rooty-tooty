package rooty;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.io.StreamUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rooty.handlers.TouchFileHandler;
import rooty.handlers.TouchMessage;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
public class RootyFanoutTest {

    public static final long TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    public static final String CONFIG_DIR = "config";
    private static final int NUM_CONSUMERS = 3;

    private final String secret = RandomStringUtils.randomAlphanumeric(20);

    private RootyMain[] consumers = new RootyMain[NUM_CONSUMERS];
    private String queueName = "test_rooty_"+RandomStringUtils.randomAlphanumeric(10);
    private File testDir;

    @Before
    public void setup () throws Exception {

        testDir = FileUtil.createTempDir("RootyTest");

        for (int i=0; i<NUM_CONSUMERS; i++) {
            final RootyMain main = new RootyMain();
            final String suffix = "_" + i;

            final File configDir = new File(testDir, CONFIG_DIR + suffix);
            if (!configDir.mkdirs()) throw new IllegalStateException("Error creating config dir");
            final File configFile = File.createTempFile("rooty-config", ".yml", configDir);

            // write config yml file -- substitute {{BASE}} with testDir we just created
            final String configTemplate = StreamUtil.loadResourceAsString("rooty-fanout-test-config.yml");
            final String configData = configTemplate
                    .replace("{{QUEUE_NAME}}", queueName + "+child" + suffix)
                    .replace("{{SECRET}}", secret)
                    .replace("{{SUFFIX}}", suffix);
            FileUtil.toFile(configFile, configData);

            final String[] args = { configFile.getAbsolutePath() };
            main.run(args);
            consumers[i] = main;
            assertTrue(consumers[i].waitForStartup(10000));
        }
    }

    @After
    public void teardown () throws Exception {
        FileUtils.deleteDirectory(testDir);
        for (int i=0; i<NUM_CONSUMERS; i++) {
            final RootyMain main = consumers[i];
            main.getMqClient().deleteQueue(queueName + "+child_" + i);
            main.shutdown();
        }
    }

    @Test
    public void testFanout () throws Exception {

        // create a request
        @Cleanup("delete") final File targetFile = File.createTempFile("testFanout", ".test");
        final TouchMessage message = new TouchMessage(targetFile.getAbsolutePath());

        // write request to requests dir
        log.info("writing to parent queue");
        TouchFileHandler.resetStats();
        new TouchFileHandler()
                .setMqClient(consumers[0].getMqClient())
                .setQueueName(queueName) // write to parent queue
                .write(message, secret);
        log.info("wrote to parent queue: "+message);

        for (int i=0; i<NUM_CONSUMERS; i++) {
            final TouchFileHandler handler = consumers[i].getConfiguration().getHandler(TouchFileHandler.class);

            final File touched = new File(targetFile.getAbsolutePath()+ handler.getFileSuffix());

            // wait for file to be touched
            long start = System.currentTimeMillis();
            while (!touched.exists() && System.currentTimeMillis() - start < TIMEOUT) {
                Thread.sleep(100);
            }

            assertTrue("expected file to exist: " + touched.getAbsolutePath(), touched.exists());
        }

        assertEquals(NUM_CONSUMERS, TouchFileHandler.getMessageCount());
        assertEquals(NUM_CONSUMERS, TouchFileHandler.getSuccessCount());
    }

}
