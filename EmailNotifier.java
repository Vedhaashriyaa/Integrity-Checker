import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;
import java.util.logging.Logger;

public class EmailNotifier {
    private final Session session;
    private final String fromEmail;
    private final String toEmail;
    private static final Logger LOGGER = Logger.getLogger(EmailNotifier.class.getName());

    public EmailNotifier() {
        this.fromEmail = "vedhaashriyaa@gmail.com";  // Your email address
        this.toEmail = "vedhaashriyaa@gmail.com";    // Recipient's email address (can be the same as sender)

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("vedhaashriyaa@gmail.com", "your_password");  // Replace "your_password" with your app-specific password
            }
        });
    }

    public void sendAlert(String message) {
        try {
            Message email = new MimeMessage(session);
            email.setFrom(new InternetAddress(fromEmail));
            email.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            email.setSubject("File Integrity Alert");
            email.setText(message);
            Transport.send(email);
            LOGGER.info("Alert email sent: " + message);
        } catch (MessagingException e) {
            LOGGER.severe("Failed to send alert: " + e.getMessage());
        }
    }
    public void sendTestEmail() {
        try {
            Message email = new MimeMessage(session);
            email.setFrom(new InternetAddress(fromEmail));
            email.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            email.setSubject("Test Email");
            email.setText("This is a test email.");
            Transport.send(email);
            LOGGER.info("Test email sent successfully.");
        } catch (MessagingException e) {
            LOGGER.severe("Failed to send test email: " + e.getMessage());
        }
    }

}
