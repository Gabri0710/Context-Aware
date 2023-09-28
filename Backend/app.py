from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
from geoalchemy2 import Geometry
from sqlalchemy import create_engine

app = Flask(__name__)

# Configuro la connessione con il database
app.config['SQLALCHEMY_DATABASE_URI'] = 'postgresql://postgres:password@localhost/geofance-emergency'
db = SQLAlchemy(app)

i = "user0"
user_used = None


# Definisco il modello per la tabella che contiene activity e posizione degli utenti
class UserUpdate(db.Model):
    __table_args__ = {'schema': 'emergency-schema'}  # Imposta lo schema qui
    __tablename__ = 'user-information'  # Il nome della tabella senza lo schema
    username = db.Column(db.String(255), primary_key=True)
    posizione = db.Column(Geometry(geometry_type='POINT', srid=4326))
    activity = db.Column(db.String(7))


# Definisco il modello per la tabella degli utenti
class UserManagement(db.Model):
    __table_args__ = {'schema': 'emergency-schema'}  # Imposta lo schema qui
    __tablename__ = 'user-credentials'  # Il nome della tabella senza lo schema
    username = db.Column(db.String(255), primary_key=True)
    password = db.Column(db.String(255))

    def create_user(self):
        # Crea un nuovo utente e inserisce i dati nel database
        db.session.add(self)
        db.session.commit()

    @classmethod
    def find_user_by_username(cls, username):
        # Trova un utente nel database dato il suo username
        return cls.query.filter_by(username=username).first()

    def check_password(self, password):
        # Verifica la password dell'utente
        return check_password_hash(self.password, password)



@app.route('/')
def hello_world():
    return '<h1>GEOFECE EMERGENCY</h1>'


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

        #ottengo l'username dell'utente (DA CAMBIARE, MOMENTANEO PER TEST)
        #username = i                    #DA SOSTITUIRE CON USER_USED

        print(latitude)
        print(longitude)
        print(activity)

        # Creo un oggetto Point con le coordinate
        coordinates = f'POINT({longitude} {latitude})'
        

        # Creo un nuovo record nella tabella
        user_update = UserUpdate(username=username, posizione=coordinates, activity=activity)

        # Provo a inserire (o aggiornare) il record
        try:
            db.session.merge(user_update)
            db.session.commit()
            print("Record inserito o aggiornato con successo")
        except Exception as e:
            print(f"Errore durante inserimento o aggiornamento: {str(e)}")
        

        #print(f"Latitude: {latitude}, Longitude: {longitude}")

        #i+=1
        return jsonify({"message": "Dati di posizione ricevuti correttamente."}), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8080, debug= False)
    #app.run(debug=True)
