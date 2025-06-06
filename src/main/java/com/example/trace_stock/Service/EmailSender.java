package com.example.trace_stock.Service;


import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;


public class EmailSender {

    public static void sendEmail(String to, String resetLink) {
        final String from = "mohamed.saifi-etu@etu.univh2c.ma";
        final String password = "";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(from, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(
                    Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("Réinitialisation du mot de passe");
            message.setText("Cliquez sur ce lien pour réinitialiser votre mot de passe :\n" + resetLink);

            Transport.send(message);

            System.out.println("Email envoyé à : " + to);

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
