# EPOS Email Sender Service

The **Email Sender Service** is a microservice within the European Plate Observing System (EPOS) Integrated Core Services Central (ICS-C) infrastructure. Its primary function is to handle the dispatching of emails triggered by various events or actions within the EPOS ecosystem, such as user notifications, system alerts, and workflow confirmations.

---

## Table of Contents

* [Overview](#overview)
* [Features](#features)
* [Architecture](#architecture)
* [Installation](#installation)
* [Configuration](#configuration)
* [Usage](#usage)
* [API Documentation](#api-documentation)
* [Development](#development)
* [Contributing](#contributing)
* [License](#license)

---

## Overview

The Email Sender Service is designed to:

* Provide a centralized mechanism for sending emails across the EPOS infrastructure.
* Ensure reliable and asynchronous email dispatching using message queues.
* Support templated emails for consistent communication.
* Integrate seamlessly with other EPOS services requiring email functionalities.([Epos EU][1])

---

## Features

* **Asynchronous Email Dispatching**: Utilizes message queues to handle email sending operations without blocking the main application flow.
* **Template Support**: Allows the use of predefined templates for various email types, ensuring consistency and ease of maintenance.
* **RESTful API**: Exposes endpoints for triggering email sending operations.
* **Integration Ready**: Designed to be easily integrated with other services within the EPOS infrastructure.

---

## Architecture

The Email Sender Service is built using Java and Spring Boot, following a microservices architecture. It leverages message queues (e.g., RabbitMQ) for handling email dispatching asynchronously.

**Components:**

* **API Layer**: Handles incoming HTTP requests and routes them to the appropriate service components.
* **Service Layer**: Contains the business logic for processing email requests and interactions.
* **Messaging Layer**: Manages the communication with the message queue for asynchronous processing.
* **Template Engine**: Processes email templates with dynamic content.

---

## Installation

**Prerequisites:**

* Java 11 or higher
* Maven 3.6 or higher
* Message Queue (e.g., RabbitMQ)

**Steps:**

1. **Clone the Repository:**

   ```bash
   git clone https://github.com/epos-eu/email-sender-service.git
   cd email-sender-service
   ```



2. **Build the Application:**

   ```bash
   mvn clean install
   ```



3. **Configure Environment Variables:**

   Set the necessary environment variables, such as `SPRING_PROFILES_ACTIVE`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, and `MAIL_PASSWORD`.

4. **Run the Application:**

   ```bash
   java -jar target/email-sender-service.jar
   ```



---

## Configuration

The application can be configured using environment variables or application properties. Key configurations include:

* **Mail Server Settings**: Configure the SMTP server details, including host, port, username, and password.
* **Template Directory**: Specify the directory containing email templates.
* **Queue Settings**: Configure the message queue connection details.

*Note*: Ensure that the message queue and mail server are properly set up and accessible by the application.

---

## Usage

The Email Sender Service exposes RESTful endpoints for sending emails. Typical usage involves:

1. **Triggering an Email**: A client service sends a POST request to the `/send-email` endpoint with the necessary details, such as recipient, subject, template name, and dynamic content.

2. **Processing the Request**: The service processes the request, renders the email template with the provided content, and places the email message onto the message queue.

3. **Dispatching the Email**: A listener consumes messages from the queue and sends the emails using the configured SMTP server.

*Note*: Detailed API specifications can be found in the Swagger documentation if available or by inspecting the controller classes within the source code.

---

## API Documentation

For detailed API specifications, refer to the Swagger documentation if available or inspect the controller classes within the source code.

---

## Development

**Project Structure:**

* **`src/main/java`**: Contains the main application code, including controllers, services, and models.
* **`src/test/java`**: Includes unit and integration tests.
* **`resources`**: Holds application properties, configuration files, and email templates.

**Building the Project:**

Use Maven to build the project:

```bash
mvn clean install
```



**Running Tests:**

Execute the test suite using Maven:

```bash
mvn test
```



---

## Contributing

Contributions are welcome! To contribute:

1. **Fork the Repository**: Create your own fork of the project.
2. **Create a Branch**: Develop your feature or fix in a new branch.
3. **Commit Changes**: Ensure your commits are well-documented.
4. **Push to Fork**: Push your changes to your forked repository.
5. **Submit a Pull Request**: Open a pull request detailing your changes.

*Note*: Please adhere to the project's coding standards and include relevant tests for your contributions.

---

## License

This project is licensed under the GNU General Public License v3.0. See the [LICENSE](LICENSE) file for details.

---

For more information on the EPOS infrastructure and related services, visit the [EPOS Open Source](https://epos-eu.github.io/epos-open-source/) page.

---

[1]: https://www.epos-eu.org/integrated-core-services "Integrated Core Services | EPOS"
