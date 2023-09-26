from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
from geoalchemy2 import Geometry
from sqlalchemy import create_engine

app = Flask(__name__)

# Configuro la connessione con il database
app.config['SQLALCHEMY_DATABASE_URI'] = 'postgresql://postgres:password@localhost/geofance'
db = SQLAlchemy(app)

i = 0

# Definisco il modello per la tabella degli utenti
class UserCoordinate(db.Model):
    __table_args__ = {'schema': 'Coordinate'}  # Imposta lo schema qui
    __tablename__ = 'geofance-coordinate'  # Il nome della tabella senza lo schema
    user_id = db.Column(db.Integer, primary_key=True)
    posizione = db.Column(Geometry(geometry_type='POINT', srid=4326))


@app.route('/')
def hello_world():
    return '<h1>Benvenuto</h1>'

@app.route('/upload_location', methods=['POST'])
def upload_location():
    try:
        # Ottiengo i dati di posizione dalla richiesta POST
        data = request.get_json()

        

        # Estrai le coordinate latitudine e longitudine dai dati
        latitude = data.get('latitude')
        longitude = data.get('longitude')

        
        

        #ottengo l'username dell'utente (DA CAMBIARE, MOMENTANEO PER TEST)
        user_id = i

        print(latitude)
        print(longitude)

        # Creo un oggetto Point con le coordinate
        coordinates = f'POINT({longitude} {latitude})'
        #coordinates = "POINT("
        #coordinates+=str(longitude)
        #coordinates+=str(latitude)
        #coordinates+=")"

        

        # Crea un nuovo record nella tabella
        new_user_coordinate = UserCoordinate(user_id=user_id, posizione=coordinates)

        print("TESTTTTTTTTTTTTTTTTTTT")

        try:
            # Codice per l'inserimento del record
            db.session.add(new_user_coordinate)
            db.session.commit()
            print("Record inserito con successo")
        except Exception as e:
            print(f"Errore durante l'inserimento: {str(e)}")
        

        # Stampa le coordinate a schermo
        print(f"Latitude: {latitude}, Longitude: {longitude}")

        # Puoi elaborare ulteriormente i dati o inviare una risposta al client Android se necessario
        #i+=1
        return jsonify({"message": "Dati di posizione ricevuti correttamente."}), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(debug=True)
