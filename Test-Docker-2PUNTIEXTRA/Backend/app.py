from flask import Flask, request, jsonify
from flask_cors import CORS
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy import create_engine
from sqlalchemy.sql import text
from geoalchemy2 import Geometry
import firebase_admin
from firebase_admin import credentials
from firebase_admin import db
import csv
import json
import numpy as np
import matplotlib
matplotlib.use('Agg')              #mi serve per specificare che il backend sia non interattivo (se no yellowbrick crea problemi avviando GUI)
import matplotlib.pyplot as plt
from yellowbrick.cluster import KElbowVisualizer
import pandas as pd
from sklearn.cluster import KMeans
from io import StringIO
import binascii
from shapely import wkb

app = Flask(__name__)

#UTILE PER TESTARE SULLO STESSO DOMINIO MA PORTE DIVERSE, CORS policy
CORS(app)


# Configuro la connessione con il database postgis
#app.config['SQLALCHEMY_DATABASE_URI'] = 'postgresql://postgres:password@database-service:5433/geofence-emergency'          #da usare su kubernetes
#app.config['SQLALCHEMY_DATABASE_URI'] = 'postgresql://postgres:password@database-container/geofence-emergency'             #da usare se si usa solo docker
app.config['SQLALCHEMY_DATABASE_URI'] = 'postgresql://postgres:password@localhost:5431/geofence-emergency'                  #da usare se si testa in locale
db = SQLAlchemy(app)


# Inizializzo Firebase
cred = credentials.Certificate("./key.json")
firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://geo-fencing-based-emergency-default-rtdb.europe-west1.firebasedatabase.app/'
})

# prendo il riferimento al nodo notifiche (allarmi)
notifiche_ref = firebase_admin.db.reference('/notifiche')


#homepage, per test 
@app.route('/')
def hello_world():
    try:
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
        return "<h1>ok</h1>"
    except Exception as e:
        return "<h1>error2</h1>"


