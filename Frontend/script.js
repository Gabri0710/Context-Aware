document.addEventListener('DOMContentLoaded', function() {
    //oggetto Feature dove memorizzo i punti scelti
    var selectedFeatures = new ol.Collection();

    //oggetto Source, mi serve per prendere in input delle Feature e passarle a un Layer successivamente
    var vectorSource = new ol.source.Vector({
        features: selectedFeatures
    });

    //Layer dei punti scelti
    var vectorLayer = new ol.layer.Vector({
        source: vectorSource,
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
    

    var map = new ol.Map({
        target: 'map',
        layers: [osmLayer, vectorLayer], 
        view: new ol.View({
            center: ol.proj.fromLonLat([11.3394883, 44.4938134]),
            zoom: 13, 
        })
        });
    
    
    

    map.on('click', function(event) {
        var coordinate = event.coordinate;
        var pointFeature = new ol.Feature(new ol.geom.Point(coordinate));
        selectedFeatures.push(pointFeature);
    });

    document.getElementById("addGeofenceForm").addEventListener("submit", function(event) {
        event.preventDefault(); // impedisce alla pagina di ricaricarsi quando viene inviato il modulo

        var formData = new FormData(this); // Creo un oggetto FormData

        //definisco le proiezioni. Sul web si lavora con EPSG:3857 ma devo convertirlo a EPSG:4326 per lavorare con le nostre coordinate note
        var projection3857 = ol.proj.get('EPSG:3857');
        var projection4326 = ol.proj.get('EPSG:4326');

        selectedFeatures.forEach(function(feature) {
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
            console.log(data);
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
        })
        .catch(error => {
            // Gestisci gli errori
            console.error("Errore:", error);
        });
    });
});