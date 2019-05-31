package io.pivotal.weeklyrotator;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;

@Component
public class GMailService {

    private static final String APPLICATION_NAME = "Weekly Report Rotator";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    @Autowired
    private GoogleApiAuthService authService;
    private Gmail gmail;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${weekly_rotator.mail.from}")
    private String from;

    @Value("${weekly_rotator.mail.to}")
    private String to;

    @PostConstruct
    public void init() throws GeneralSecurityException, IOException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        gmail = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, authService.getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public void sendWeeklyRotated(String url) {
        try {
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            MimeMessageHelper messageHelper = new MimeMessageHelper(new MimeMessage(session), true, "UTF-8");

            messageHelper.setFrom(from);
            messageHelper.setTo(to);
            messageHelper.setSubject("Weekly Report Rotated");

            Context ctx = new Context();
            ctx.setVariable("url", url);

            messageHelper.setText(this.templateEngine.process("weekly-rotated.txt", ctx), this.templateEngine.process("weekly-rotated.html", ctx));

            Message message = getBase64UrlEncodedMessage(messageHelper);
            gmail.users().messages().send("jhoffmann@pivotal.io", message).execute();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendWeeklyReminder(String url) {
        try {
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            MimeMessageHelper messageHelper = new MimeMessageHelper(new MimeMessage(session), true, "UTF-8");

            messageHelper.setFrom(from);
            messageHelper.setTo(to);
            messageHelper.setSubject("Weekly Report Reminder");

            Context ctx = new Context();
            ctx.setVariable("url", url);

            messageHelper.setText(this.templateEngine.process("weekly-reminder.txt", ctx), this.templateEngine.process("weekly-reminder.html", ctx));

            Message message = getBase64UrlEncodedMessage(messageHelper);
            gmail.users().messages().send("jhoffmann@pivotal.io", message).execute();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Message getBase64UrlEncodedMessage(MimeMessageHelper messageHelper) throws IOException, MessagingException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        messageHelper.getMimeMessage().writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }
}