# aggiorna la posizione dell'utente
@app.route('/upload_location', methods=['POST'])
def upload_location():
    try:
        # Ottengo i dati di posizione dalla richiesta POST
        data = request.get_json()
        
        # Estraggo i valori a cui sono interessato
        username = data.get('username')
        latitude = data.get('latitude')
        longitude = data.get('longitude')
        activity = data.get('recognizedActivity')
    
        # Creo un oggetto Point con le coordinate ottenute
        coordinates = f'POINT({longitude} {latitude})'
        
        # Creo la query SQL per inserire o aggiornare il record nel mio db postgis
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
            db.session.rollback()  # Annullo in caso di errore
            print(f"Errore durante l'inserimento o l'aggiornamento: {str(e)}")
        
        
        #prendo il riferimento di firebase del nodo dell'utente coinvolto
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
        

        try:
            result = db.session.execute(query, parametri)  # Eseguo la query con i parametri
            # Estraggo il primo risultato della query
            row = result.fetchone()
        except Exception as e:
            db.session.rollback()  # Annulla la transazione in caso di errore
            print(f"Errore durante l'esecuzione della query: {str(e)}")

        
        

        #se la query ha restituito qualcosa
        if row is not None:
            new_value = {
                'stato': 'DENTRO IL GEOFENCE',              #aggiorno su firebase la condizione dell'utente
                'id_geofence': row[1]                       #inserisco su firebase l'id del geofence alla quale appartiene
            }
            user_state_ref.set(new_value)                   #inserisco i valori su firebase
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
            'username': username
        }
        
        
        try:
            result = db.session.execute(query, parametri)  # Eseguo la query con i parametri
            # Estraggo il primo risultato della query
            row = result.fetchone()
        except Exception as e:
            db.session.rollback()  # Annullo in caso di errore
            print(f"Errore durante l'esecuzione della query: {str(e)}")

        
        
        #se la query ha restituito qualcosa
        if row is not None:
            new_value = {
                'stato': 'A 1 KM DAL GEOFENCE',                 #aggiorno su firebase la condizione dell'utente
                'id_geofence': row[1]                           #inserisco su firebase l'id del geofence alla quale appartiene
            }
            user_state_ref.set(new_value)                       #inserisco i valori su firebase
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
            'username': username
        }
        
    
        try:
            result = db.session.execute(query, parametri)  # Eseguo la query con i parametri
            #Estraggo il primo risultato della query
            row = result.fetchone()
        except Exception as e:
            db.session.rollback()  # Annullo in caso di errore
            print(f"Errore durante l'esecuzione della query: {str(e)}")

        
        #se la query ha restituito qualcosa
        if row is not None:
            new_value = {
                'stato': '1-2 KM DAL GEOFENCE',                      #aggiorno su firebase la condizione dell'utente
                'id_geofence': row[1]                               #inserisco su firebase l'id del geofence alla quale appartiene
            }
            user_state_ref.set(new_value)                            #inserisco i valori su firebase
            return "<h1>1-2 KM DAL GEOFENCE</h1>"

        #se l'utente non è né dentro il geofence, né nel raggio di 1km, né tra 1-2km
        new_value = {
            'stato': 'OK',                                           #aggiorno su firebase la condizione dell'utente
            'id_geofence': ""                                       #inserisco su firebase l'id del geofence alla quale appartiene (nessuno)
        }
        user_state_ref.set(new_value)                               #inserisco i valori su firebase                      
        return "<h1>OK</h1>"
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# aggiunge un geofence
@app.route('/add_geofence', methods=['POST'])
def add_geofence():
    titolo = request.form.get('titolo_allarme', 'ALLARME GENERALE')                         #titolo geofence
    allarme1 = request.form.get('testo_allarme1', 'SEI NEL GEOFENCE')                       #allarme per coloro che sono dentro il geofence
    allarme2 = request.form.get('testo_allarme2', 'SEI A MENO DI 1KM DAL GEOFENCE')         #allarme per coloro che sono nel raggio di 1km
    allarme3 = request.form.get('testo_allarme3', 'SEI TRA 1 E 2 KM DAL GEOFENCE')          #allarme per coloro che sono tra 1 e 2 km
    coordinate = request.form.getlist('coordinates[]')                                      #lista di coordinate che definisce il geofence
    
    #definisco un array per le coordinate effettive
    coordinate_points = []
    
    #calcolo le coordinate effettive
    for coord in coordinate:
        lon_string, lat_string = coord.split(',')
        lat = float(lat_string)
        lon = float(lon_string)
        tmp = []
        tmp.append(lat)
        tmp.append(lon)
        coordinate_points.append(tmp)
    
    # Aggiungo il geofence su firebase sotto il nodo "notifiche"
    nuova_notifica_ref = notifiche_ref.push()

    #prendo l'identificatore univoco con la quale abbiamo contrassgnato la nuova notifica
    id_notifica = nuova_notifica_ref.key

    #
    #inserisco il geofence anche nel mio database postgres/postgis:
    #

    #creo un oggetto polygon da aggiungere su postgis
    polygon_string = "POLYGON(("
    for lat, lon in coordinate_points:
        polygon_string += f"{lon} {lat}, "
    # Chiudo il poligono aggiungendo il primo punto alla fine
    lat, lon = coordinate_points[0] 
    polygon_string += f"{lon} {lat}))"

    #creo la query per inserire i valori nel database postgis
    query = text("""
        INSERT INTO "emergency-schema"."geofence-information" (id, polygon)
        VALUES (:id, :polygon)
    """)

    # Parametri per la query
    parametri = {
        'id': id_notifica,
        'polygon': polygon_string
    }

    
    try:
        db.session.execute(query, parametri)  # Eseguo la query con i parametri
        db.session.commit()  # Confermo le modifiche nel database
        print("Record inserito con successo")
    except Exception as e:
        db.session.rollback()  # Annullo in caso di errore
        print(f"Errore durante l'inserimento: {str(e)}")
    
    
    #definisco i valori da inserire nella notifica creata su firebase
    nuova_notifica = {
    'titolo': titolo,
    'allarme1' : allarme1,
    'allarme2' : allarme2,
    'allarme3' : allarme3,
    'coordinate': coordinate_points
    }

    #aggiungo alla notifica inserita su firebase precedentemente i dati specificati
    nuova_notifica_ref.set(nuova_notifica)
    
    return jsonify({'ok': 'record inserito'}), 200 


