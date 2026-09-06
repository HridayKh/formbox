export $(grep -v '^#' .env | xargs)
java -Dspring.aot.enabled=true -jar build/libs/formbox-*.jar --spring.profiles.active=prod
