# weekly-rotator
Spring Shell Application to run on my local workstation at specific times, which does trivial admin tasks for me like rotating the teams weekly and sends mails as well as reminders


I have setup the program in my dev directory and ran

`mvn clean package`

then I had to create the following 2 files in `$HOME/Library/LaunchAgents`

- `io.pivotal.weekly-rotator.check.plist`
- `io.pivotal.weekly-rotator.reminder.plist`

you can find them in `src/main/resources` for your convinient integration

`io.pivotal.weekly-rotator.check.plist` is run everyday at 10am. If it finds a weekly report for the current week in the 
configured folder, it does nothing and silently exits. If the new report is missing, it rotates the template (by using its 
configured id) and creates a copy of the template with a specific name in the folder with the configured folder. Then it sends
an E-Mail defined in `src/main/resources/templates/weekly-rotated.(html|txt)` to the configured recipient e-mail address to
distribute the new document and link.

The reminder mail works similarly.


```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>Disabled</key>
	<false/>
	<key>Label</key>
	<string>io.pivotal.weekly-rotator.check</string>
	<key>ProgramArguments</key>
	<array>
		<string>/Users/buddy/.jenv/shims/java</string>
		<string>-Dspring.profiles.active=prod</string>
		<string>-jar</string>
		<string>target/weekly-rotator-0.0.1-SNAPSHOT.jar</string>
		<string>check</string>
	</array>
	<key>StartCalendarInterval</key>
	<dict>
		<key>Hour</key>
		<integer>10</integer>
		<key>Minute</key>
		<integer>0</integer>
	</dict>
	<key>WorkingDirectory</key>
	<string>/Users/buddy/dev/weekly-rotator</string>
</dict>
</plist>
```

The tool uses the following 2 files for configuration `application-dev.yml`
and `application-prod.yml` within those there are 2 sections. `spring` and `weekly-rotator`

Leave `spring` alone. Only configure `weekly-rotator` section. 

```
weekly_rotator:
  weekly_report_folder_id: <THE FOLDER_ID CONTAINING THE WEEKLY REPORTS>
  weekly_report_template_id: <THE ID OF THE WEEKLY REPORT TEMPLATE>
  mail:
    from: <FROM WHICH EMAIL TO ADDRESS TO SEND>
    to: <TO WHICH EMAIL ADDRESS TO SEND>
```
