package rooty;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.io.StreamUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rooty.handlers.TouchFileHandler;
import rooty.handlers.TouchMessage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
public class RootyTest {

    public static final long TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    public static final String CONFIG_DIR = "config";

    private final String secret = RandomStringUtils.randomAlphanumeric(20);

    private RootyMain main;
    private File testDir;
    private String queueName = "test_rooty_"+RandomStringUtils.randomAlphanumeric(10);
    private File configFile;

    @Before
    public void setup () throws Exception {

        main = new RootyMain();
        testDir = FileUtil.createTempDir("RootyTest");

        if (!new File(testDir, CONFIG_DIR).mkdirs()) throw new IllegalStateException("Error creating config dir");
        configFile = File.createTempFile("rooty-config", ".yml", new File(testDir, CONFIG_DIR));

        // write config yml file -- substitute {{params}}
        final String configTemplate = StreamUtil.loadResourceAsString("rooty-test-config.yml");
        final String configData = configTemplate
                .replace("{{QUEUE_NAME}}", queueName)
                .replace("{{SECRET}}", secret);
        FileUtil.toFile(configFile, configData);

        final String[] args = { configFile.getAbsolutePath() };
        main.run(args);
        assertTrue(main.waitForStartup(10000));

        TouchFileHandler.resetStats();
    }

    @After
    public void teardown () throws Exception {
        main.getMqClient().deleteQueue(queueName);
        main.shutdown();
        FileUtils.deleteDirectory(testDir);
    }

    @Test
    public void testBasic () throws Exception {

        // create a request
        final File targetFile = new File(System.getProperty("java.io.tmpdir"), RandomStringUtils.randomAlphanumeric(10));
        final TouchMessage message = new TouchMessage(targetFile.getAbsolutePath());

        // write request to requests dir
        log.info("writing to MQ");
        TouchFileHandler.resetStats();
        new TouchFileHandler()
                .setMqClient(main.getMqClient())
                .setQueueName(queueName)
                .write(message, secret);
        log.info("wrote to MQ: "+message);

        expectFileTouched(targetFile);
    }

    @Test
    public void testSender () throws Exception {

        final File targetFile = new File(System.getProperty("java.io.tmpdir"), RandomStringUtils.randomAlphanumeric(10));
        final TouchMessage message = new TouchMessage(targetFile.getAbsolutePath());

        final RootySenderMain sender = new RootySenderMain();
        final String[] args = { configFile.getAbsolutePath() };
        final InputStream in = new ByteArrayInputStream(JsonUtil.toJson(message).getBytes());
        sender.send(args, in);

        expectFileTouched(targetFile);
    }

    public void expectFileTouched(File targetFile) throws InterruptedException {

        // wait for file to be touched
        long start = System.currentTimeMillis();
        while (!targetFile.exists() && System.currentTimeMillis() - start < TIMEOUT) {
            Thread.sleep(100);
        }

        assertTrue("expected file to exist: " + targetFile.getAbsolutePath(), targetFile.exists());
        assertEquals(1, TouchFileHandler.getMessageCount());
        assertEquals(1, TouchFileHandler.getSuccessCount());
    }
}
