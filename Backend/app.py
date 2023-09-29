from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
from geoalchemy2 import Geometry
from sqlalchemy import create_engine
from sqlalchemy.sql import text
import firebase_admin
from firebase_admin import credentials
from firebase_admin import db

app = Flask(__name__)

# Configuro la connessione con il database
app.config['SQLALCHEMY_DATABASE_URI'] = 'postgresql://postgres:password@localhost/geofance-emergency'
db = SQLAlchemy(app)

i = "user0"
user_used = None


# Inizializzo l'app Firebase nel tuo backend
cred = credentials.Certificate("C:\\Users\\racit\\Desktop\\chiave firebase\\geo-fencing-based-emergency-firebase-adminsdk-1yviz-e39c6f8807.json")
firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://geo-fencing-based-emergency-default-rtdb.europe-west1.firebasedatabase.app/'
})

notifiche_ref = firebase_admin.db.reference('/notifiche')


@app.route('/')
def hello_world():
    return '<h1>GEOFECE EMERGENCY</h1>'

#DA RIFARE
@app.route('/register', methods=['POST'])
def register():
    try:
        data = request.get_json()
        username = data.get('username')
        password = data.get('password')

        # Verifica che l'utente con lo stesso username non esista già
        existing_user = UserManagement.find_user_by_username(username)
        if existing_user:
            return jsonify({"error": "L'utente con questo username esiste già."}), 400

        # Crea un nuovo utente con la password in chiaro
        new_user = UserManagement(username=username, password=password)
        new_user.create_user()

        return jsonify({"message": "Registrazione completata con successo."}), 200

    except Exception as e:
        db.session.rollback()  # Annulla la transazione in caso di errore generico
        return jsonify({"error": str(e)}), 500




#DA RIFARE
@app.route('/login', methods=['POST'])
def login():
    try:
        data = request.get_json()
        username = data.get('username')
        password = data.get('password')

        # Trova l'utente nel database
        user = UserManagement.find_user_by_username(username)
        if not user:
            return jsonify({"error": "Utente non trovato."}), 404

        # Verifica la password in chiaro
        if user.password != password:
            return jsonify({"error": "Credenziali non valide."}), 401

        user_used = username
        return jsonify({"message": "Accesso effettuato con successo."}), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500



@app.route('/upload_location', methods=['POST'])
def upload_location():
    try:
        # Ottiengo i dati di posizione dalla richiesta POST
        data = request.get_json()
        
        # Estrai le coordinate latitudine e longitudine dai dati
        username = data.get('username')
        latitude = data.get('latitude')
        longitude = data.get('longitude')
        activity = data.get('recognizedActivity')

        #TODO: ottenere username con registrazione (da android in realtà)
    
        # Creo un oggetto Point con le coordinate
        coordinates = f'POINT({longitude} {latitude})'
        
        # Creo la query SQL per inserire o aggiornare il record
        query = text("""
            INSERT INTO "emergency-schema"."user-information" (username, posizione, activity)
            VALUES (:username, :posizione, :activity)
            ON CONFLICT (username) DO UPDATE
            SET posizione = EXCLUDED.posizione, activity = EXCLUDED.activity
        """)

        # Parametri per la query
        parametri = {
            'username': username,
            'posizione': coordinates,
            'activity': activity
        }
      
        try:
            db.session.execute(query, parametri)  # Eseguo la query con i parametri
            db.session.commit()  # Eseguo il commit per confermare le modifiche nel database
            print("Record inserito o aggiornato con successo")
        except Exception as e:
            db.session.rollback()  # Annulla la transazione in caso di errore
            print(f"Errore durante l'inserimento o l'aggiornamento: {str(e)}")
        

        return jsonify({"message": "Dati di posizione ricevuti correttamente."}), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500



@app.route('/firebase_update')
def firebase_update():
    # lista di liste contenente coppie [latitudine, longitudine] del geofence dell'allarme
    coordinate_points = [[44.493760, 11.343032], [44.493760,11.343234], [44.493911, 11.343437], [44.494072, 11.343437], [44.494222, 11.343234], [44.494222, 11.343032]]
    nuova_notifica = {
    'testo': 'Emergenza! Terremoto in corso.',
    'coordinate': coordinate_points
    }

    # Scrivi la notifica nel database sotto il nodo "notifiche" (solo identificatore univoco della notifica, non ancora i dati)
    nuova_notifica_ref = notifiche_ref.push()

    #prendo l'identificatore univoco con la quale abbiamo contrassgnato la nuova notifica
    id_notifica = nuova_notifica_ref.key

    #
    #inserisco il geofence anche nel mio database postgres/postgis:
    #

    #creo una lista con le coordinate richieste nel database postgis (Array di POINT)
    coordinate_points_postgis = []
    for lat, lon in coordinate_points:
        newpoint = f'POINT({lon} {lat})'
        coordinate_points_postgis.append(newpoint)

    #creo la query    
    query = text("""
        INSERT INTO "emergency-schema"."geofence-information" (id, points)
        VALUES (:id, :points)
    """)

    # Parametri per la query
    parametri = {
        'id': id_notifica,
        'points': coordinate_points_postgis,
    }

    #TODO: controllare il risultato. E' in un formato diverso da quello aspettato
    try:
        db.session.execute(query, parametri)  # Eseguo la query con i parametri
        db.session.commit()  # Eseguo il commit per confermare le modifiche nel database
        print("Record inserito con successo")
    except Exception as e:
        db.session.rollback()  # Annulla la transazione in caso di errore
        print(f"Errore durante l'inserimento: {str(e)}")
    

    #aggiungo alla notifica inserita su firebase precedentemente i dati specificati (testo e coordinate)
    nuova_notifica_ref.set(nuova_notifica)
    
    return "<h1>Insert successfull</h1>"



if __name__ == '__main__':
    #app.run(host='0.0.0.0', port=8080, debug= False)
    app.run(debug=True)
