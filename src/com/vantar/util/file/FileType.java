package com.vantar.util.file;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.*;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.*;
import org.xml.sax.SAXException;
import java.io.*;
import java.nio.file.*;
import java.util.*;


public class FileType {

    private static final Logger log = LoggerFactory.getLogger(FileType.class);
    private MediaType mediaType;
    private final TikaInputStream stream;


    public FileType(String filepath) throws FileNotFoundException {
        stream = TikaInputStream.get(new FileInputStream(filepath));
        detect(stream);
    }

    public FileType(Path filepath) throws IOException {
        stream = TikaInputStream.get(Files.newInputStream(filepath));
        detect(stream);
    }

    public FileType(InputStream inputStream) {
        stream = TikaInputStream.get(inputStream);
        detect(stream);
    }

    public FileType(byte[] bytes) {
        stream = TikaInputStream.get(bytes);
        detect(stream);
    }

    private void detect(InputStream inputStream) {
        Detector detector = TikaConfig.getDefaultConfig().getDetector();
        try {
            mediaType = detector.detect(inputStream, new Metadata());
        } catch (IOException e) {
            log.error("! io error", e);
        }
    }

    public String getType() {
        return mediaType == null ? null : mediaType.getType();
    }

    public String getSubType() {
        return mediaType == null ? null : mediaType.getSubtype();
    }

    public Map<String,String> getParams() {
        return mediaType == null ? null : mediaType.getParameters();
    }

    public Map<String,String> getMeta() {
        Parser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        BodyContentHandler handler = new BodyContentHandler();
        ParseContext context = new ParseContext();
        try {
            parser.parse(stream, handler, metadata, context);
        } catch (IOException | SAXException | TikaException e) {
            e.printStackTrace();
            return null;
        }

        Map<String,String> meta = new HashMap<>(metadata.names().length);
        for (String key : metadata.names()) {
            meta.put(key, metadata.get(key));
        }
        return meta;
    }

    public boolean isType(String... type) {
        if (mediaType == null) {
            return false;
        }
        for (String t : type) {
            if (mediaType.getSubtype().equals(t) || mediaType.getType().equals(t)) {
                return true;
            }
        }
        return false;
    }
}