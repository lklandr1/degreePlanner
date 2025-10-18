#!/bin/bash
# =====================================
# Run Spring Boot application
# For all developers on the team
# =====================================

# Exit immediately if any command fails
set -e

echo "Cleaning and compiling the project..."
mvn clean compile

echo "Starting Spring Boot application..."
mvn spring-boot:run
