package io.swagger.api;

import io.swagger.model.Email;
import io.swagger.model.ProviderType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.mail.MessagingException;

import org.apache.commons.lang3.StringUtils;
import org.epos.core.ContactPointGet;
import org.epos.core.EmailSenderHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2023-08-04T13:31:01.781679391Z[GMT]")
@RestController
public class SenderApiController implements SenderApi{

	@org.springframework.beans.factory.annotation.Autowired
	public SenderApiController(ObjectMapper objectMapper, HttpServletRequest request) {
		this.objectMapper = objectMapper;
		this.request = request;
	}

	private static final Logger log = LoggerFactory.getLogger(SenderApiController.class);

	private final ObjectMapper objectMapper;

	private final HttpServletRequest request;


	public ResponseEntity<Email> sendEmailPost(
			@NotNull @Parameter(in = ParameterIn.QUERY, description = "Id of the resource" , required=true,schema=@Schema()) @Valid @RequestParam(value = "id", required = true) String id,
			@NotNull @Parameter(in = ParameterIn.QUERY, description = "Contact point type" , required=true,schema=@Schema()) @Valid @RequestParam(value = "contactType", required = true) ProviderType contactType, 
			@Parameter(in = ParameterIn.DEFAULT, description = "", schema=@Schema()) @Valid @RequestBody Email body) {
		String accept = request.getHeader("Accept");
		String userEmail = request.getParameter("userEmail");
		String firstName = request.getParameter("firstName");
		String lastName = request.getParameter("lastName");
		if (accept != null && accept.contains("application/json")) {
			try {
				final Map<String, Object> requestParameters = new HashMap<String, Object>();
				if(StringUtils.isBlank(id) && StringUtils.isBlank(userEmail)) {
					return new ResponseEntity<Email>(HttpStatus.BAD_REQUEST);
				}

				requestParameters.put("id", id);
				requestParameters.put("type", contactType);
				requestParameters.put("email", userEmail);
				requestParameters.put("firstName", firstName);
				requestParameters.put("lastName", lastName);
				log.info(requestParameters.toString());

				redirectRequest(requestParameters, body);

				return new ResponseEntity<Email>(objectMapper.readValue("{\n  \"bodyText\" : \"bodyText\",\n  \"subject\" : \"subject\"\n}", Email.class), HttpStatus.ACCEPTED);
			} catch (IOException e) {
				log.error("Couldn't serialize response for content type application/json", e);
				return new ResponseEntity<Email>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<Email>(HttpStatus.NOT_IMPLEMENTED);
	}

	private ResponseEntity<Email> redirectRequest(Map<String, Object> requestParams,Email sendEmail) {

		JsonObject response = ContactPointGet.generate(new JsonObject(), requestParams);
		
			try {
				EmailSenderHandler.handle(response,sendEmail, requestParams);
			} catch (UnsupportedEncodingException | MessagingException e) {
				log.error("Couldn't serialize response for content type application/json", e);
				return new ResponseEntity<Email>(HttpStatus.INTERNAL_SERVER_ERROR);
			}

		return new ResponseEntity<Email>(HttpStatus.ACCEPTED);
	}

}
