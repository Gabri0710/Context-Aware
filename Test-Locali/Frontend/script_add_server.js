document.addEventListener('DOMContentLoaded', function() {

    //oggetto Feature dove memorizzo il punto scelto dove inserire il server
    var selectedPoints = new ol.Collection();

    //oggetto Source associato alle feature dei punti
    var vectorPoints = new ol.source.Vector({
        features: selectedPoints
    });

    //Layer dei punti selezionati
    var pointsLayer = new ol.layer.Vector({
        source: vectorPoints,
        style: new ol.style.Style({
            image: new ol.style.Circle({
              radius: 6,
              fill: new ol.style.Fill({
                color: 'red'
              }),
              stroke: new ol.style.Stroke({
                color: 'black',
                width: 2
              })
            })
        })
    });
    
    //layer dei server
    var serverLayer;
    
    //Layer di OpenStreetMap
    var osmLayer=new ol.layer.Tile({
        source: new ol.source.OSM({
        })
    });
    osmLayer.setVisible(true);

    //inizializzo la mappa con i layer scelti
    var map = new ol.Map({
        target: 'map',
        layers: [osmLayer, pointsLayer], 
        view: new ol.View({
            center: ol.proj.fromLonLat([11.3394883, 44.4938134]),
            zoom: 13, 
        })
    });
    
    
    //al click
    map.on('click', function(event) {
                if (event.originalEvent.ctrlKey) {                                                      //se ho ctrl premuto, tolgo il punto
                    selectedPoints.pop()
                }
                else if (selectedPoints.getLength() === 0) {                                            //se non ho ancora scelto il punto, lo inserisco
                    var coordinate = event.coordinate;
                    var pointFeature = new ol.Feature(new ol.geom.Point(coordinate));
                    selectedPoints.push(pointFeature);
                }
        
    });

    

    //funzione che carica i server già presenti
    function loadServer(){
            fetch("http://localhost:5001/get_server", {
            method: "GET"
            })
            .then(response => response.json())
            .then(data => {

                //tolgo i layer presenti precedentemente
                map.removeLayer(serverLayer);

                //inizializzo le feature con il punto selezionato
                var serverFeatures = data.map(item => {
                    const coordinates = item.geometry.coordinates;
                    const point = new ol.Feature({
                        geometry: new ol.geom.Point(ol.proj.fromLonLat(coordinates)),
                    });
                    return point;
                });
            
                // source associata al server
                serverSource = new ol.source.Vector({
                    features: serverFeatures,
                });
                
                
                //layer associato al server
                serverLayer = new ol.layer.Vector({
                    source: serverSource,
                    style: new ol.style.Style({
                        image: new ol.style.Circle({
                            radius: 7,
                            fill: new ol.style.Fill({
                                color: 'black',
                            }),
                        }),
                    }),
                });
            
                // Aggiungo il layer
                map.addLayer(serverLayer);
            })
            .catch(error => {
                console.error("Errore:", error);
            });
        

    }

    //aggiunta di un server al click del pulsante
    document.getElementById("addServerForm").addEventListener("submit", function(event) {
        event.preventDefault();                                 // impedisce alla pagina di ricaricarsi quando viene inviato il modulo

        var formData = new FormData(this);              

        //definisco le proiezioni. Sul web si lavora con EPSG:3857 ma devo convertirlo a EPSG:4326 per lavorare con le nostre coordinate note
        var projection3857 = ol.proj.get('EPSG:3857');
        var projection4326 = ol.proj.get('EPSG:4326');

        selectedPoints.forEach(function(feature) {
            // Ottengo le coordinate della feature
            var coordinates = ol.proj.transform(feature.getGeometry().getCoordinates(), projection3857, projection4326);
            
            // Aggiungo le coordinate alla FormData
            formData.append('coordinates', coordinates);
            
        });

        // Eseguo una richiesta POST al backend
        fetch("http://localhost:5001/add_server", {
            method: "POST",
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            selectedPoints.clear();                                     //rimuovo il punto dopo averlo mandato
            loadServer();                                               //ricarico i server
        })
        .catch(error => {
            console.error("Errore:", error);
        });

    });

    
    document.getElementById("loadServer").addEventListener("click", function() {
        loadServer();
    });
    
});