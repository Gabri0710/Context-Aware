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
        
        
        
        user_state_ref = firebase_admin.db.reference('/user/' + username)

        print(user_state_ref)
        #creo la query per calcolare se l'utente si trova dentro il geofence
        query = text("""
            SELECT ui.username, gi.id as geofence_id
            FROM "emergency-schema"."user-information" ui
            JOIN "emergency-schema"."geofence-information" gi 
            ON ST_Within(ui.posizione::geometry, gi.polygon::geometry)
            WHERE ui.username = :username;

        """)

        # Parametri per la query
        parametri = {
            'username': username,
        }
        
        users_in_geofence = []

        try:
            result = db.session.execute(query, parametri)  # Esegui la query con i parametri
            # Estrai la colonna "username" dai risultati e crea una lista
            row = result.fetchone()
        except Exception as e:
            db.session.rollback()  # Annulla la transazione in caso di errore
            print(f"Errore durante l'esecuzione della query: {str(e)}")

        
        

        
        if row is not None:
            new_value = {
                'stato': 'DENTRO IL GEOFENCE',
                'id_geofence': row[1]
            }
            user_state_ref.set(new_value)
            return "<h1>DENTRO IL GEOFENCE</h1>"

        
        
        #creo la query per calcolare se l'utente si trova a 1km di distanza dal geofence
        query = text("""
            SELECT ui.username, gi.id AS geofence_id
            FROM "emergency-schema"."user-information" ui
            JOIN "emergency-schema"."geofence-information" gi 
            ON ST_Distance(ui.posizione::geography, gi.polygon::geography) <= 1000 
            AND NOT ST_Within(ui.posizione::geometry, gi.polygon::geometry)
            WHERE ui.username = :username;


        """)

        # Parametri per la query
        parametri = {
            'username': username,
        }
        
        users_in_1km = []
        try:
            result = db.session.execute(query, parametri)  # Esegui la query con i parametri
            # Estrai la colonna "username" dai risultati e crea una lista
            row = result.fetchone()
        except Exception as e:
            db.session.rollback()  # Annulla la transazione in caso di errore
            print(f"Errore durante l'esecuzione della query: {str(e)}")

        
        

        if row is not None:
            new_value = {
                'stato': 'A 1 KM DAL GEOFENCE',
                'id_geofence': row[1]
            }
            user_state_ref.set(new_value)
            return "<h1>A 1 km dal geofence</h1>"


        
        #creo la query per calcolare se l'utente si trova tra 1 e 2 km di distanza dal geofence
        query = text("""
            SELECT ui.username, gi.id AS geofence_id
            FROM "emergency-schema"."user-information" ui
            JOIN "emergency-schema"."geofence-information" gi 
            ON ST_Distance(ui.posizione::geography, gi.polygon::geography) > 1000  -- Maggiore di 1 km
            AND ST_Distance(ui.posizione::geography, gi.polygon::geography) <= 2000 -- Inferiore o uguale a 2 km
            AND NOT ST_Within(ui.posizione::geometry, gi.polygon::geometry)
            WHERE ui.username = :username;

        """)

        # Parametri per la query
        parametri = {
            'username': username,
        }
        
        users_between_1_2km = []
        try:
            result = db.session.execute(query, parametri)  # Esegui la query con i parametri
            # Estrai la colonna "username" dai risultati e crea una lista
            row = result.fetchone()
        except Exception as e:
            db.session.rollback()  # Annulla la transazione in caso di errore
            print(f"Errore durante l'esecuzione della query: {str(e)}")

        

        if row is not None:
            new_value = {
                'stato': '1-2 KM DAL GEOFENCE',
                'id_geofence': row[1]
            }
            user_state_ref.set(new_value)
            return "<h1>1-2 KM DAL GEOFENCE</h1>"

        new_value = {
            'stato': 'OK',
            'id_geofence': ""
        }
        user_state_ref.set(new_value)
        return "<h1>OK</h1>"
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500



