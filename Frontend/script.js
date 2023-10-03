document.addEventListener('DOMContentLoaded', function() {

    loadGeofence();

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
    
    
    

    map.on('click', function(event) {
        var coordinate = event.coordinate;
        var pointFeature = new ol.Feature(new ol.geom.Point(coordinate));
        selectedPoints.push(pointFeature);
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
    
            data.forEach(function(path) {
                var coordinates = path.map(function(point) {
                    return ol.proj.fromLonLat(point.geometry.coordinates);
                });
    
                var polygon = new ol.geom.Polygon([coordinates]);
                var singlePolygonFeature = new ol.Feature(polygon);
                polygonsFeatures.push(singlePolygonFeature);
            });
    
            geofenceLayer = new ol.layer.Vector({
                source: new ol.source.Vector({
                    features: polygonsFeatures
                })
            });
    
           
            map.addLayer(geofenceLayer);
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