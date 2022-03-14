package com.vantar.util.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;


public class Email {

    private static final Logger log = LoggerFactory.getLogger(Email.class);


    static public void send(String recipient, String sender, String subject, String body) {
        Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", "localhost");
        Session sess = Session.getDefaultInstance(properties);

        try {
            Message message = new MimeMessage(sess);
            message.setFrom(new InternetAddress(sender));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
        } catch (MessagingException e) {
            log.error("! failed to send email({} < {})", recipient, subject, e);
        }
    }

}