import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.logging.Logger;

public class EmailNotifier {
    private final Session session;
    private final String fromEmail;
    private final String toEmail;
    private static final Logger LOGGER = Logger.getLogger(EmailNotifier.class.getName());

    public EmailNotifier(String configPath) {
        Properties props = loadConfig(configPath);
        this.fromEmail = props.getProperty("mail.from");
        this.toEmail = props.getProperty("mail.to");
        
        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                    props.getProperty("mail.username"),
                    props.getProperty("mail.password")
                );
            }
        });
    }

    private Properties loadConfig(String path) {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(path)) {
            props.load(fis);
        } catch (IOException e) {
            LOGGER.severe("Failed to load email config: " + e.getMessage());
            throw new RuntimeException("Email configuration failed", e);
        }
        return props;
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
}
