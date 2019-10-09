# start.sh
/usr/lib/jvm/java-8-openjdk-amd64/bin/java -jar target/sdnc_reports_api-0.0.1-SNAPSHOT.jar &

/usr/lib/jvm/java-8-openjdk-amd64/bin/java -jar target/sdnc_reports_certification-1.0.1-SNAPSHOT.jar &

/usr/lib/jvm/java-8-openjdk-amd64/bin/java -jar target/sdnc_reports_service-0.0.1-SNAPSHOT.jar