#cancella un geofence
@app.route('/delete_geofence', methods=['POST'])
def delete_geofence():
    id_geofence = request.form.get('id_allarme')                #ottengo l'id del geofence da cancellare
    
    #prendo il riferimento da firebase del geofence da eliminare
    id_ref = notifiche_ref.child(id_geofence)

    # elimino il geofence da firebase
    id_ref.delete()

    #creo la query per eliminarlo su postgis
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
        db.session.commit()  # Confermo le modifiche nel database
        print("Record eliminato con successo")
    except Exception as e:
        db.session.rollback()  # Annullo in caso di errore
        print(f"Errore durante l'eliminazione: {str(e)}")
    
    return jsonify({'ok': 'record eliminato'}), 200 


# restituisce al frontend le posizioni degli utenti a piedi
@app.route('/get_walking_user_data', methods=['GET'])
def get_waling_user_data():
    try:
        # Eseguo una query per recuperare le informazioni sulle posizioni degli utenti dal database
        query = text("""
            SELECT ST_X(posizione) as lon, ST_Y(posizione) as lat
            FROM "emergency-schema"."user-information"
            WHERE activity = 'WALKING';
        """)

        # Eseguo la query e ottiengo i risultati
        result = db.session.execute(query)

        # Costruisco un elenco di dizionari rappresentanti i dati Geojson delle posizioni degli utenti
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

        # Restituisco i dati delle posizioni degli utenti come Geojson al frontend
        return jsonify(user_data), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500


# restituisce al frontend le posizioni degli utenti a in macchina
@app.route('/get_car_user_data', methods=['GET'])
def get_car_user_data():
    try:
        # Eseguo una query per recuperare le informazioni sulle posizioni degli utenti dal database
        query = text("""
            SELECT ST_X(posizione) as lon, ST_Y(posizione) as lat
            FROM "emergency-schema"."user-information"
            WHERE activity = 'CAR';
        """)

        # Eseguo la query e ottiengo i risultati
        result = db.session.execute(query)

        # Costruisco un elenco di dizionari rappresentanti i dati Geojson delle posizioni degli utenti
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

        # Restituisco i dati delle posizioni degli utenti come Geojson al frontend
        return jsonify(user_data), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500


# restituisce al frontend le posizioni di tutti gli utenti
@app.route('/get_all_user_data', methods=['GET'])
def get_all_user_data():
    try:
        # Eseguo una query per recuperare le informazioni sulle posizioni degli utenti dal database
        query = text("""
            SELECT ST_X(posizione) as lon, ST_Y(posizione) as lat
            FROM "emergency-schema"."user-information";
        """)

        # Eseguo la query e ottiengo i risultati
        result = db.session.execute(query)

        # Costruisco un elenco di dizionari rappresentanti i dati Geojson delle posizioni degli utenti
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

        # Restituisco i dati delle posizioni degli utenti come Geojson al frontend
        return jsonify(user_data), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500


