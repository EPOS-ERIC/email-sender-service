package org.epos.core;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.epos.api.utility.EmailUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.swagger.model.Email;
import jakarta.mail.Authenticator;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import okhttp3.Credentials;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class EmailSenderHandler {


	private static final Logger LOGGER = LoggerFactory.getLogger(EmailSenderHandler.class);

	private static String subjectForwardedMessage = " | Receipt Confirmation";

	private static String forwardedMessage = "Thank you for your message. \n"
			+ "We will get in touch with you shortly.\n"
			+ "Copy of the message.\n\n-----------------------------------------------------\n\n";


	public static Map<String, Object> handle(JsonObject payload,Email sendEmail, Map<String, Object> requestParams) throws MessagingException, UnsupportedEncodingException {

		JsonArray mails = payload.get("emails").getAsJsonArray();
		String[] recipientEmails = resolveTargetEmails(mails);
		String from = requestParams.get("email").toString();
		String firstName = requestParams.get("firstName").toString();
		String lastName = requestParams.get("lastName").toString();

		String messageBody = "From: "+from+"\n" + sendEmail.getBodyText();
		if("SMTP".equals(System.getenv("MAIL_TYPE"))){
			sendViaSMTP(recipientEmails, sendEmail.getSubject(), messageBody);
		}
		if("API".equals(System.getenv("MAIL_TYPE"))){
			try {
				sendViaAPI(recipientEmails, sendEmail.getSubject(), messageBody);
				sendForwardViaAPI(from, sendEmail.getSubject(), forwardedMessage + sendEmail.getBodyText(), firstName, lastName);
			} catch (IOException | InterruptedException e) {
				LOGGER.error(e.getLocalizedMessage());
			}
		}

		return new HashMap<String, Object>();
	}

	public static Map<String, Object> handleDirect(String[] recipientEmails, Email sendEmail) throws MessagingException, UnsupportedEncodingException {

		String[] targetEmails = resolveTargetEmails(recipientEmails);

		if(!"API".equals(System.getenv("MAIL_TYPE"))) {
			throw new MessagingException("Direct email sending requires MAIL_TYPE=API");
		}

		try {
			sendSingleMessageViaAPI(targetEmails, sendEmail.getSubject(), sendEmail.getBodyText());
		} catch (IOException | InterruptedException e) {
			LOGGER.error(e.getLocalizedMessage());
		}

		return new HashMap<String, Object>();
	}

	private static String[] resolveTargetEmails(JsonArray mails) {
		String[] emails = new String[mails.size()];
		for (int i = 0; i < mails.size(); i++) {
			emails[i] = mails.get(i).getAsString();
		}
		return resolveTargetEmails(emails);
	}

	private static String[] resolveTargetEmails(String[] requestedEmails) {
		if (isProduction()) {
			return requestedEmails;
		}
		return System.getenv("DEV_EMAILS").split(";");
	}

	private static boolean isProduction() {
		return "production".equals(System.getenv("ENVIRONMENT_TYPE"));
	}

	public static void sendViaSMTP(String[] emails, String subject, String bodyText) {
		Properties props = new Properties();
		{
			props.setProperty("mail.smtp.auth", "true");
			props.setProperty("mail.smtp.host", System.getenv("MAIL_HOST"));
			props.setProperty("mail.smtp.port", "587");
		}

		Authenticator auth = new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication( System.getenv("MAIL_USER"), System.getenv("MAIL_PASSWORD"));
			}
		};

		for(String email : emails) {
			LOGGER.info("Preparing email to: "+email.toString());
			LOGGER.info("Using properties: "+props.toString());
			LOGGER.info("Using Auth: "+auth.toString());
			LOGGER.info("Creating a new session");
			Session session = Session.getDefaultInstance(props, auth);
			LOGGER.info("New session created, sending email");
			EmailUtil.sendEmail(session, email,subject, bodyText);
			LOGGER.info("End session");
		}
	}

	public static void sendViaAPI(String[] emails, String subject, String bodyText) throws IOException, InterruptedException {	
		for(String email : emails) {
			sendApiEmail(new String[] { email }, subject, bodyText);
		}
	}

	public static void sendSingleMessageViaAPI(String[] emails, String subject, String bodyText) throws IOException, InterruptedException {
		sendApiEmail(emails, subject, bodyText);
	}

	private static void sendApiEmail(String[] emails, String subject, String bodyText) throws IOException, InterruptedException {
		OkHttpClient client = new OkHttpClient();

		String[] apiKey = System.getenv("MAIL_API_KEY").split(":");
		String credential = Credentials.basic(apiKey[0], apiKey[1]);

		MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("from", System.getenv("SENDER_NAME")+" <"+System.getenv("SENDER")+"@"+System.getenv("SENDER_DOMAIN")+">")
				.addFormDataPart("subject", subject)
				.addFormDataPart("text", bodyText);
		for (String email : emails) {
			requestBodyBuilder.addFormDataPart("to", email);
		}

		RequestBody requestBody = requestBodyBuilder.build();

		Request request = new Request.Builder()
				.url(System.getenv("MAIL_API_URL"))
				.post(requestBody)
				.header("Authorization", credential)
				.build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
			response.body().string();
		}
	}

	public static void sendForwardViaAPI(String from, String subject, String bodyText, String firstName, String lastName) throws IOException, InterruptedException {	
		String dear = "Dear ";
		if(firstName!=null) dear+=firstName+" ";
		if(lastName!=null) dear+=lastName;
		if(firstName==null && lastName==null) dear+="User";
		dear+=",\n";
		
		subject += subjectForwardedMessage;
		
		OkHttpClient client = new OkHttpClient();

		String[] apiKey = System.getenv("MAIL_API_KEY").split(":");

		String credential = Credentials.basic(apiKey[0], apiKey[1]);

		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("from", System.getenv("SENDER_NAME")+" <"+System.getenv("SENDER")+"@"+System.getenv("SENDER_DOMAIN")+">")
				.addFormDataPart("to", from)
				.addFormDataPart("subject", subject)
				.addFormDataPart("text", dear)
				.addFormDataPart("text", bodyText)
				.build();

		Request request = new Request.Builder()
				.url(System.getenv("MAIL_API_URL"))
				.post(requestBody)
				.header("Authorization", credential)
				.build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
			response.body().string();
		}

	}

}
