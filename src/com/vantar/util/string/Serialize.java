package com.vantar.util.string;

import com.vantar.exception.SerializeException;
import java.io.*;
import java.util.Base64;


public class Serialize {

    public static Object fromString(String s) throws SerializeException {
        try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(s)))) {
            return stream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializeException(e);
        }
    }

    public static String toString(Serializable o) throws SerializeException {
        try (
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream stream = new ObjectOutputStream(byteStream)
        ) {
            stream.writeObject(o);
            return Base64.getEncoder().encodeToString(byteStream.toByteArray());
        } catch (IOException e) {
            throw new SerializeException(e);
        }
    }
}
