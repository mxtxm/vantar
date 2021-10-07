package com.vantar.util.string;

import com.vantar.exception.SerializeException;
import java.io.*;
import java.util.Base64;


public class Serialize {

    public static Object fromString(String s) throws SerializeException {
        byte[] data = Base64.getDecoder().decode(s);

        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(new ByteArrayInputStream(data));
            Object o = ois.readObject();
            ois.close();
            return o;
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializeException(e);
        }
    }

    public static String toString(Serializable o) throws SerializeException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(o);
            oos.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new SerializeException(e);
        }
    }
}
