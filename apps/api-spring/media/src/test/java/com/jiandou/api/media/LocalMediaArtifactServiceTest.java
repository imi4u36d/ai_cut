package com.jiandou.api.media;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jiandou.api.config.JiandouStorageProperties;
import com.sun.net.httpserver.HttpServer;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalMediaArtifactServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void writesAndCopiesLocalArtifacts() throws Exception {
        LocalMediaArtifactService service = new LocalMediaArtifactService(storageProperties(), "ffmpeg");

        LocalMediaArtifactService.TextArtifact text = service.writeText("gen/text", "note.md", "hello");
        LocalMediaArtifactService.StoredArtifact binary = service.writeBinary("gen/bin", "data.bin", new byte[] {1, 2, 3});
        LocalMediaArtifactService.StoredArtifact copied = service.copyArtifact(binary.publicUrl(), "gen/copy", "copy.bin");

        assertEquals("note.md", text.fileName());
        assertEquals("text/markdown", text.mimeType());
        assertEquals(5L, text.sizeBytes());
        assertEquals("hello", Files.readString(Path.of(text.absolutePath())));
        assertEquals(binary.absolutePath(), service.resolveAbsolutePath(binary.publicUrl()));
        assertEquals("copy.bin", copied.fileName());
        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(Path.of(copied.absolutePath())));
    }

    @Test
    void materializesLocalArtifactsAndValidatesSourceUrls() throws Exception {
        LocalMediaArtifactService service = new LocalMediaArtifactService(storageProperties(), "ffmpeg");
        LocalMediaArtifactService.StoredArtifact binary = service.writeBinary("gen/bin", "data.bin", new byte[] {4, 5, 6});
        LocalMediaArtifactService.StoredArtifact materialized = service.materializeArtifact(
            binary.publicUrl(),
            "gen/materialized",
            "materialized.bin"
        );

        assertEquals("materialized.bin", materialized.fileName());
        assertArrayEquals(new byte[] {4, 5, 6}, Files.readAllBytes(Path.of(materialized.absolutePath())));
        assertEquals(materialized.absolutePath(), service.resolveAbsolutePath(materialized.publicUrl()));

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.materializeArtifact("   ", "gen/materialized", "blank.bin")
        );
        assertEquals("source url is required", ex.getMessage());

        IllegalArgumentException copyEx = assertThrows(
            IllegalArgumentException.class,
            () -> service.copyArtifact("https://example.com/file.bin", "gen/copy", "copy.bin")
        );
        assertEquals("source public url is not a local storage path", copyEx.getMessage());
    }

    @Test
    void createsCachedImageThumbnailForLocalStorageImages() throws Exception {
        LocalMediaArtifactService service = new LocalMediaArtifactService(storageProperties(), "ffmpeg");
        Path imagePath = tempDir.resolve("gen/images/source.png");
        Files.createDirectories(imagePath.getParent());
        BufferedImage image = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.RED);
        graphics.fillRect(0, 0, 1200, 800);
        graphics.dispose();
        ImageIO.write(image, "png", imagePath.toFile());

        String thumbnailUrl = service.ensureImageThumbnail("/storage/gen/images/source.png", 480);
        String secondThumbnailUrl = service.ensureImageThumbnail("/storage/gen/images/source.png", 480);

        assertTrue(thumbnailUrl.matches("^/storage/thumbs/gen/images/source-w480-[a-z0-9]+\\.jpg$"));
        assertEquals(thumbnailUrl, secondThumbnailUrl);
        Path thumbnailPath = Path.of(service.resolveAbsolutePath(thumbnailUrl));
        assertTrue(Files.isRegularFile(thumbnailPath));
        BufferedImage thumbnail = ImageIO.read(thumbnailPath.toFile());
        assertEquals(480, thumbnail.getWidth());
        assertEquals(320, thumbnail.getHeight());
    }

    @Test
    void createsCachedImageThumbnailForRemoteImages() throws Exception {
        LocalMediaArtifactService service = new LocalMediaArtifactService(storageProperties(), "ffmpeg");
        byte[] imageBytes = pngBytes(900, 600);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/source.png", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, imageBytes.length);
            exchange.getResponseBody().write(imageBytes);
            exchange.close();
        });
        server.start();
        try {
            String sourceUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/source.png";

            String thumbnailUrl = service.ensureImageThumbnail(sourceUrl, 480);
            String secondThumbnailUrl = service.ensureImageThumbnail(sourceUrl, 480);

            assertTrue(thumbnailUrl.matches("^/storage/thumbs/remote/[a-f0-9]{24}-w480\\.jpg$"));
            assertEquals(thumbnailUrl, secondThumbnailUrl);
            Path thumbnailPath = Path.of(service.resolveAbsolutePath(thumbnailUrl));
            assertTrue(Files.isRegularFile(thumbnailPath));
            BufferedImage thumbnail = ImageIO.read(thumbnailPath.toFile());
            assertEquals(480, thumbnail.getWidth());
            assertEquals(320, thumbnail.getHeight());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void ensureMediaThumbnailReusesExistingThumbnailCandidates() {
        LocalMediaArtifactService service = new LocalMediaArtifactService(storageProperties(), "ffmpeg");

        String thumbnailUrl = service.ensureMediaThumbnail(
            "video",
            "/storage/gen/video/source.mp4",
            List.of("/storage/thumbs/gen/images/source-w480.jpg"),
            480
        );

        assertEquals("/storage/thumbs/gen/images/source-w480.jpg", thumbnailUrl);
    }

    @Test
    void concatValidationBehavesAsExpected() throws Exception {
        LocalMediaArtifactService service = new LocalMediaArtifactService(storageProperties(), "ffmpeg");
        LocalMediaArtifactService.StoredArtifact first = service.writeBinary("gen/video", "first.mp4", new byte[] {1});

        IllegalArgumentException tooFewSources = assertThrows(
            IllegalArgumentException.class,
            () -> service.concatVideos("gen/video", "join.mp4", List.of("/storage/a.mp4"))
        );
        assertEquals("at least two source videos are required", tooFewSources.getMessage());

        IllegalStateException nonLocalSource = assertThrows(
            IllegalStateException.class,
            () -> service.concatVideos("gen/video", "join.mp4", List.of(first.publicUrl(), "https://example.com/second.mp4"))
        );
        assertTrue(nonLocalSource.getMessage().contains("source public url is not a local storage path"));
    }

    private JiandouStorageProperties storageProperties() {
        JiandouStorageProperties properties = new JiandouStorageProperties();
        properties.setRootDir(tempDir.toString());
        return properties;
    }

    private byte[] pngBytes(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