@app.route('/add_geofence')
def add_geofence():
    # lista di liste contenente coppie [latitudine, longitudine] del geofence dell'allarme
    coordinate_points = [[44.493760, 11.343032], [44.493760,11.343234], [44.493911, 11.343437], [44.494072, 11.343437], [44.494222, 11.343234], [44.494222, 11.343032]]
    

    # Scrivi la notifica nel database sotto il nodo "notifiche" (solo identificatore univoco della notifica, non ancora i dati)
    nuova_notifica_ref = notifiche_ref.push()

    #prendo l'identificatore univoco con la quale abbiamo contrassgnato la nuova notifica
    id_notifica = nuova_notifica_ref.key

    #
    #inserisco il geofence anche nel mio database postgres/postgis:
    #

    #creo una lista con le coordinate richieste nel database postgis (Array di POINT)
    polygon_string = "POLYGON(("
    for lat, lon in coordinate_points:
        polygon_string += f"{lon} {lat}, "
    # Chiudo il poligono aggiungendo il primo punto alla fine
    lat, lon = coordinate_points[0]  # Primo punto
    polygon_string += f"{lon} {lat}))"

    #creo la query per inserire i valori nel database postgis
    query = text("""
        INSERT INTO "emergency-schema"."geofence-information" (id, polygon)
        VALUES (:id, :polygon)
    """)

    # Parametri per la query
    parametri = {
        'id': id_notifica,
        'polygon': polygon_string,
    }

    
    try:
        db.session.execute(query, parametri)  # Eseguo la query con i parametri
        db.session.commit()  # Eseguo il commit per confermare le modifiche nel database
        print("Record inserito con successo")
    except Exception as e:
        db.session.rollback()  # Annulla la transazione in caso di errore
        print(f"Errore durante l'inserimento: {str(e)}")
    
    

    nuova_notifica = {
    'testo': 'Emergenza! Terremoto in corso.',
    'coordinate': coordinate_points,
    }

    #aggiungo alla notifica inserita su firebase precedentemente i dati specificati (testo e coordinate)
    nuova_notifica_ref.set(nuova_notifica)
    
    return "<h1>Insert successfull</h1>"


@app.route('/delete_geofence', methods=['POST'])
def delete_geofence():
    id_geofence = request.form['id_geofence']

    id_ref = notifiche_ref.child(id_geofence)

    # Elimina il dato dal database
    id_ref.delete()

    query = text("""
        DELETE FROM "emergency-schema"."geofence-information"
        WHERE id = :id_geofence
    """)

    # Parametri per la query
    parametri = {
        'id_geofence': id_geofence
    }

    
    try:
        db.session.execute(query, parametri)  # Eseguo la query con i parametri
        db.session.commit()  # Eseguo il commit per confermare le modifiche nel database
        print("Record eliminato con successo")
    except Exception as e:
        db.session.rollback()  # Annulla la transazione in caso di errore
        print(f"Errore durante l'eliminazione: {str(e)}")
    
    return "<h1> Eliminato </h1>"



@app.route('/get_user_data', methods=['GET'])
def get_user_data():
    try:
        # Esegui una query per recuperare le informazioni sulle posizioni degli utenti dal database
        query = text("""
            SELECT *
            FROM "emergency-schema"."user-information";
        """)

        # Esegui la query e ottieni i risultati
        result = db.session.execute(query)

        # Costruisci un elenco di dizionari rappresentanti i dati GeoJSON degli utenti
        user_data = []
        for row in result.fetchall():
            username, posizione, activity = row
            user_geojson = {
                "type": "Feature",
                "properties": {
                    "username": username,
                    "activity": activity
                },
                "posizione": posizione
            }
            user_data.append(user_geojson)

        # Restituisci i dati degli utenti come GeoJSON al frontend
        return jsonify(user_data), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500



if __name__ == '__main__':
    #app.run(host='0.0.0.0', port=8080, debug= False)
    app.run(debug=True)