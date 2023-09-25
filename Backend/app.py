from flask import Flask, request, jsonify

app = Flask(__name__)

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

        # Stampa le coordinate a schermo
        print(f"Latitude: {latitude}, Longitude: {longitude}")

        # Puoi elaborare ulteriormente i dati o inviare una risposta al client Android se necessario

        return jsonify({"message": "Dati di posizione ricevuti correttamente."}), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(debug=True)
