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
import org.springframework.scheduling.annotation.Scheduled;
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
public class TestFunction {

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

    @ShellMethod(value = "Check weekly reports", key = "check")
    @Scheduled(cron = "0 0 9 ? * MON")
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
    @Scheduled(cron = "0 0 9 ? * WED")
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

        // This will create a copy without a link. There fore we have to get the file again
        File copy = gdrive.files().copy(WEEKLY_REPORT_TEMPLATE_ID, weeklyReport).execute();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Getting the newly created weekly report with the link to it
        copy = getWeeklyReport(copy.getName());

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

    @ShellMethod("End the rotator")
    public void quit() {
        System.exit(0);
    }
}
