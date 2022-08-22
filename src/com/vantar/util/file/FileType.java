package com.vantar.util.file;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.*;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.*;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;
import java.io.*;
import java.nio.file.*;
import java.util.*;


public class FileType {

    private MediaType mediaType;
    private TikaInputStream stream;
    private String filepath;


    public FileType() {

    }

    public FileType(String filepath) throws FileNotFoundException {
        this.filepath = filepath;
        stream = TikaInputStream.get(new FileInputStream(filepath));
        detect();
    }

    public FileType(Path filepath) throws IOException {
        this.filepath = filepath.toAbsolutePath().toString();
        stream = TikaInputStream.get(Files.newInputStream(filepath));
        detect();
    }

    public FileType(InputStream inputStream, String filepath) {
        this.filepath = filepath;
        stream = TikaInputStream.get(inputStream);
        detect();
    }

    public FileType(InputStream inputStream) {
        stream = TikaInputStream.get(inputStream);
        detect();
    }

    public FileType(byte[] bytes) {
        stream = TikaInputStream.get(bytes);
        detect();
    }

    private void detect() {
        try {
            Metadata metadata = new Metadata();
            if (filepath != null) {
                metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filepath);
            }
            mediaType = TikaConfig.getDefaultConfig().getDetector().detect(stream, metadata);
        } catch (IOException ignore) {

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
        if (filepath != null) {
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filepath);
        }
        BodyContentHandler handler = new BodyContentHandler();
        ParseContext context = new ParseContext();
        try {
            parser.parse(stream, handler, metadata, context);
        } catch (IOException | SAXException | TikaException e) {
            return null;
        }

        Map<String,String> meta = new HashMap<>(metadata.names().length, 1);
        for (String key : metadata.names()) {
            meta.put(key, metadata.get(key));
        }
        return meta;
    }

    public String getMimeType() {
        return mediaType == null ? null : mediaType.getType() + '/' + mediaType.getSubtype();
    }

    public boolean isType(String... type) {
        if (mediaType == null) {
            return false;
        }
        for (String t : type) {
            if (mediaType.getSubtype().equalsIgnoreCase(t) || mediaType.getType().equalsIgnoreCase(t)
                || getMimeType().equalsIgnoreCase(t)) {
                return true;
            }
        }
        return false;
    }
}