# restituisce al frontend i geofence esistenti
@app.route('/get_geofence', methods=['GET'])
def get_geofence():
    try:
        # Eseguo una query per recuperare le informazioni sui geofence presenti nel db
        query = text("""
           SELECT g.id, g.polygon, COUNT(u.username) as n_users_inside
            FROM "emergency-schema"."geofence-information" as g
            LEFT JOIN "emergency-schema"."user-information" as u
            ON ST_Contains(g.polygon, u.posizione)
            GROUP BY g.id;
        """)

        # Eseguo la query e ottengo i risultati
        result = db.session.execute(query)

        # prendo i risultati
        query_result = result.fetchall()

        id_polygons = [row[0] for row in query_result]              #creo una lista con gli id dei poligoni
        polygons = [row[1] for row in query_result]                 #creo una lista con i poligoni
        n_users = [row[2] for row in query_result]                  #creo una lista con il numero degli utenti presenti nei vari poligoni

        #definisco una lista per adattare i dati ottenuti al formato richiesto dal frontend
        polygons_frontend_format = []

        for p in polygons:
            # Decodifico da esadecimale a binario
            binary_polygon = binascii.unhexlify(p)

            # Creo un oggetto poligono usando il mio polygono espresso in binario
            polygon = wkb.loads(binary_polygon)

            # Estraggo le coordinate del poligono
            coordinates = polygon.exterior.coords.xy

            tmp = []
            # Prendo le coordinate (lon, lat) e le inserisco in una lista tmp
            for lon, lat in zip(coordinates[0], coordinates[1]):
                tmp.append([lon, lat])

            # aggiungo la tupla contenuta in tmp nella mia lista di poligoni con formato adatto al frontend
            polygons_frontend_format.append(tmp)


        #creo una lista di punti che definiscono il poligono
        points_geojson = []

        i = 0
        for record in polygons_frontend_format:
            tmp = []
            for lon, lat in record:
                point_geojson = {
                        "type": "Feature",
                        "geometry": {
                            "type": "Point",        
                            "coordinates": [lon, lat]                   #prendo ogni punto del poligono
                        }
                    }

                #aggiungo il punto a tmp (mi servirà dopo)
                tmp.append(point_geojson)

                #prendo anche gli altri valori alla quale sono interessato
                tmp_geojson_with_id = {
                        "id": id_polygons[i],                           #id del poligono
                        "n_users" : n_users[i],                         #numero utenti all'interno del poligono
                        "points": tmp                                   #metto tmp, il punto ottenuto prima
                    }
                
            i+=1

            #aggiungo tutto alla mia lista di punti che definiscono il poligono
            points_geojson.append(tmp_geojson_with_id)
        

        # Restituisco i dati delle posizioni degli utenti come Geojson al frontend
        return jsonify(points_geojson), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500



#restituisce i cluster calcolati
@app.route('/get_cluster', methods=['POST'])
def get_cluster():
    #creo la query per prendere le posizioni degli utenti
    query = text("""
           SELECT ST_X(posizione) AS longitudine, ST_Y(posizione) AS latitudine 
            FROM "emergency-schema"."user-information";
        """)
   
    result = db.session.execute(query)

    query_result = result.fetchall()

    #definisco un csv_buffer (per non creare un csv localmente)
    csv_buffer = StringIO()

    #definisco un writer per scrivere sul mio csv_buffer
    writer = csv.writer(csv_buffer)
    
    # Scrivo le colonne del mio csv
    writer.writerow(['longitudine', 'latitudine'])
    
    # Scrivo le righe restituite dalla query nel csv
    for row in query_result:
        writer.writerow(row)

    #riporto il punto di lettura del buffer all'inizio
    csv_buffer.seek(0)

    # Carico il file CSV come DataFrame
    df = pd.read_csv(csv_buffer)

    # Seleziona le colonne di latitudine e longitudine per il clustering
    dati = df[['longitudine', 'latitudine']]

    # fisso il numero di cluster al valore ottenuto dal frontend
    num_cluster = int(request.form.get('num_cluster'))

    # Inizializzo il modello K-Means con il numero di cluster
    kmeans = KMeans(n_clusters=num_cluster, n_init=10)

    # Eseguo il clustering
    cluster_labels = kmeans.fit_predict(dati)

    # Aggiungo le etichette con il cluster corrispondente per ogni elemento e lo aggiungo al dataframe
    df['CLUSTER_ID'] = cluster_labels
    
    #restituisco il dataframe con le posizioni degli utenti + cluster_id
    json_data = df.to_json(orient='records')


    return jsonify(json_data), 200


