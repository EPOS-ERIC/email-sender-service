package io.swagger.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class SwaggerDocumentationConfig {

	@Bean
	public OpenAPI openApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Email Sender Service RESTful APIs")
						.description("This is the Email Sender Service RESTful APIs Swagger page.")
						.version("0.0.1")
						.license(new License()
								.name("GPLv3")
								.url("https://www.gnu.org/licenses/gpl-3.0.html#license-text"))
						.contact(new Contact()
								.email("apiteam@swagger.io")));
	}
}
