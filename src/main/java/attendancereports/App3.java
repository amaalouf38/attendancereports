package attendancereports;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.opencsv.exceptions.CsvException;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class App3 {

    private static final Logger LOGGER = LogManager.getLogger(App3.class);
    private static final String CSV_FILE1 = "../attendance/classList.csv";
    private static final String CSV_FILE2 = "../attendance/attendanceList.csv";
    private static final String CSV_FILE3 = "../attendance/studentEmail.csv";
    private static final String DID_NOT_SCAN = "../attendance/didNotScan.csv";
    private static final String CHECKED_IN_LATE = "../attendance/checkedInLate.csv";
    private static final String ON_TIME = "../attendance/onTime.csv";
    private static final String ALL_SCAN = "../attendance/allScan.csv";
    private static final String outputPath = "../attendance/scripts.sql";
    private static final String outputPath_ = "../attendance/";

    private static enum Session {
        CHECKIN, CHECKOUT
    }

    private static enum SessionMode {
        MORNING, AFTERNOON
    }

    // private static final SessionMode SESSION_MODE = SessionMode.MORNING;
    private static final SessionMode SESSION_MODE = SessionMode.AFTERNOON;

    public static void main(String[] args) {
        LOGGER.info("Starting application.");
        try {
            generateAttendanceSheets();
            // generatesqlattendancescript();
            // insertattendancedatabse();
            // generateAttendanceReportForStudent("000000");
            // generateAttendanceReportForStudentAbsences("MissingClasses");
            // sendEmails();
        } catch (Exception e) {
            LOGGER.error("Error in Application: ", e);
        }
        LOGGER.info("Application finished.");
    }

    public static void generateAttendanceSheets() throws IOException, CsvException {
        List<String[]> classList = Utility.readCsv(CSV_FILE1);
        List<String[]> attendanceList = Utility.readCsv(CSV_FILE2);

        Map<String, List<String[]>> latestAttendance = mapLatestAttendance(attendanceList);

        List<String[]> allScan = new LinkedList<>();
        List<String[]> didNotScan = new LinkedList<>();
        List<String[]> checkedInLate = new LinkedList<>();
        List<String[]> onTime = new LinkedList<>();

        prepareHeaders(didNotScan, checkedInLate, onTime, allScan);

        classifyAttendance(classList, latestAttendance, didNotScan, checkedInLate, onTime, allScan);

        Utility.writeCsv(ALL_SCAN, allScan);
        Utility.writeCsv(DID_NOT_SCAN, didNotScan);
        Utility.writeCsv(CHECKED_IN_LATE, checkedInLate);
        Utility.writeCsv(ON_TIME, onTime);
    }

    public static void sendEmails() throws Exception {
        List<String[]> students = Utility.readCsv(CSV_FILE3);
        String subject = Utility.loadresourceasstring("emailSubject.txt");
        String emailBodyTemplate = Utility.loadresourceasstring("emailBody.txt");
        Utility.sendEmails(students, subject, emailBodyTemplate);

    }

    public static void generatesqlattendancescript() throws IOException, CsvException {
        List<String[]> attendanceList = Utility.readCsv(CSV_FILE2);
        Utility.writetoFile(outputPath, generatesqlattendance(attendanceList));
    }

    public static void generatesqlstudentscript() throws IOException, CsvException {
        List<String[]> classList = Utility.readCsv(CSV_FILE1);
        Utility.writetoFile(outputPath, generatesqlstudent(classList));
    }

    public static void insertattendancedatabse() throws IOException, CsvException {
        List<String[]> attendanceList = Utility.readCsv(CSV_FILE2);
        insertsqlattendancescript(attendanceList);
    }

    public static void insertstudentsdatabse() throws IOException, CsvException {
        List<String[]> classList = Utility.readCsv(CSV_FILE1);
        insertsqlstudent(classList);
    }

    private static String generatesqlstudent(List<String[]> classList) {
        StringBuilder builder = new StringBuilder();
        classList.forEach(s -> {
            builder.append(String.format("INSERT INTO student (ID, Name, Color) VALUES ('%s', '%s', '%s');", s[0], s[1],
                    s[2]));
            builder.append(System.lineSeparator());
        });
        return builder.toString();
    }

    private static void insertsqlstudent(List<String[]> classList) {
        classList.forEach(s -> {
            String sql = String.format("INSERT INTO student (ID, Name, Color) VALUES ('%s', '%s', '%s');", s[0], s[1],
                    s[2]);
            try {
                DataBaseUtil.executeUpdate(sql, new ArrayList<>());
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }

        });

    }

    private static void insertsqlattendancescript(List<String[]> attendanceLsitList) {
        attendanceLsitList.forEach(s -> {
            String sql = String.format("INSERT INTO attendance (StudentID,Date ) VALUES ('%s', '%s');", s[0],
                    getDateasString(s[1]));
            try {
                DataBaseUtil.executeUpdate(sql, new ArrayList<>());
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
            }

        });
    }

    private static String generatesqlattendance(List<String[]> attendance) {
        StringBuilder builder = new StringBuilder();
        attendance.forEach(a -> {
            builder.append(String.format("INSERT INTO attendance (StudentID,Date ) VALUES ('%s', '%s');", a[0],
                    getDateasString(a[1])));
            builder.append(System.lineSeparator());
        });
        return builder.toString();
    }

    private static String getDateasString(String s) {
        LocalDateTime curd = tryparseDateFromString(s);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return curd.format(formatter);
    }

    private static Map<String, List<String[]>> mapLatestAttendance(List<String[]> attendanceList) {
        Map<String, List<String[]>> attendanceMap = new HashMap<>();
        for (String[] record : attendanceList) {
            String studentId = record[0];
            if (attendanceMap.containsKey(studentId)) {
                List<String[]> existingList = attendanceMap.get(studentId);
                existingList.add(record);

            } else {
                List<String[]> existingList = new ArrayList<>(2);
                existingList.add(record);
                attendanceMap.put(studentId, existingList);
            }
        }
        return attendanceMap;
    }

    private static LocalDateTime tryparseDateFromString(String dateString) {
        LocalDateTime retuValue = LocalDateTime.MAX;
        try {
            retuValue = Utility.tryparseDateFromString(dateString);
        } catch (DateTimeParseException e) {
            LOGGER.error(e.getMessage());
        }
        return retuValue;

    }

    private static void prepareHeaders(List<String[]>... lists) {
        String[] headers = { "ID", "Name", "Group", "In", "Out", "Scans", "Duration", "Message" };
        for (List<String[]> list : lists) {
            list.add(headers);
        }
    }

    private static int compareDates(String[] s1, String[] s2) {
        return tryparseDateFromString(s1[1]).compareTo(tryparseDateFromString(s2[1]));
    }

    private static void classifyAttendance(List<String[]> classList, Map<String, List<String[]>> attendanceMap,
            List<String[]> didNotScan, List<String[]> checkedInLate, List<String[]> onTime, List<String[]> allScan) {
        for (String[] student : classList) {
            String studentId = student[0];
            if (!attendanceMap.containsKey(studentId)) {
                didNotScan.add(new String[] { studentId, student[1], student[2], "Not Scanned" });
                continue;
            }
            List<String[]> attendanceRecord = attendanceMap.get(studentId);
            String[] checkinRecod = attendanceRecord.stream()
                    .min(App3::compareDates)
                    .orElse(null);
            String[] checkoutRecod = attendanceRecord.stream()
                    .max(App3::compareDates)
                    .orElse(null);

            LocalDateTime checkInTime = tryparseDateFromString(checkinRecod[1]);
            LocalDateTime checkOutTime = tryparseDateFromString(checkoutRecod[1]);
            Duration duration = Duration.between(checkInTime, checkOutTime);
            double numHours = duration.toMinutes() / 60.0;
            String numHoursString = String.format("%.2f", numHours);
            String numScans = String.format("%d", attendanceRecord.size());

            allScan.add(new String[] { studentId, student[1], student[2], checkinRecod[1], checkoutRecod[1],
                    numScans, numHoursString, "" });

            StringBuilder message = new StringBuilder();
            boolean late = false;

            if (attendanceRecord.size() == 1) {
                message.append("Did not scan in and out.");
            }

            if (inLate(checkInTime)) {
                message.append(" In Late");
                late = true;
            }
            if (outEarly(checkOutTime)) {
                message.append(" Out Early");
                late = true;
            }

            if (late) {
                checkedInLate.add(new String[] { studentId, student[1], student[2], checkinRecod[1], checkoutRecod[1],
                        numScans, numHoursString, message.toString() });

            } else {
                onTime.add(new String[] { studentId, student[1], student[2], checkinRecod[1], checkoutRecod[1],
                        numScans, numHoursString, message.toString() });
            }
        }
    }

    private static boolean inLate(LocalDateTime checkInTime) {
        if (checkInTime == null)
            return true;

        LocalDate datePart = checkInTime.toLocalDate();
        LocalTime th1 = null;

        if (SESSION_MODE == SessionMode.MORNING) {
            th1 = LocalTime.of(10, 30);
            return checkInTime.compareTo(LocalDateTime.of(datePart, th1)) >= 0;

        } else {

            th1 = LocalTime.of(16, 00);
            return checkInTime.compareTo(LocalDateTime.of(datePart, th1)) >= 0;

        }

    }

    private static boolean outEarly(LocalDateTime checkOutTime) {
        if (checkOutTime == null)
            return true;

        LocalDate datePart = checkOutTime.toLocalDate();
        LocalTime th2 = null;

        if (SESSION_MODE == SessionMode.MORNING) {
            th2 = LocalTime.of(11, 45);
            return checkOutTime.compareTo(LocalDateTime.of(datePart, th2)) < 0;

        } else {
            th2 = LocalTime.of(16, 00);
            return checkOutTime.compareTo(LocalDateTime.of(datePart, th2)) < 0;
        }
    }

    public static void generateAttendanceReportForStudent(String s) throws IOException, SQLException {
        String query = "attedanceReport.sql";
        List<Object> parameters = new ArrayList<>();
        parameters.add(s);
        String[] headers1 = { "Attendance Report for: ", s };
        String[] headers2 = { "Generated On:",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) };
        String[] headers3 = { "ID", "Name", "Group", "Day", "Session", "In", "Out", "Scans", "Duration", "Absent",
                "OneWayScan" };
        DataBaseUtil.generateReport(query, parameters, outputPath_ + s + ".csv",
                Arrays.asList(headers1, headers2, headers3));

    }

    public static void generateAttendanceReportForStudentAbsences(String s) throws IOException, SQLException {
        String query = "missingClasses.sql";
        List<Object> parameters = new ArrayList<>();
        parameters.add(4);
        String[] headers1 = { "Students missing more than 3 sessions: " };
        String[] headers2 = { "Generated On:",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) };
        String[] headers3 = { "ID", "Absent",
                "OneWayScan" };
        DataBaseUtil.generateReport(query, parameters, outputPath_ + s + ".csv",
                Arrays.asList(headers1, headers2, headers3));

    }
}
