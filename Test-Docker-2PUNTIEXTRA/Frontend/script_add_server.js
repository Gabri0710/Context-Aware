document.addEventListener('DOMContentLoaded', function() {

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

    var map = new ol.Map({
        target: 'map',
        layers: [osmLayer, pointsLayer], 
        view: new ol.View({
            center: ol.proj.fromLonLat([11.3394883, 44.4938134]),
            zoom: 13, 
        })
        });
    
    

    map.on('click', function(event) {
                if (event.originalEvent.ctrlKey) {
                    selectedPoints.pop()
                }
                else if (selectedPoints.getLength() === 0) {
                    var coordinate = event.coordinate;
                    var pointFeature = new ol.Feature(new ol.geom.Point(coordinate));
                    selectedPoints.push(pointFeature);
                }
        
    });

    var serverLayer;

    function loadServer(){
            fetch("http://localhost:5001/get_server", {
            method: "GET"
            })
            .then(response => response.json())
            .then(data => {
                //console.log(data)
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

    document.getElementById("addServerForm").addEventListener("submit", function(event) {
        event.preventDefault(); // impedisce alla pagina di ricaricarsi quando viene inviato il modulo

        var formData = new FormData(this); // Creo un oggetto FormData

        //definisco le proiezioni. Sul web si lavora con EPSG:3857 ma devo convertirlo a EPSG:4326 per lavorare con le nostre coordinate note
        var projection3857 = ol.proj.get('EPSG:3857');
        var projection4326 = ol.proj.get('EPSG:4326');

        selectedPoints.forEach(function(feature) {
            // Ottengo le coordinate della feature
            
            var coordinates = ol.proj.transform(feature.getGeometry().getCoordinates(), projection3857, projection4326);
            
            // Aggiungo le coordinate alla FormData con chiave coordinates
            formData.append('coordinates', coordinates);
            
        });

        // Eseguo una richiesta POST al backend
        fetch("http://localhost:5001/add_server", {
            method: "POST",
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            selectedPoints.clear();
            loadServer();
        })
        .catch(error => {
            // Gestisco gli errori
            console.error("Errore:", error);
        });

    });

    
    document.getElementById("loadServer").addEventListener("click", function() {
        loadServer();
    });
    
});