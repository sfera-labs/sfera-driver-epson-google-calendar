package cc.sferalabs.sfera.drivers.google_calendar;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Events;

import cc.sferalabs.sfera.core.Configuration;
import cc.sferalabs.sfera.drivers.Driver;
import cc.sferalabs.sfera.drivers.google_calendar.events.GoogleCalendarEvent;
import cc.sferalabs.sfera.events.Bus;

/**
 *
 * @author Giampiero Baggiani
 *
 * @version 1.0.0
 *
 */
public class GoogleCalendar extends Driver {

	private static final String APPLICATION_NAME = "Sfera - Google Calendar driver";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final List<String> SCOPES = Arrays.asList(CalendarScopes.CALENDAR_READONLY);

	private Calendar cal;
	private List<String> calendars;
	private long pollInterval;

	/**
	 * 
	 * @param id
	 *            the driver instance ID
	 */
	public GoogleCalendar(String id) {
		super(id);
	}

	@Override
	protected boolean onInit(Configuration config) throws InterruptedException {
		String clientSecretParam = config.get("client_secret_file", "client_secret.json");
		Path tempClientSecretPath = Paths.get(clientSecretParam);
		Path clientSecretPath;
		try {
			clientSecretPath = getDriverInstanceDataDir().resolve("client_secret.json");
		} catch (IOException e) {
			log.error("Error creating data dir", e);
			return false;
		}
		if (Files.exists(tempClientSecretPath)) {
			log.debug("Temp client secret file found. Moving it to data dir");
			try {
				Files.move(tempClientSecretPath, clientSecretPath,
						StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				log.error("Error moving client secret file '" + tempClientSecretPath + "'", e);
				return false;
			}
		}

		GoogleClientSecrets clientSecrets;
		try (Reader reader = Files.newBufferedReader(clientSecretPath, StandardCharsets.UTF_8)) {
			clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, reader);
		} catch (NoSuchFileException nsfe) {
			log.error("Client secret file not found");
			return false;
		} catch (Exception e) {
			log.error("Error loading client secret file '" + clientSecretPath + "'", e);
			return false;
		}

		calendars = config.get("calendars", Arrays.asList("primary"));
		pollInterval = config.get("poll_interval", 5) * 1000;

		try {
			NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(
					getDriverInstanceDataDir().toFile());
			GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
					httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
							.setDataStoreFactory(dataStoreFactory).setAccessType("offline").build();

			Credential credential = flow.loadCredential("user");
			if (credential == null || (credential.getRefreshToken() == null
					&& credential.getExpiresInSeconds() <= 60)) {
				String code = config.get("code", null);
				if (code == null || code.isEmpty()) {
					AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl()
							.setRedirectUri("urn:ietf:wg:oauth:2.0:oob");
					log.info(
							"Go to '{}', grant access and copy the obtained code "
									+ "in the 'code' parameter of the driver configuration",
							authorizationUrl.toString());
					return false;
				}

				TokenResponse response = flow.newTokenRequest(code)
						.setRedirectUri("urn:ietf:wg:oauth:2.0:oob").execute();
				credential = flow.createAndStoreCredential(response, "user");
				log.debug("Credentials saved");
			}

			cal = new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
					.setApplicationName(APPLICATION_NAME).build();

		} catch (Exception e) {
			log.error("Authorization error", e);
			return false;
		}

		return true;
	}

	@Override
	protected boolean loop() throws InterruptedException {
		long ts = System.currentTimeMillis();
		DateTime startMax = new DateTime(ts + 1000);
		DateTime endMin = new DateTime(ts);
		try {
			for (String calendarId : calendars) {
				Events events = cal.events().list(calendarId).setTimeMax(startMax)
						.setTimeMin(endMin).setSingleEvents(true).execute();
				String calendarName = events.getSummary().replace(' ', '-');
				Bus.postIfChanged(new GoogleCalendarEvent(this, calendarName, events));
			}
		} catch (Exception e) {
			log.error("Polling error", e);
			return false;
		}

		Thread.sleep(pollInterval);
		return true;
	}

	@Override
	protected void onQuit() {
	}

}
