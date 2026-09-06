export $(grep -v '^#' .env | xargs)
gradle build -Dspring.profiles.active=prod
