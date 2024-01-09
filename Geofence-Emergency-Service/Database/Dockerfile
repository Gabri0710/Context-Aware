# Usa un'immagine di PostgreSQL con PostGIS preinstallato
FROM postgis/postgis:latest

# Copia il tuo file SQL nel container
COPY ./geofence-emergency.sql /docker-entrypoint-initdb.d/

# Imposta variabili d'ambiente per PostgreSQL
ENV POSTGRES_DB geofence-emergency
ENV POSTGRES_USER postgres
ENV POSTGRES_PASSWORD password

# Esponi la porta di PostgreSQL (di default è 5432)
#EXPOSE 5432
