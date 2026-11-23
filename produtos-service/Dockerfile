
# Imagem base com Tomcat 10.1 e Java 21
FROM tomcat:10.1-jdk21-temurin

# Remove aplicações padrão do Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

# Copia o WAR gerado pelo Maven
COPY target/produtos-service.war /usr/local/tomcat/webapps/ROOT.war

# Porta utilizada pelo Tomcat
EXPOSE 8080

# Inicia o servidor
CMD ["catalina.sh", "run"]
