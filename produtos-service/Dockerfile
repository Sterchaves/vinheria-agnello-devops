FROM tomcat:10.1-jdk21-temurin

# Remove as aplicações padrão
RUN rm -rf /usr/local/tomcat/webapps/*

# Instala a configuração personalizada do Tomcat
COPY config/server.xml /usr/local/tomcat/conf/server.xml

# Habilita a leitura de variáveis de ambiente no server.xml
RUN echo "org.apache.tomcat.util.digester.PROPERTY_SOURCE=org.apache.tomcat.util.digester.EnvironmentPropertySource" \
    >> /usr/local/tomcat/conf/catalina.properties

# Copia a aplicação compilada
COPY target/produtos-service.war /usr/local/tomcat/webapps/ROOT.war

# Portas HTTP e HTTPS
EXPOSE 8443

CMD ["catalina.sh", "run"]