#restituisce i cluster calcolati con il metodo elbow
@app.route('/get_cluster_elbow')
def get_cluster_elbow():
    #creo la query per prendere le posizioni degli utenti
    query = text("""
           SELECT ST_X(posizione) AS longitudine, ST_Y(posizione) AS latitudine 
            FROM "emergency-schema"."user-information";
        """)
   
    result = db.session.execute(query)

    query_result = result.fetchall()

    #definisco un csv_buffer (per non creare un csv localmente)
    csv_buffer = StringIO()

    #definisco un writer per scrivere sul mio csv_buffer
    writer = csv.writer(csv_buffer)
    
    # Scrivo le colonne del mio csv
    writer.writerow(['longitudine', 'latitudine'])
    
    # Scrivo le righe restituite dalla query nel csv
    for row in query_result:
        writer.writerow(row)

    #riporto il punto di lettura del buffer all'inizio
    csv_buffer.seek(0)

    # Carico il file CSV come DataFrame
    df = pd.read_csv(csv_buffer)

    # Seleziono le colonne di latitudine e longitudine per il clustering
    dati = df[['longitudine', 'latitudine']]
    
    #inizializzo un modello kmeans
    model = KMeans(n_init=10)

    #inizializzo un visualizer con KElbowVisualizer, a cui passo il mio modello e il range k che vogliamo esplorare
    visualizer = KElbowVisualizer(model, k=(1, 11), visualize=False)

    # Adatto il modello ai dati
    visualizer.fit(dati)  

    #ottengo il best_k ottenuto con il metodo elbow
    best_k = visualizer.elbow_value_

    #inizializzo un modello kmeans con il numero di cluster fissato al best_k
    kmeans = KMeans(n_clusters=best_k, n_init=10)

    # Eseguo il clustering
    cluster_labels = kmeans.fit_predict(dati)

    # Aggiungo i cluster_id trovati ai dati del dataframe originale
    df['CLUSTER_ID'] = cluster_labels
    
    plt.close()

    #restituisco al frontend il dataframe con le posizioni degli utenti e il cluster_id corrispondente
    json_data = df.to_json(orient='records')

    return jsonify(json_data), 200


#aggiunge la posizione del server, richiamato da un mio frontend per comodità nel fissaggio del server
@app.route('/add_server', methods=['POST'])
def add_server():
    try:
        #prendo l'id del server indicate nel frontend
        id_server = request.form.getlist('id_server')
        id_server = json.loads(id_server[0])

        #prendo le coordinate del punto selezionato nel frontend
        coordinate = request.form.getlist('coordinates')
        coordinate_points = []
        
        #casting a float 
        lon_string, lat_string = coordinate[0].split(',')
        latitude = float(lat_string)
        longitude = float(lon_string)

        #creo un oggetto POINT con le coordinate ottenute
        coordinates = f'POINT({longitude} {latitude})'
        
        # Creo la query SQL per inserire o aggiornare la posizione del server nel db postgis
        query = text("""
            INSERT INTO "emergency-schema"."edge-information" (id, posizione)
            VALUES (:id_server, :posizione)
            ON CONFLICT (id) DO UPDATE
            SET posizione = EXCLUDED.posizione
        """)

        # Parametri per la query
        parametri = {
            'id_server': id_server,
            'posizione': coordinates
        }
      
        try:
            db.session.execute(query, parametri)  # Eseguo la query con i parametri
            db.session.commit()  # Eseguo il commit
            print("SERVER inserito o aggiornato con successo")
            return jsonify({'ok': 'record inserito'}), 200
        except Exception as e:
            db.session.rollback()  # Annullo in caso di errore
            print(f"Errore durante l'inserimento o l'aggiornamento DEL SERVER: {str(e)}")
            return jsonify({'errore': e}), 500
    except:
        return jsonify({'errore': 'fuori'}), 500


#restituisce al frontend le posizioni del server
@app.route('/get_server')
def get_server():
    try:
        # creo una query per recuperare le informazioni della posizione dei server dal db
        query = text("""
            SELECT ST_X(posizione) as lon, ST_Y(posizione) as lat
            FROM "emergency-schema"."edge-information";
        """)

        # Eseguo la query e ottengo i risultati
        result = db.session.execute(query)

        # Costruisco un elenco di dizionari rappresentanti i dati geojson delle posizioni dei server
        server_data = []
        for row in result.fetchall():
            lon, lat = row[0], row[1]
            server_geojson = {
                "type": "Feature",
                "geometry": {
                    "type": "Point",
                    "coordinates": [lon, lat]
                }
            }
            server_data.append(server_geojson)

        # Restituisco i dati delle posizioni dei server al frontend
        return jsonify(server_data), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500



if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug= True)
    #app.run(debug=True)