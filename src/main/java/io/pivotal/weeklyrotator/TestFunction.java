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
import org.springframework.shell.standard.ShellOption;

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
        // Build the Name of the Weekly Report
        LocalDateTime now = LocalDateTime.now();
        TemporalField fieldISO = WeekFields.of(Locale.GERMANY).dayOfWeek();

        DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH);
        String weeklyReportName = String.format("Weekly Report - %s", format.format(now.with(fieldISO, 5L)));

        System.out.println(String.format("Searching for: name = '%s' and '%s' in parents", weeklyReportName, WEEKLY_REPORT_FOLDER_ID));

        // Check if a weekly report document already exists
        FileList result = gdrive.files().list()
                .setQ(String.format("name = '%s' and '%s' in parents", weeklyReportName, WEEKLY_REPORT_FOLDER_ID))
                .setSpaces("drive")
                //.setFields("nextPageToken, files(id, name)")
                .setFields("*")
                .execute();
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("Weekly Report not found, creating it");
            File weeklyReport = new File();
            weeklyReport.setName(weeklyReportName);
            weeklyReport.setParents(Collections.singletonList(WEEKLY_REPORT_FOLDER_ID));

            File copy = gdrive.files().copy(WEEKLY_REPORT_TEMPLATE_ID, weeklyReport).execute();
            System.out.printf("Copy created %s (%s) link: %s\n", copy.getName(), copy.getId(), copy.getWebViewLink());

            gmail.sendWeeklyRotated(copy.getWebViewLink());
        } else {
            System.out.println("Weekly Report already exists:");
            for (File file : files) {
                System.out.printf("%s (%s) link: %s\n", file.getName(), file.getId(), file.getWebViewLink());
            }
        }
    }

    @ShellMethod(value = "Add numbers.", key = "sum")
    public int add(int a, int b) {
        return a + b;
    }

    @ShellMethod(value = "Display stuff.", prefix = "-")
    public String echo(int a, int b, @ShellOption("--third") int c) {
        return String.format("You said a=%d, b=%d, c=%d", a, b, c);
    }
}
