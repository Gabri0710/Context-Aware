document.addEventListener('DOMContentLoaded', function() {

    loadGeofence();
    setInterval(loadGeofence, 2000);

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
    

    var map = new ol.Map({
        target: 'map',
        layers: [osmLayer, pointsLayer], 
        view: new ol.View({
            center: ol.proj.fromLonLat([11.3394883, 44.4938134]),
            zoom: 13, 
        })
        });
    
    
    var openmenu = 0;
    var myMenu = document.getElementById('clickMenu');
    var myDeleteField = document.getElementById('deleteField');
    var idToDelete = "";

    map.on('click', function(event) {
        if (openmenu==1){
            var coordinate = map.getCoordinateFromPixel(event.pixel);

            var menuX = event.pixel[0] + 'px';
            var menuY = event.pixel[1] + 'px';

            myMenu.style.left = menuX;
            myMenu.style.top = menuY;

            // Mostra il menu del contesto
            myMenu.style.display = 'block';

            myDeleteField.onclick = function() {
                deleteGeofence(idToDelete);
                loadGeofence();
                // Fai qualcosa quando viene cliccato su "MIOCAMPO"
                console.log('Campo cliccato!');
                // Nascondi il menu del contesto
                myMenu.style.display = 'none';
            };
        }

        else{
            myMenu.style.display = 'none';
            if (event.originalEvent.ctrlKey) {
                selectedPoints.pop()
            }
            else{
                var coordinate = event.coordinate;
                var pointFeature = new ol.Feature(new ol.geom.Point(coordinate));
                selectedPoints.push(pointFeature);
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


    function updateUsersLocation(){
        if (userVisualization=="WALKING"){
                fetch("http://localhost:5000/get_walking_user_data", {
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
                fetch("http://localhost:5000/get_car_user_data", {
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


    function loadGeofence(){
        fetch("http://localhost:5000/get_geofence", {
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

                //prendo il colore e lo normalizzo, poi definisco il fillColor. VALORE 1.5=NUMERO MASSIMO DI UTENTI, DA MODIFICARE IN SEGUITO
                var colorNormalization = n_users/1.5;
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

                //pusho ogni singola feature (poligono) personalizzata in un array di features
                polygonsFeatures.push(singlePolygonFeature);
            });

            //creo il layer con i vari poligoni personalizzati
            geofenceLayer = new ol.layer.Vector({
                source: new ol.source.Vector({
                    features: polygonsFeatures
                })
            });

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
        fetch("http://localhost:5000/delete_geofence", {
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
        fetch("http://localhost:5000/delete_geofence", {
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
            loadGeofence();
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
        fetch("http://localhost:5000/add_geofence", {
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
        var formData = new FormData(this); // Crea un oggetto FormData dal modulo

        // Esegui una richiesta POST al backend
        fetch("http://localhost:5000/delete_geofence", {
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
    });


    document.getElementById("viewWalkingUserButton").addEventListener("click", function() {
        userVisualization = "WALKING";
        updateUsersLocation();
    });


    document.getElementById("viewCarUserButton").addEventListener("click", function() {
        userVisualization = "CAR";
        updateUsersLocation();
    });

    document.getElementById("viewGeofence").addEventListener("click", function() {
        loadGeofence()
    });

    
});