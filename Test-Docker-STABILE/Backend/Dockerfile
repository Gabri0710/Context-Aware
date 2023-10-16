# Usa un'immagine di base di Python
FROM python:3.10

# Imposta il working directory all'interno del container
WORKDIR /app

# Copia il codice del backend nell'immagine del container
COPY . /app/

# Installa le dipendenze
RUN pip install --no-cache-dir -r requirements.txt

# Esponi la porta su cui Flask ascolterà le richieste
EXPOSE 5001

# Comando per avviare l'app Flask
CMD ["python", "app.py"]
