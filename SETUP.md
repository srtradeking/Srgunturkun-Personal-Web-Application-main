# Educational Web Application Setup Guide

⚠️ **EDUCATIONAL PURPOSES ONLY** ⚠️
This project is intended solely for educational and learning purposes. It should NOT be used in production environments or for commercial applications.

## Prerequisites

- Node.js (v14 or higher)
- Java (JDK 11 or higher)
- Maven

## Backend Setup

1. Navigate to the backend directory:
   ```bash
   cd backend-java
   ```

2. Build the project:
   ```bash
   mvn clean install
   ```

3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

The backend will start on `http://localhost:8080`

## Frontend Setup

1. Install dependencies:
   ```bash
   npm install
   ```

2. Start the development server:
   ```bash
   npm run serve
   ```

The frontend will start on `http://localhost:3000`

## Configuration

### Database
- This application uses H2 in-memory database for educational purposes
- Database console is available at: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (leave empty)

### File Storage
- Files are stored locally in the `./uploads` directory
- This is configured for educational purposes to avoid external dependencies

## Features for Learning

This application demonstrates:
- Vue.js frontend with modern JavaScript
- Spring Boot backend with Java
- JWT authentication and authorization
- File upload and storage
- RESTful API design
- Security best practices (CSRF protection, input validation)
- Database operations with JPA/Hibernate

## Important Notes

- **Security**: Some security implementations are simplified for educational purposes
- **Scalability**: Not optimized for production workloads
- **Dependencies**: Uses local storage and in-memory database for simplicity
- **Error Handling**: Basic error handling for demonstration

## License

This project is provided for educational purposes. Please use responsibly and do not deploy to production without proper security review and hardening.
