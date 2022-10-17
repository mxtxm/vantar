package com.vantar.util.os;

import com.jcraft.jsch.*;
import com.vantar.admin.model.Admin;
import java.io.*;
import java.util.*;


public class Command {

    public static String run(String command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = input.readLine()) != null) {
                sb.append(line).append('\n');
            }
            process.waitFor();
            process.destroy();
        }
        return sb.toString();
    }

    public static String runX(String command, String password) throws IOException, InterruptedException {
        String[] cmd = {"/bin/bash","-c","echo " + password + "| sudo -S " + command};
        Process process = Runtime.getRuntime().exec(cmd);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = input.readLine()) != null) {
                sb.append(line).append('\n');
            }
            process.waitFor();
            process.destroy();
        }
        return sb.toString();
    }



    public static String run(String password, String command) {
        String user = "lynx";
        StringBuilder sb = new StringBuilder();
        String host = "127.0.0.1";
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        JSch jsch = new JSch();
        Session session;
        try {
            session = jsch.getSession(user, host, 22);
            session.setPassword(password);
            session.setConfig(config);
            session.connect();
            System.out.println("Connected to " + host);
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand("sudo -S -p '' " + command);
            channel.setInputStream(null);
            OutputStream out = channel.getOutputStream();
            ((ChannelExec) channel).setErrStream(System.err);
            InputStream in = channel.getInputStream();
            ((ChannelExec) channel).setPty(true);
            channel.connect();
            out.write((password + "\n").getBytes());
            out.flush();
            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    System.out.print(new String(tmp, 0, i));
                    sb.append(new String(tmp, 0, i)).append("\n");
                }
                if (channel.isClosed()) {
                    System.out.println("Exit status: " + channel.getExitStatus());
                    sb.append(channel.getExitStatus());
                    break;
                }
            }
            channel.disconnect();
            session.disconnect();
            System.out.println("DONE");
        } catch (JSchException | IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

}
