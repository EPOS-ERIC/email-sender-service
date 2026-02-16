package io.swagger.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.epos.core.ContactPointGet;
import org.epos.core.EmailSenderHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.swagger.model.Email;

import io.swagger.model.ProviderType;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2023-08-04T13:31:01.781679391Z[GMT]")
@RestController
public class SenderApiController implements SenderApi {

	@org.springframework.beans.factory.annotation.Autowired
	public SenderApiController(ObjectMapper objectMapper, HttpServletRequest request) {
		this.objectMapper = objectMapper;
		this.request = request;
	}

	private static final Logger log = LoggerFactory.getLogger(SenderApiController.class);

	private final ObjectMapper objectMapper;

	private final HttpServletRequest request;

	public ResponseEntity<Email> sendEmailPost(
			@NotNull @Parameter(in = ParameterIn.QUERY, description = "Id of the resource", required = true, schema = @Schema()) @Valid @RequestParam(value = "id", required = true) String id,
			@NotNull @Parameter(in = ParameterIn.QUERY, description = "Contact point type", required = true, schema = @Schema()) @Valid @RequestParam(value = "contactType", required = true) ProviderType contactType,
			@Parameter(in = ParameterIn.DEFAULT, description = "", schema = @Schema()) @Valid @RequestBody Email body) {
		String accept = request.getHeader("Accept");
		String userEmail = request.getParameter("userEmail");
		String firstName = request.getParameter("firstName");
		String lastName = request.getParameter("lastName");
		log.info("Received sendEmailPost request: id='{}', contactType='{}', userEmail='{}', accept='{}'", id,
				contactType, userEmail, accept);
		if (accept != null && accept.contains("application/json")) {
			try {
				final Map<String, Object> requestParameters = new HashMap<String, Object>();
				if (StringUtils.isBlank(id) && StringUtils.isBlank(userEmail)) {
					log.warn("Rejecting sendEmailPost with BAD_REQUEST: both 'id' and 'userEmail' are blank");
					return new ResponseEntity<Email>(HttpStatus.BAD_REQUEST);
				}

				requestParameters.put("id", id);
				requestParameters.put("type", contactType);
				requestParameters.put("email", userEmail);
				requestParameters.put("firstName", firstName);
				requestParameters.put("lastName", lastName);
				log.info(requestParameters.toString());

				redirectRequest(requestParameters, body);

				return new ResponseEntity<Email>(objectMapper
						.readValue("{\n  \"bodyText\" : \"bodyText\",\n  \"subject\" : \"subject\"\n}", Email.class),
						HttpStatus.ACCEPTED);
			} catch (IOException e) {
				log.error("Couldn't serialize response for content type application/json", e);
				return new ResponseEntity<Email>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		log.warn("Rejecting sendEmailPost with NOT_IMPLEMENTED: Accept header '{}' is not supported", accept);
		return new ResponseEntity<Email>(HttpStatus.NOT_IMPLEMENTED);
	}

	public ResponseEntity<Email> sendEmailToGroupPost(@NotBlank @PathVariable String group,
			@Valid @RequestBody Email body) {
		log.info("Received sendEmailToGroupPost request: group='{}', subject='{}', bodyTextLength={}", group,
				body != null ? body.getSubject() : null,
				body != null && body.getBodyText() != null ? body.getBodyText().length() : 0);

		String normalizedGroup = StringUtils.trimToNull(group);
		if (normalizedGroup == null) {
			log.warn("Rejecting sendEmailToGroupPost with BAD_REQUEST: group is blank");
			return new ResponseEntity<Email>(HttpStatus.BAD_REQUEST);
		}

		if (!normalizedGroup.equals(group)) {
			log.info("Normalized group path variable from '{}' to '{}'", group, normalizedGroup);
		}

		JsonArray emails = ContactPointGet.generateEmailListForGroup(normalizedGroup);
		log.info("Resolved {} recipient emails for group '{}'", emails.size(), normalizedGroup);
		if (emails.size() == 0) {
			log.warn("Rejecting sendEmailToGroupPost with BAD_REQUEST: no contact emails found for group '{}'",
					normalizedGroup);
			return new ResponseEntity<Email>(HttpStatus.BAD_REQUEST);
		}

		JsonObject response = new JsonObject();
		response.add("emails", emails);
		Map<String, Object> requestParams = new HashMap<String, Object>();
		requestParams.put("email", "system@epos");
		requestParams.put("firstName", "System");
		requestParams.put("lastName", "");

		try {
			log.info("Dispatching email to group '{}' with {} recipients", normalizedGroup, emails.size());
			EmailSenderHandler.handle(response, body, requestParams);
		} catch (UnsupportedEncodingException | MessagingException e) {
			log.error("Couldn't send email to group '{}'", normalizedGroup, e);
			return new ResponseEntity<Email>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		log.info("Email sent to {} contacts in group: {}", emails.size(), normalizedGroup);

		try {
			log.info("Returning ACCEPTED for sendEmailToGroupPost, group='{}'", normalizedGroup);
			return new ResponseEntity<Email>(objectMapper
					.readValue("{\n  \"bodyText\" : \"bodyText\",\n  \"subject\" : \"subject\"\n}", Email.class),
					HttpStatus.ACCEPTED);
		} catch (Exception e) {
			log.error("Error creating response for group '{}'", normalizedGroup, e);
			return new ResponseEntity<Email>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private ResponseEntity<Email> redirectRequest(Map<String, Object> requestParams, Email sendEmail) {
		JsonObject response = ContactPointGet.generate(new JsonObject(), requestParams);

		try {
			EmailSenderHandler.handle(response, sendEmail, requestParams);
		} catch (UnsupportedEncodingException | MessagingException e) {
			log.error("Couldn't serialize response for content type application/json", e);
			return new ResponseEntity<Email>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<Email>(HttpStatus.ACCEPTED);
	}

}
