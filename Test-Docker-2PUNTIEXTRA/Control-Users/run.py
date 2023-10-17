from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy import create_engine
from sqlalchemy.sql import text
from flask_cors import CORS
from kubernetes import client, config
from kubernetes.client.api import apps_v1_api
import time

app = Flask(__name__)

#UTILE PER TESTARE SULLO STESSO DOMINIO MA PORTE DIVERSE, CORS policy
CORS(app)


# Configuro la connessione con il database
#DATABASE_URI = 'postgresql://postgres:password@database-service:5433/geofence-emergency'
#DATABASE_URI = 'postgresql://postgres:password@database-container/geofence-emergency'
#DATABASE_URI = 'postgresql://postgres:password@database-deployment/geofence-emergency'
DATABASE_URI = 'postgresql://postgres:password@localhost:5431/geofence-emergency'

engine = create_engine(DATABASE_URI)

# Crea una connessione al database
connection = engine.connect()


def get_utenti_server():
    try:
        # Definisci la query SQL
        query = text("""
                SELECT
                    edge_id,
                    COUNT(*) AS numero_utenti_vicini
                FROM (
                    SELECT
                        u.username,
                        e.id AS edge_id,
                        ST_Distance(e.posizione, u.posizione) AS distanza
                    FROM
                        "emergency-schema"."user-information" AS u
                    CROSS JOIN
                        "emergency-schema"."edge-information" AS e
                    WHERE
                        ST_Distance(e.posizione, u.posizione) = (
                            SELECT
                                MIN(ST_Distance(e2.posizione, u2.posizione))
                            FROM
                                "emergency-schema"."edge-information" AS e2
                            CROSS JOIN
                                "emergency-schema"."user-information" AS u2
                            WHERE
                                u2.username = u.username
                        )
                ) AS utenti_vicini_per_edge
                GROUP BY
                    edge_id
                ORDER BY
                    edge_id

                """)

        # Esegui la query e ottieni i risultati
        result = connection.execute(query)

        # Ottieni i risultati della query
        query_result = result.fetchall()

        utenti_server_1 = query_result[0][1]
        utenti_server_2 = query_result[1][1]

        # Ora query_result contiene i risultati della tua query
        print(utenti_server_1)
        print(utenti_server_2)

        return utenti_server_1, utenti_server_2
    except Exception as e:
        print("Errore nella query SQL:", e)



        





def update_pod():
    

    config.load_kube_config()

    apps_api = client.AppsV1Api()

    namespace = "default"
    edge1 = "backend-pod-1"
    edge2 = "backend-pod-2"

    
    utenti_server_1, utenti_server_2 = get_utenti_server() 

    if utenti_server_1 > utenti_server_2:
        # Bilancio il carico su backend-pod-1 e disabilito backend-pod-2
        deployment1 = apps_api.read_namespaced_deployment(name=edge1, namespace=namespace)
        deployment2 = apps_api.read_namespaced_deployment(name=edge2, namespace=namespace)

        deployment1.spec.replicas = 1
        deployment2.spec.replicas = 0

        apps_api.patch_namespaced_deployment(name=edge1, namespace=namespace, body=deployment1)
        apps_api.patch_namespaced_deployment(name=edge2, namespace=namespace, body=deployment2)
        print("Attivato backend-pod-1 e disabilitato backend-pod-2.")
    elif utenti_server_1 < utenti_server_2:
        # Bilancio il carico su backend-pod-2 e disabilito backend-pod-1
        deployment1 = apps_api.read_namespaced_deployment(name=edge1, namespace=namespace)
        deployment2 = apps_api.read_namespaced_deployment(name=edge2, namespace=namespace)

        deployment1.spec.replicas = 0
        deployment2.spec.replicas = 1

        apps_api.patch_namespaced_deployment(name=edge1, namespace=namespace, body=deployment1)
        apps_api.patch_namespaced_deployment(name=edge2, namespace=namespace, body=deployment2)
        print("Attivato backend-pod-2 e disabilitato backend-pod-1.")

        


if __name__=='__main__':
    while True:
        update_pod()
        time.sleep(10)
    