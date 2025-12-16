FROM tomcat:9.0-jdk11

# Remove existing webapps
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy the war file
COPY target/ROOT.war /usr/local/tomcat/webapps/ROOT.war

# Expose default Tomcat port
EXPOSE 8080

CMD ["catalina.sh", "run"]
