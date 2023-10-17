document.addEventListener('DOMContentLoaded', function() {

    loadGeofence();
    loadServer();
    setInterval(loadGeofence, 3000);
    setInterval(loadServer, 3000);

    //oggetto Feature dove memorizzo i punti scelti
    var selectedPoints = new ol.Collection();

    //oggetto Source, mi serve per prendere in input delle Feature e passarle a un Layer successivamente
    var vectorPoints = new ol.source.Vector({
        features: selectedPoints
    });

    //Layer dei punti scelti
    var pointsLayer = new ol.layer.Vector({
        source: vectorPoints,
        style: new ol.style.Style({
            image: new ol.style.Circle({
              radius: 6,
              fill: new ol.style.Fill({
                color: 'red' // Colore di riempimento del cerchietto
              }),
              stroke: new ol.style.Stroke({
                color: 'black', // Colore del bordo del cerchietto
                width: 2
              })
            })
        })
    });
    
    
    //Layer di OpenStreetMap
    var osmLayer=new ol.layer.Tile({
        source: new ol.source.OSM({
        })
    });
    osmLayer.setVisible(true);

    var walkingUserSource;
    var carUserSource;
    var walkingUserLayer;
    var carUserLayer;
    var geofenceLayer;
    var userVisualization = "CAR";

    var clusterSource;
    var clusterLayer;
    var clusterMode = 0;

    var map = new ol.Map({
        target: 'map',
        layers: [osmLayer, pointsLayer], 
        view: new ol.View({
            center: ol.proj.fromLonLat([11.3394883, 44.4938134]),
            zoom: 13, 
        })
        });
    
    
    var openmenu = 0;
    var idToDelete = "";
    var featureToDelete = null;
    var geofence_idLabel = document.getElementById('id_geofence_label');
    var geofence_nUsersLabel = document.getElementById('n_utenti_label');
    
    

    map.on('click', function(event) {
        if (openmenu==1){
            //geofence_label.textContent = idToDelete;
            geofence_idLabel.textContent = featureToDelete.get('id')
            geofence_nUsersLabel.textContent = featureToDelete.get('n_users').toString();
            document.getElementById("deleteButton").removeAttribute("disabled");
        }

        else{
            if (geofence_idLabel.textContent !== "") {
                geofence_idLabel.textContent = "";
                geofence_nUsersLabel.textContent = "";
                featureToDelete = null;
                document.getElementById("deleteButton").setAttribute("disabled", "true");
            }
            else{
                if (event.originalEvent.ctrlKey) {
                    selectedPoints.pop()
                }
                else{
                    var coordinate = event.coordinate;
                    var pointFeature = new ol.Feature(new ol.geom.Point(coordinate));
                    selectedPoints.push(pointFeature);
                }
            }
            
        }
        
        
    });


    map.on('pointermove', function(event) {
        var feature = map.forEachFeatureAtPixel(event.pixel, function(feature) {
            return feature;
        });
        
        if (feature && feature.get('id')){
            console.log(feature.get('id'))
            openmenu = 1;
            featureToDelete = feature;
            idToDelete = feature.get('id');
        }
        else{
            idToDelete = "";
            openmenu = 0;
        }
        
        //if (feature && feature.get('id')) {
            //var id = feature.get('id');
            //var coordinates = polygonCoordinatesById[id];  // Ottieni le coordinate del poligono dall'ID
            // Visualizza l'ID del poligono e le sue coordinate quando passi sopra con il mouse
           // console.log('ID del poligono:', id);
           // console.log('Coordinate del poligono:', coordinates);
        //} else {
            // Non c'è alcun poligono sotto il cursore del mouse
            // Puoi fare qualcosa qui se vuoi gestire questo caso
       // }
    });

    function clusterUser(){
        formData = new FormData(this);
        
        fetch("http://localhost:5001/createcluster", {
                method: "POST",
                body : formData
                })
                .then(response => response.json())
                .then(data => {

                    const jsonData = JSON.parse(data);
                    map.removeLayer(carUserLayer);
                    map.removeLayer(walkingUserLayer);
                    console.log(data)
                    
                    
                    var clusterFeatures = jsonData.map(item => {
                        const coordinates = [item.longitudine, item.latitudine];
                        
                        const point = new ol.Feature({
                            geometry: new ol.geom.Point(ol.proj.fromLonLat(coordinates)),
                        });
                        var cluster_id = item.CLUSTER_ID;
                        
                        var fillColor = 'rgba('+255*cluster_id+','+255*(1-cluster_id)+', 0, 0.8)'
                        point.setStyle(new ol.style.Style({
                            image: new ol.style.Circle({
                                radius: 6,
                                fill: new ol.style.Fill({
                                    color: fillColor 
                                })
                            })
                        }));
                        return point;
                    });
                
                    // Creazione del livello vettoriale
                    clusterSource = new ol.source.Vector({
                        features: clusterFeatures
                    });
                    
                
                    clusterLayer = new ol.layer.Vector({
                        source: clusterSource
                    });
                
                    // Aggiungi il livello vettoriale alla mappa
                    map.addLayer(clusterLayer);
                })
                .catch(error => {
                    // Gestisci gli errori
                    console.error("Errore:", error);
                });
    }

    function updateUsersLocation(){
        if (userVisualization=="WALKING"){
                fetch("http://localhost:5001/get_walking_user_data", {
                method: "GET"
                })
                .then(response => response.json())
                .then(data => {
                    map.removeLayer(carUserLayer);
                    map.removeLayer(walkingUserLayer);
                    //console.log(data)
                    var walkingUserFeatures = data.map(item => {
                        const coordinates = item.geometry.coordinates;
                        const point = new ol.Feature({
                            geometry: new ol.geom.Point(ol.proj.fromLonLat(coordinates)),
                        });
                        return point;
                    });
                
                    // Creazione del livello vettoriale
                    walkingUserSource = new ol.source.Vector({
                        features: walkingUserFeatures,
                    });
                    
                
                    walkingUserLayer = new ol.layer.Vector({
                        source: walkingUserSource,
                        style: new ol.style.Style({
                            image: new ol.style.Circle({
                                radius: 6,
                                fill: new ol.style.Fill({
                                    color: 'blue', // Colore del punto
                                }),
                            }),
                        }),
                    });
                
                    // Aggiungi il livello vettoriale alla mappa
                    map.addLayer(walkingUserLayer);
                    userVisualization = "WALKING";
                })
                .catch(error => {
                    // Gestisci gli errori
                    console.error("Errore:", error);
                });
        }
        else{
                fetch("http://localhost:5001/get_car_user_data", {
                method: "GET"
                })
                .then(response => response.json())
                .then(data => {
                    map.removeLayer(walkingUserLayer);
                    map.removeLayer(carUserLayer);
                    console.log(data)
                    var carUserFeatures = data.map(item => {
                        const coordinates = item.geometry.coordinates;
                        const point = new ol.Feature({
                            geometry: new ol.geom.Point(ol.proj.fromLonLat(coordinates)),
                        });
                        return point;
                    });
                    
                
                    // Creazione del livello vettoriale
                    carUserSource = new ol.source.Vector({
                        features: carUserFeatures,
                    });
                
                    carUserLayer = new ol.layer.Vector({
                        source: carUserSource,
                        style: new ol.style.Style({
                            image: new ol.style.Circle({
                                radius: 6,
                                fill: new ol.style.Fill({
                                    color: 'blue', // Colore del punto
                                }),
                            }),
                        }),
                    });
                
                    // Aggiungi il livello vettoriale alla mappa
                    map.addLayer(carUserLayer);
                    userVisualization = "CAR";
                })
                .catch(error => {
                    // Gestisci gli errori
                    console.error("Errore:", error);
                });
        }
        


    }


    var serverLayer;

    function loadServer(){
            fetch("http://localhost:5001/get_server", {
            method: "GET"
            })
            .then(response => response.json())
            .then(data => {
                console.log("ok");
                console.log(data);
                map.removeLayer(serverLayer);
                var serverFeatures = data.map(item => {
                    const coordinates = item.geometry.coordinates;
                    const point = new ol.Feature({
                        geometry: new ol.geom.Point(ol.proj.fromLonLat(coordinates)),
                    });
                    return point;
                });
            
                // Creazione del livello vettoriale
                serverSource = new ol.source.Vector({
                    features: serverFeatures,
                });
                
            
                serverLayer = new ol.layer.Vector({
                    source: serverSource,
                    style: new ol.style.Style({
                        image: new ol.style.Circle({
                            radius: 7,
                            fill: new ol.style.Fill({
                                color: 'black', // Colore del punto
                            }),
                        }),
                    }),
                });
            
                // Aggiungi il livello vettoriale alla mappa
                map.addLayer(serverLayer);
            })
            .catch(error => {
                // Gestisci gli errori
                console.error("Errore:", error);
            });
        

    }


    function loadGeofence(){
        fetch("http://localhost:5001/get_geofence", {
            method: "GET"
        })
        .then(response => response.json())
        .then(data => {
            console.log(data);
            map.removeLayer(geofenceLayer);
            var polygonsFeatures = [];
            data.forEach(function(obj) {
                var id = obj.id;
                var n_users = obj.n_users; 
                var points = obj.points;
                var coordinates = points.map(function(record) {
                    return ol.proj.fromLonLat(record.geometry.coordinates);
                });
                

                var polygon = new ol.geom.Polygon([coordinates]);
                var singlePolygonFeature = new ol.Feature(polygon);

                //prendo il colore e lo normalizzo, poi definisco il fillColor. VALORE 6=NUMERO MASSIMO DI UTENTI, DA MODIFICARE IN SEGUITO
                var colorNormalization = n_users/6;
                var fillColor = 'rgba('+255*colorNormalization+','+255*(1-colorNormalization)+', 0, 0.2)'
                
                //setto il colore di riempimento di ogni singola feature
                singlePolygonFeature.setStyle(new ol.style.Style({
                    fill: new ol.style.Fill({
                        color: fillColor
                    }),
                    stroke: new ol.style.Stroke({
                        color: 'grey', // Colore del bordo del poligono
                        width: 1 // Larghezza del bordo
                    })
                }));

                singlePolygonFeature.set('id', id);
                singlePolygonFeature.set('n_users', n_users);

                //pusho ogni singola feature (poligono) personalizzata in un array di features
                polygonsFeatures.push(singlePolygonFeature);
            });

            //creo il layer con i vari poligoni personalizzati
            geofenceLayer = new ol.layer.Vector({
                source: new ol.source.Vector({
                    features: polygonsFeatures
                })
            });

            if(clusterMode==0)
                updateUsersLocation();
            map.addLayer(geofenceLayer);
            
            
        })
        .catch(error => {
            // Gestisci gli errori
            console.error("Errore:", error);
        });
    }

    /*
    function deleteGeofence(){
        event.preventDefault(); // Previeni il comportamento di default del modulo (l'invio della pagina)

        var formData = new FormData(this); // Crea un oggetto FormData dal modulo

        // Esegui una richiesta POST al backend
        fetch("http://localhost:5001/delete_geofence", {
            method: "POST",
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            // Gestisci la risposta dal backend
            console.log(data);
            loadGeofence();
        })
        .catch(error => {
            // Gestisci gli errori
            console.error("Errore:", error);
        });
    }
    */

    function deleteGeofence(s){
        
        var queryString = 'id_allarme='+s;

        // Esegui una richiesta POST al backend
        fetch("http://localhost:5001/delete_geofence", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: queryString
        })
        .then(response => response.json())
        .then(data => {
            // Gestisci la risposta dal backend
            console.log(data);
            //loadGeofence();
        })
        .catch(error => {
            // Gestisci gli errori
            console.error("Errore:", error);
        });
    }

    document.getElementById("addGeofenceForm").addEventListener("submit", function(event) {
        event.preventDefault(); // impedisce alla pagina di ricaricarsi quando viene inviato il modulo

        var formData = new FormData(this); // Creo un oggetto FormData

        //definisco le proiezioni. Sul web si lavora con EPSG:3857 ma devo convertirlo a EPSG:4326 per lavorare con le nostre coordinate note
        var projection3857 = ol.proj.get('EPSG:3857');
        var projection4326 = ol.proj.get('EPSG:4326');

        selectedPoints.forEach(function(feature) {
            // Ottengo le coordinate della feature
            
            var coordinates = ol.proj.transform(feature.getGeometry().getCoordinates(), projection3857, projection4326);
            
            // Aggiungo le coordinate alla FormData con chiave coordinates
            formData.append('coordinates[]', coordinates);
            
        });

        // Eseguo una richiesta POST al backend
        fetch("http://localhost:5001/add_geofence", {
            method: "POST",
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            // Gestisco la risposta dal backend
            //console.log(data);
            //map.removeLayer(pointsLayer);
            //map.removeLayer(geofenceLayer);
            selectedPoints.clear();
            loadGeofence();
        })
        .catch(error => {
            // Gestisco gli errori
            console.error("Errore:", error);
        });

    });

    document.getElementById("deleteGeofenceForm").addEventListener("submit", function(event) {
        /*
         formData = new FormData(this); // Crea un oggetto FormData dal modulo

        // Esegui una richiesta POST al backend
        fetch("http://localhost:5001/delete_geofence", {
            method: "POST",
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            // Gestisci la risposta dal backend
            console.log(data);
            loadGeofence();
        })
        .catch(error => {
            // Gestisci gli errori
            console.error("Errore:", error);
        });
        */
        event.preventDefault();
        deleteGeofence(featureToDelete.get('id'));
        geofence_idLabel.textContent = "";
        geofence_nUsersLabel.textContent = "";
        featureToDelete = null;
        document.getElementById("deleteButton").setAttribute("disabled", "true");
    });


    document.getElementById("viewWalkingUserButton").addEventListener("click", function() {
        userVisualization = "WALKING";
        clusterMode=0;
        updateUsersLocation();
    });


    document.getElementById("viewCarUserButton").addEventListener("click", function() {
        userVisualization = "CAR";
        clusterMode=0;
        updateUsersLocation();
    });


    document.getElementById("ClusterForm").addEventListener("submit", function() {
        event.preventDefault();
        formData = new FormData(this);
        
        fetch("http://localhost:5001/get_cluster", {
                method: "POST",
                body : formData
                })
                .then(response => response.json())
                .then(data => {

                    const jsonData = JSON.parse(data);
                    map.removeLayer(carUserLayer);
                    map.removeLayer(walkingUserLayer);
                    console.log(data)
                    
                    var colorArray= new Array(100).fill("-1");

                    var clusterFeatures = jsonData.map(item => {
                        const coordinates = [item.longitudine, item.latitudine];
                        
                        const point = new ol.Feature({
                            geometry: new ol.geom.Point(ol.proj.fromLonLat(coordinates)),
                        });
                        var cluster_id = item.CLUSTER_ID;

                        if(colorArray[cluster_id]==="-1"){
                            fillColor = 'rgba('+Math.floor(Math.random() * 256)+','+Math.floor(Math.random() * 256)+','+Math.floor(Math.random() * 256)+', 0.8)'
                            colorArray[cluster_id] = fillColor;
                        }

                        console.log(fillColor);
                        
                        //var fillColor = 'rgba('+255*cluster_id+','+255*(1-cluster_id)+', 0, 0.8)'
                        point.setStyle(new ol.style.Style({
                            image: new ol.style.Circle({
                                radius: 6,
                                fill: new ol.style.Fill({
                                    color: colorArray[cluster_id]
                                })
                            })
                        }));
                        return point;
                    });
                
                    // Creazione del livello vettoriale
                    clusterSource = new ol.source.Vector({
                        features: clusterFeatures
                    });
                    
                
                    clusterLayer = new ol.layer.Vector({
                        source: clusterSource
                    });
                
                    // Aggiungi il livello vettoriale alla mappa
                    map.addLayer(clusterLayer);
                    clusterMode=1;
                })
                .catch(error => {
                    // Gestisci gli errori
                    console.error("Errore:", error);
                });
    });


    
    document.getElementById("viewElbowCluster").addEventListener("click", function() {
        fetch("http://localhost:5001/get_cluster_elbow", {
                method: "GET"
                })
                .then(response => response.json())
                .then(data => {

                    const jsonData = JSON.parse(data);
                    map.removeLayer(carUserLayer);
                    map.removeLayer(walkingUserLayer);
                    map.removeLayer(clusterLayer);
                    console.log(data)
                    
                    var colorArray= new Array(100).fill("-1");
                    var clusterFeatures = jsonData.map(item => {
                        const coordinates = [item.longitudine, item.latitudine];
                        
                        const point = new ol.Feature({
                            geometry: new ol.geom.Point(ol.proj.fromLonLat(coordinates)),
                        });
                        var cluster_id = item.CLUSTER_ID;

                        if(colorArray[cluster_id]==="-1"){
                            fillColor = 'rgba('+Math.floor(Math.random() * 256)+','+Math.floor(Math.random() * 256)+','+Math.floor(Math.random() * 256)+', 0.8)'
                            colorArray[cluster_id] = fillColor;
                        }

                        console.log(fillColor);
                        
                        //var fillColor = 'rgba('+255*cluster_id+','+255*(1-cluster_id)+', 0, 0.8)'
                        point.setStyle(new ol.style.Style({
                            image: new ol.style.Circle({
                                radius: 6,
                                fill: new ol.style.Fill({
                                    color: colorArray[cluster_id]
                                })
                            })
                        }));
                        return point;
                    });

                    var n_cluster = 0;
                    while (colorArray[n_cluster]!="-1" && n_cluster<100){
                        n_cluster+=1;
                    }

                    
                    document.getElementById("elbowLabel").textContent = "Numero ottimale di cluster rilevati con metodo elbow: " + n_cluster;
                    // Creazione del livello vettoriale
                    clusterSource = new ol.source.Vector({
                        features: clusterFeatures
                    });
                    
                
                    clusterLayer = new ol.layer.Vector({
                        source: clusterSource
                    });
                
                    // Aggiungi il livello vettoriale alla mappa
                    map.addLayer(clusterLayer);
                    clusterMode=1;
                })
                .catch(error => {
                    // Gestisci gli errori
                    console.error("Errore:", error);
                });
    });


    document.getElementById("loadServer").addEventListener("click", function() {
        loadServer();
    });
    
});