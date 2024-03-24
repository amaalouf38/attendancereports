package attendancereports;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.Session;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Message;
import javax.mail.MessagingException;

public class Utility {

    public static String loadresourceasstring(String resourceName) throws IOException {
        try (InputStream inputStream = DataBaseUtil.class.getClassLoader().getResourceAsStream(resourceName);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    public static List<String[]> readCsv(String csvFile) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            return reader.readAll();
        }
    }

    public static void writeCsv(String filePath, List<String[]> data) throws IOException {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            writer.writeAll(data);
        }
    }

    public static void writetoFile(String filePath, String s) throws IOException {
        try (PrintWriter writer = new PrintWriter(filePath)) {
            writer.println(s);
        }
    }

    public static String getDateasString(String s) {
        LocalDateTime curd = LocalDateTime.parse(s, DateTimeFormatter.ofPattern("M/d/yyyy H:mm"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return curd.format(formatter);
    }

    public static LocalDateTime tryparseDateFromString(String dateString) {
        return LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("M/d/yyyy H:mm"));
    }

    private static final String FROM_EMAIL = "myemail@gmail.com";
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_AUTH = "true";
    private static final String SMTP_PORT = "587";

    public static void sendEmails(List<String[]> students, String subject, String emailBodyTemplate)
            throws MessagingException {

        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.auth", SMTP_AUTH);
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.port", SMTP_PORT);

        if (SMTP_AUTH.equals("true")) {
            props.put("mail.smtp.user", FROM_EMAIL);
            props.put("mail.smtp.password", "App Password"); // Use App Password if 2-Step)
        }

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(props.get("mail.smtp.user").toString(),
                        props.get("mail.smtp.password").toString());
            }
        });
        // 0 Student Name, 1 Student Email
        for (String[] student : students) {

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(student[1]));
            message.setSubject(subject);

            String personalizedBody = String.format(emailBodyTemplate, student[0], student[1]);
            message.setContent(personalizedBody, "text/html"); // Set content type (text/plain or text/html)

            Transport.send(message);
            System.out.println("Email sent to " + student[1]);
        }

    }

}
