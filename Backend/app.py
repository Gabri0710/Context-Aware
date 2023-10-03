from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
from geoalchemy2 import Geometry
from sqlalchemy import create_engine
from sqlalchemy.sql import text
import firebase_admin
from firebase_admin import credentials
from firebase_admin import db
from flask_cors import CORS

app = Flask(__name__)

#UTILE PER TESTARE SULLO STESSO DOMINIO MA PORTE DIVERSE, CORS policy
CORS(app)


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
        
        
        
        user_state_ref = firebase_admin.db.reference('/user/' + username + "/information")

        
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



@app.route('/add_geofence', methods=['POST'])
def add_geofence():
    # lista di liste contenente coppie [latitudine, longitudine] del geofence dell'allarme
    #coordinate_points = [[44.493760, 11.343032], [44.493760,11.343234], [44.493911, 11.343437], [44.494072, 11.343437], [44.494222, 11.343234], [44.494222, 11.343032]]
    testo_allarme = request.form.get('testo_allarme', 'ALLARME')
    coordinate = request.form.getlist('coordinates[]')
    coordinate_points = []
    
    for coord in coordinate:
        lon_string, lat_string = coord.split(',')
        lat = float(lat_string)
        lon = float(lon_string)
        tmp = []
        tmp.append(lat)
        tmp.append(lon)
        coordinate_points.append(tmp)
    
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
    'testo': testo_allarme,
    'coordinate': coordinate_points,
    }

    #aggiungo alla notifica inserita su firebase precedentemente i dati specificati (testo e coordinate)
    nuova_notifica_ref.set(nuova_notifica)
    
    return jsonify({'ok': 'record inserito'}), 200 


@app.route('/delete_geofence', methods=['POST'])
def delete_geofence():
    id_geofence = request.form.get('id_allarme')
    

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
    
    return jsonify({'ok': 'record eliminato'}), 200 



@app.route('/get_walking_user_data', methods=['GET'])
def get_waling_user_data():
    try:
        # Eseguo una query per recuperare le informazioni sulle posizioni degli utenti dal database
        query = text("""
            SELECT ST_X(posizione) as lon, ST_Y(posizione) as lat
            FROM "emergency-schema"."user-information"
            WHERE activity = 'WALKING';
        """)

        # Eseguo la query e ottieni i risultati
        result = db.session.execute(query)

        # Costruisco un elenco di dizionari rappresentanti i dati GeoJSON delle posizioni degli utenti
        user_data = []
        for row in result.fetchall():
            lon, lat = row[0], row[1]
            user_geojson = {
                "type": "Feature",
                "geometry": {
                    "type": "Point",
                    "coordinates": [lon, lat]
                }
            }
            user_data.append(user_geojson)

        # Restituisci i dati delle posizioni degli utenti come GeoJSON al frontend
        return jsonify(user_data), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route('/get_car_user_data', methods=['GET'])
def get_car_user_data():
    try:
        # Eseguo una query per recuperare le informazioni sulle posizioni degli utenti dal database
        query = text("""
            SELECT ST_X(posizione) as lon, ST_Y(posizione) as lat
            FROM "emergency-schema"."user-information"
            WHERE activity = 'CAR';
        """)

        # Eseguo la query e ottieni i risultati
        result = db.session.execute(query)

        # Costruisco un elenco di dizionari rappresentanti i dati GeoJSON delle posizioni degli utenti
        user_data = []
        for row in result.fetchall():
            lon, lat = row[0], row[1]
            user_geojson = {
                "type": "Feature",
                "geometry": {
                    "type": "Point",
                    "coordinates": [lon, lat]
                }
            }
            user_data.append(user_geojson)

        # Restituisci i dati delle posizioni degli utenti come GeoJSON al frontend
        return jsonify(user_data), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500



import binascii
from shapely import wkb
@app.route('/get_geofence', methods=['GET'])
def get_geofence():
    try:
        # Eseguo una query per recuperare le informazioni sulle posizioni degli utenti dal database
        query = text("""
           SELECT g.id, g.polygon, COUNT(u.username) as n_users_inside
            FROM "emergency-schema"."geofence-information" as g
            LEFT JOIN "emergency-schema"."user-information" as u
            ON ST_Contains(g.polygon, u.posizione)
            GROUP BY g.id;
        """)

        # Eseguo la query e ottieni i risultati
        result = db.session.execute(query)

        query_result = result.fetchall()

        id_polygons = [row[0] for row in query_result]
        polygons = [row[1] for row in query_result]
        n_users = [row[2] for row in query_result]


        polygons_frontend_format = []

        for p in polygons:
            # Decodifica l'HEXEWKB nella rappresentazione binaria WKB
            binary_polygon = binascii.unhexlify(p)

            # Crea un oggetto poligono da WKB usando shapely
            polygon = wkb.loads(binary_polygon)

            # Estrai le coordinate del poligono
            coordinates = polygon.exterior.coords.xy

            tmp = []
            # Prendo le coordinate (lon, lat)
            for lon, lat in zip(coordinates[0], coordinates[1]):
                tmp.append([lon, lat])

            polygons_frontend_format.append(tmp)


        points_geojson = []
        i = 0
        for record in polygons_frontend_format:
            tmp = []
            for lon, lat in record:
                point_geojson = {
                        "type": "Feature",
                        "geometry": {
                            "type": "Point",
                            "coordinates": [lon, lat]
                        }
                    }

                tmp.append(point_geojson)
                tmp_geojson_with_id = {
                        "id": id_polygons[i],
                        "n_users" : n_users[i],
                        "points": tmp
                    }
                
            i+=1
            points_geojson.append(tmp_geojson_with_id)
        

        # Restituisci i dati delle posizioni degli utenti come GeoJSON al frontend
        return jsonify(points_geojson), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500



    


@app.route('/testforfrontend', methods=['POST'])
def testforfrontend():
    data_from_frontend = request.form.get('data')  # Ottieni i dati dal campo 'data' del modulo inviato (campo specificato sotto "name")
    
    print(data_from_frontend)
    response_data = {'message': 'Messaggio ricevuvto: ' + data_from_frontend}
    return jsonify(response_data), 200  


if __name__ == '__main__':
    #app.run(host='0.0.0.0', port=8080, debug= False)
    app.run(debug=True)