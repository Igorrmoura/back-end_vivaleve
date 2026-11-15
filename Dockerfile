# ---------- Etapa 1: compila o projeto e gera o .jar ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia só o pom primeiro: as dependências ficam em cache enquanto o pom não mudar.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Os testes rodam no CI (GitHub Actions); aqui só empacotamos.
RUN mvn -B -q package -DskipTests

# ---------- Etapa 2: imagem final, só com o Java e o .jar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Roda a API com um usuário sem privilégios (não como root).
RUN useradd --system --no-create-home pluma
USER pluma

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
