package io.pivotal.weeklyrotator;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@ShellComponent
public class InteractiveShellFunctions {

    private static final String APPLICATION_NAME = "Weekly Report Rotator";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();


    @Value("${weekly_rotator.weekly_report_template_id}")
    private String WEEKLY_REPORT_TEMPLATE_ID;

    @Value("${weekly_rotator.weekly_report_folder_id}")
    private String WEEKLY_REPORT_FOLDER_ID;

    private Drive gdrive;

    @Autowired
    private GMailService gmail;

    @Autowired
    private GoogleApiAuthService authService;

    @PostConstruct
    public void init() throws GeneralSecurityException, IOException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        gdrive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, authService.getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public void run() {
        try {
            check();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @ShellMethod(value = "Check weekly reports", key = "check")
    public void check() throws IOException {
        String weeklyReportName = getWeeklyReportName();
        File weekly = getWeeklyReport(weeklyReportName);

        if (weekly == null) {
            File copy = createWeekly(weeklyReportName);
            gmail.sendWeeklyRotated(copy.getWebViewLink());

        } else {
            System.out.printf("Weekly Report already exists: %s (%s) link: %s\n", weekly.getName(), weekly.getId(), weekly.getWebViewLink());
        }
    }

    @ShellMethod(value = "Send a weekly Report Reminder", key = "reminder")
    public void reminder() throws IOException {
        String weeklyReportName = getWeeklyReportName();
        File weekly = getWeeklyReport(weeklyReportName);

        if (weekly == null) {
            File copy = createWeekly(weeklyReportName);
            gmail.sendWeeklyReminder(copy.getWebViewLink());
        } else {
            gmail.sendWeeklyReminder(weekly.getWebViewLink());
        }
    }

    private File createWeekly(String weeklyReportName) throws IOException {
        System.out.println("Weekly Report not found, creating it");
        File weeklyReport = new File();
        weeklyReport.setName(weeklyReportName);
        weeklyReport.setParents(Collections.singletonList(WEEKLY_REPORT_FOLDER_ID));

        // This will create a copy with all fields returned in the response (.setFields("*"))
        File copy = gdrive.files().copy(WEEKLY_REPORT_TEMPLATE_ID, weeklyReport).setFields("*").execute();

        System.out.printf("Copy created %s (%s) link: %s\n", copy.getName(), copy.getId(), copy.getWebViewLink());
        return copy;
    }

    private String getWeeklyReportName() {
        // Build the Name of the Weekly Report
        LocalDateTime now = LocalDateTime.now();
        TemporalField fieldISO = WeekFields.of(Locale.GERMANY).dayOfWeek();

        DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH);
        return String.format("Weekly Report - %s", format.format(now.with(fieldISO, 5L)));
    }

    private File getWeeklyReport(String name) throws IOException {
        System.out.println(String.format("Searching for: name = '%s' in folder '%s'", name, WEEKLY_REPORT_FOLDER_ID));

        // Check if a weekly report document already exists
        FileList result = gdrive.files().list()
                .setQ(String.format("name = '%s' and '%s' in parents", name, WEEKLY_REPORT_FOLDER_ID))
                .setSpaces("drive")
                //.setFields("nextPageToken, files(id, name)")
                .setFields("*")
                .execute();
        List<File> files = result.getFiles();
        return (files == null || files.isEmpty()) ? null : files.get(0);
    }

/*    @ShellMethod("End the rotator")
    public void quit() {
        System.exit(0);
    }*/
}
