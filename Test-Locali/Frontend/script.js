document.addEventListener('DOMContentLoaded', function() {

    loadGeofence();
    loadServer();
    setInterval(loadGeofence, 3000);
    setInterval(loadServer, 3000);

    var walkingUserSource;                                                  //source utenti a piedi
    var carUserSource;                                                      //source utenti in macchina
    var allUserSource;                                                      //source di tutti gli utenti
    var clusterSource;                                                      //source dei cluster
    var walkingUserLayer;                                                   //layer degli utenti a piedi
    var carUserLayer;                                                       //layer degli utenti in macchina
    var allUserLayer;                                                       //layer di tutti gli utenti
    var geofenceLayer;                                                      //layer dei geofence
    var clusterLayer;                                                       //layer dei cluster
    var serverLayer;                                                        //layer dei server

    var addmode = 1;                                                        //flag per capire se stiamo cercando di aggiungere un geofence
    var deletemode = 0;                                                     //flag per capire se stiamo cercando di eliminare un geofence
    var isFeature = 0;                                                      //flag per capire se abbiamo il mouse su un geofence o no
    var featureToDelete = null;                                             //feature associata al geofence da eliminare
    var userVisualization = "ALL";                                          //variabile che memorizza la tipologia di visualizzazione che vogliamo (piedi, macchina, cluster)

    var geofence_idLabel = document.getElementById('id_geofence_label');            //riferimento alla label del geofence selezionato
    var geofence_nUsersLabel = document.getElementById('n_utenti_label');           //riferimento alla label che esprime il numero di utenti del geofence selezionato
    var geofence_titleLabel = document.getElementById('titolo_geofence_label');     //riferimento alla label del titolo del geofence selezionato

    //oggetto Feature dove memorizzo i punti scelti per la creazione del geofence
    var selectedPoints = new ol.Collection();

    //oggetto Source, mi serve per prendere in input le Feature riguardanti i punti scelti per definire il geofence e passarle a un Layer successivamente
    var vectorPoints = new ol.source.Vector({
        features: selectedPoints
    });

    //Layer dei punti scelti per il geofence
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
    
    // funzione che viene richiamata al movimento del mouse
    map.on('pointermove', function(event) {
        //inizializzo la feature con quella presente nella posizione del mouse
        var feature = map.forEachFeatureAtPixel(event.pixel, function(feature) {
            return feature;
        });

        
        //se esiste la feature e ha un id (quindi è una feature associata a un geofence)
        if (feature && feature.get('id')){
            isFeature = 1;                                                              //abilito il flag per indicare che siamo su un geofence
            featureToDelete = feature;                                                  //imposto la feature da eliminare con quella selezionata
        }
        else{
            isFeature = 0;                                                              //disabilito il flag per indicare che NON siamo su un geofence                                                    
        }
        
    });
    
    // funzione che viene richiamata al click
    map.on('click', function(event) {
        // se stiamo cliccando su un geofence
        if (deletemode==1){
            if(isFeature==1){
                geofence_idLabel.textContent = featureToDelete.get('id');                                    //aggiorno la label di visualizzazione
                geofence_nUsersLabel.textContent = featureToDelete.get('n_users').toString();                //aggiorno la label di visualizzazione
                geofence_titleLabel.textContent = featureToDelete.get('title');                             //aggiorno la label di visualizzazione
                document.getElementById("deleteButton").removeAttribute("disabled");                        //attivo il button per cancellare il geofence
                document.getElementById("deleteButton").classList.add("btn-primary");                       //indico il pulsante come quello selezionato
                document.getElementById("deleteButton").classList.remove("btn-outline-secondary");           //indico il pulsante come quello selezionato
                
                updateMode("delete", "cluster", "addgeofence");                                             //aggiorno visualizzazione
                selectedPoints.clear();                                                                     //rimuovo i punti selezionati
                addmode = 0;                                                                                //imposto il flag per l'aggiunta di geofence a 0
            }
            else{
                geofence_idLabel.textContent = "-";                                                      //aggiorno la label di visualizzazione      
                geofence_nUsersLabel.textContent = "-";                                                  //aggiorno la label di visualizzazione
                geofence_titleLabel.textContent = "-";                                                  //aggiorno la label di visualizzazione
                featureToDelete = null;                                                                 //porto a null la label da eliminare
                document.getElementById("deleteButton").setAttribute("disabled", "true");               //disabilito il button per la visualizzazione
                document.getElementById("deleteButton").classList.add("btn-outline-secondary");         //indico il pulsante come quello selezionato
                document.getElementById("deleteButton").classList.remove("btn-primary");                //indico il pulsante come quello selezionato
            }
            
        }

        else if (addmode==1){
            //se sto tenendo ctrl premuto
            if (event.originalEvent.ctrlKey) {
                selectedPoints.pop()                                                                //faccio il pop dell'ultimo punto selezionato
            }

            //se sto solo cliccando
            else{
                var coordinate = event.coordinate;                                                  //prendo le coordinate del punto
                var pointFeature = new ol.Feature(new ol.geom.Point(coordinate));                   //creo un nuovo punto con le coordinate del click
                selectedPoints.push(pointFeature);                                                  //aggiungo il punto alla mia collezione di punti scelti per la definizione del geofence
            }
                
        }
        
        
    });


    //funzione di aggiornamento della posizione degli utenti
    function updateUsersLocation(){
        //se la visualizzazione è impostata agli utenti a piedi
        if (userVisualization=="WALKING"){
                fetch("http://localhost:5001/get_walking_user_data", {
                method: "GET"
                })
                .then(response => response.json())
                .then(data => {
                    //rimuovo i layer presenti
                    map.removeLayer(allUserLayer);
                    map.removeLayer(carUserLayer);
                    map.removeLayer(walkingUserLayer);
                    map.removeLayer(clusterLayer);

                    //inizializzo le feature associate alla posizione con i dati ottenuti dal backend
                    var walkingUserFeatures = data.map(item => {
                        const coordinates = item.geometry.coordinates;
                        const point = new ol.Feature({
                            geometry: new ol.geom.Point(ol.proj.fromLonLat(coordinates)),
                        });
                        return point;
                    });
                
                    // Source associata alle feature
                    walkingUserSource = new ol.source.Vector({
                        features: walkingUserFeatures,
                    });
                    
                    
                    //layer associato agli utenti a piedi
                    walkingUserLayer = new ol.layer.Vector({
                        source: walkingUserSource,
                        style: new ol.style.Style({
                            image: new ol.style.Circle({
                                radius: 6,
                                fill: new ol.style.Fill({
                                    color: 'blue',
                                }),
                            }),
                        }),
                    });
                
                    // Aggiungo il layer
                    map.addLayer(walkingUserLayer);
                    userVisualization = "WALKING";
                })
                .catch(error => {
                    console.error("Errore:", error);
                });
        }
        //se la visualizzazione è impostata agli utenti in macchina
        else if(userVisualization=="CAR"){
                fetch("http://localhost:5001/get_car_user_data", {
                method: "GET"
                })
                .then(response => response.json())
                .then(data => {
                    //rimuovo i layer presenti
                    map.removeLayer(allUserLayer);
                    map.removeLayer(walkingUserLayer);
                    map.removeLayer(carUserLayer);
                    map.removeLayer(clusterLayer);
                    
                    //inizializzo le feature associate alla posizione con i dati ottenuti dal backend
                    var carUserFeatures = data.map(item => {
                        const coordinates = item.geometry.coordinates;
                        const point = new ol.Feature({
                            geometry: new ol.geom.Point(ol.proj.fromLonLat(coordinates)),
                        });
                        return point;
                    });
                    
                
                    // Source associata alle feature
                    carUserSource = new ol.source.Vector({
                        features: carUserFeatures,
                    });
                
                    //layer associato agli utenti in macchina
                    carUserLayer = new ol.layer.Vector({
                        source: carUserSource,
                        style: new ol.style.Style({
                            image: new ol.style.Circle({
                                radius: 6,
                                fill: new ol.style.Fill({
                                    color: 'blue',
                                }),
                            }),
                        }),
                    });
                
                    // Aggiungo il layer alla mappa
                    map.addLayer(carUserLayer);
                    userVisualization = "CAR";
                })
                .catch(error => {
                    console.error("Errore:", error);
                });
        }
        else if (userVisualization=="ALL"){
            //se la visualizzazione è impostata a tutti gli utenti (sia a piedi che in macchina)
            fetch("http://localhost:5001/get_all_user_data", {
                method: "GET"
                })
                .then(response => response.json())
                .then(data => {
                    //rimuovo i layer presenti
                    map.removeLayer(walkingUserLayer);
                    map.removeLayer(carUserLayer);
                    map.removeLayer(allUserLayer);
                    map.removeLayer(clusterLayer);
                    
                    //inizializzo le feature associate alla posizione con i dati ottenuti dal backend
                    var allUserFeatures = data.map(item => {
                        const coordinates = item.geometry.coordinates;
                        const point = new ol.Feature({
                            geometry: new ol.geom.Point(ol.proj.fromLonLat(coordinates)),
                        });
                        return point;
                    });
                    
                
                    // Source associata alle feature
                    allUserSource = new ol.source.Vector({
                        features: allUserFeatures
                    });
                
                    //layer associato a tutti gli utenti
                    allUserLayer = new ol.layer.Vector({
                        source: allUserSource,
                        style: new ol.style.Style({
                            image: new ol.style.Circle({
                                radius: 6,
                                fill: new ol.style.Fill({
                                    color: 'blue',
                                }),
                            }),
                        }),
                    });
                
                    // Aggiungo il layer
                    map.addLayer(allUserLayer);
                    userVisualization = "ALL";
                })
                .catch(error => {
                    console.error("Errore:", error);
                });
        }
        //se la visualizzazione è impostata a tutti gli utenti (sia a piedi che in macchina)
        else if (userVisualization=="CLUSTER"){
            //aggiorno il layer per metterlo in primo piano
            map.removeLayer(clusterLayer);
            map.addLayer(clusterLayer);
        }
    }


    //funzione che carica i server presenti
    function loadServer(){
            fetch("http://localhost:5001/get_server", {
            method: "GET"
            })
            .then(response => response.json())
            .then(data => {
                map.removeLayer(serverLayer);

                // inizializzo le feature associate alle posizioni dei server ottenute dal backend
                var serverFeatures = data.map(item => {
                    const coordinates = item.geometry.coordinates;
                    const point = new ol.Feature({
                        geometry: new ol.geom.Point(ol.proj.fromLonLat(coordinates)),
                    });
                    return point;
                });
            
                // Source associata alle feature
                serverSource = new ol.source.Vector({
                    features: serverFeatures,
                });
                
                //layer associato ai server
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


    //funzione che carica i geofence presenti
    function loadGeofence(){
        fetch("http://localhost:5001/get_geofence", {
            method: "GET"
        })
        .then(response => response.json())
        .then(data => {
            //rimuovo lo stesso layer coi dati precedenti
            map.removeLayer(geofenceLayer);
            var polygonsFeatures = [];                                                 //array di features (punti)
            data.forEach(function(obj) {                                               //per ogni geofence che arriva
                var id = obj.id;                                                       //prendo l'id del geofence
                var n_users = obj.n_users;                                             //prendo il numero di utenti al suo interno
                var title = obj.title;                                                 //prendo il titolo del geofence
                var points = obj.points;                                               //prendo i punti che lo compongono
                var coordinates = points.map(function(record) {                        //per ogni coordinata del punto
                    return ol.proj.fromLonLat(record.geometry.coordinates);             //la converto in formato richiesto dal frontend e le memorizzo
                });
                
                var polygon = new ol.geom.Polygon([coordinates]);                       //creo un poligono con le coordinate
                var singlePolygonFeature = new ol.Feature(polygon);                     //creo una feature a partire dal poligono

                //prendo il colore e lo normalizzo, poi definisco il fillColor (colore del geofence). VALORE 6=NUMERO MASSIMO DI UTENTI, DA MODIFICARE IN SEGUITO
                var colorNormalization = n_users/6;
                var fillColor = 'rgba('+255*colorNormalization+','+255*(1-colorNormalization)+', 0, 0.2)'
                
                // feature associato a ogni poligono, setto il colore di riempimento di ogni singola feature
                singlePolygonFeature.setStyle(new ol.style.Style({
                    fill: new ol.style.Fill({
                        color: fillColor
                    }),
                    stroke: new ol.style.Stroke({
                        color: 'grey', // Colore del bordo
                        width: 1
                    })
                }));

                singlePolygonFeature.set('id', id);                                                     //associo l'id alla feature
                singlePolygonFeature.set('n_users', n_users);                                           //associo il numeto utenti alla feature
                singlePolygonFeature.set('title', title);                                               //associo il titolo alla feature

                //pusho ogni singola feature (poligono) personalizzata in un array di features
                polygonsFeatures.push(singlePolygonFeature);
            });


            //creo il layer con i vari poligoni personalizzati
            geofenceLayer = new ol.layer.Vector({
                source: new ol.source.Vector({
                    features: polygonsFeatures
                })
            });

            //aggiungo il layer, aggiorno la posizione degli utenti
            map.addLayer(geofenceLayer);
            updateUsersLocation(); 
        })
        .catch(error => {
            console.error("Errore:", error);
        });
    }

    //funzione per eliminare il geofence, prende in input l'id del geofence
    function deleteGeofence(s){
        //associo la stringa dell'id all'identificatore da passare al backend
        var queryString = 'id_allarme='+s;

        // mando al backend
        fetch("http://localhost:5001/delete_geofence", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: queryString
        })
        .then(response => response.json())
        .then(data => {
            console.log(data);
        })
        .catch(error => {
            console.error("Errore:", error);
        });
    }

    //quando viene aggiunto un geofence
    document.getElementById("addGeofenceForm").addEventListener("submit", function(event) {
        event.preventDefault(); // impedisce alla pagina di ricaricarsi quando viene inviato il modulo

        var formData = new FormData(this); // Creo un oggetto FormData con i dati associati nel form (testo allarme e coordinate)

        //definisco le proiezioni. Sul web si lavora con EPSG:3857 ma devo convertirlo a EPSG:4326 per lavorare con le nostre coordinate note
        var projection3857 = ol.proj.get('EPSG:3857');
        var projection4326 = ol.proj.get('EPSG:4326');

        //per ogni punto selezionato che definisce i punti della feature
        selectedPoints.forEach(function(feature) {

            // Ottengo le coordinate della feature e le converto
            var coordinates = ol.proj.transform(feature.getGeometry().getCoordinates(), projection3857, projection4326);
            
            // Aggiungo le coordinate alla FormData
            formData.append('coordinates[]', coordinates);
            
        });
        
        var inputDataTitolo = document.getElementById("inputDataTitolo");                           //prendo riferimento al titolo
        var inputDataAllarme1 = document.getElementById("inputDataAllarme1");                       //prendo riferimento all'allarme 1
        var inputDataAllarme2 = document.getElementById("inputDataAllarme2");                       //prendo riferimento all'allarme 2
        var inputDataAllarme3 = document.getElementById("inputDataAllarme3");                       //prendo riferimento all'allarme 3
        var labelError = document.getElementById("labelError1");                                    //prendo riferimento alla label di errore

        //controllo che i vari campi vengano riempiti correttamente
        if(inputDataTitolo.value===""){
            labelError.textContent = "Errore, inserisci titolo dell'allarme!";
        }
        else if(inputDataAllarme1.value==="" || inputDataAllarme2.value===""|| inputDataAllarme3.value===""){
            labelError.textContent = "Errore, inserisci l'allarme per tutte le zone!";
        }
        else if(selectedPoints.getLength()<3){
            labelError.textContent = "Errore, seleziona almeno 3 punti per creare l'allarme!";
        }
        else{
            // chiamo il backend
            fetch("http://localhost:5001/add_geofence", {
                method: "POST",
                body: formData
            })
            .then(response => response.json())
            .then(data => {
                selectedPoints.clear();                                 //tolgo i punti selezionati
                inputDataTitolo.value = "";                             //resetto le label
                inputDataAllarme1.value = "";                           //resetto le label 
                inputDataAllarme2.value = "";                           //resetto le label
                inputDataAllarme3.value = "";                           //resetto le label    
                labelError.textContent = "";                            //resetto le label
                loadGeofence();                                         //richiamo il caricamento del geofence per caricare anche questo nuovo
            })
            .catch(error => {
                console.error("Errore:", error);
            });
        }
        

    });

    //quando viene cliccato il pulsante per eliminare il geofence
    document.getElementById("deleteGeofenceForm").addEventListener("submit", function(event) {
        event.preventDefault();                                                                  //evito la ricarica della pagina
        deleteGeofence(geofence_idLabel.textContent);                                              //richiamo la funzione passando l'id del geofence associato
        geofence_idLabel.textContent = "-";                                                     //pulisco le label
        geofence_nUsersLabel.textContent = "-";                                                 //pulisco le label
        geofence_titleLabel.textContent = "-";                                                  //pulisco le label
        featureToDelete = null;                                                               //riporto la feature da cancellare a null
        document.getElementById("deleteButton").setAttribute("disabled", "true");             //disabilito il pulsante per cancellare
    });

    //quando viene cliccato il pulsante per vedere gli utenti a piedi
    document.getElementById("viewWalkingUserButton").addEventListener("click", function() {
        userVisualization = "WALKING";                                                          //imposto la visualizzazione richiesta
        updateUsersLocation();                                                                  //richiamo la funzione
        document.getElementById("viewAllUserButton").classList.remove("btn_select");
        document.getElementById("viewCarUserButton").classList.remove("btn_select");
        document.getElementById("viewWalkingUserButton").classList.add("btn_select");
    });

    //quando viene cliccato il pulsante per vedere gli utenti in macchina
    document.getElementById("viewCarUserButton").addEventListener("click", function() {
        userVisualization = "CAR";                                                              //imposto la visualizzazione richiesta
        updateUsersLocation();                                                                  //richiamo la funzione
        document.getElementById("viewAllUserButton").classList.remove("btn_select");
        document.getElementById("viewWalkingUserButton").classList.remove("btn_select");
        document.getElementById("viewCarUserButton").classList.add("btn_select");
    });

    //quando viene cliccato il pulsante per vedere tutti gli utenti
    document.getElementById("viewAllUserButton").addEventListener("click", function() {
        userVisualization = "ALL";                                                              //imposto la visualizzazione richiesta
        updateUsersLocation();                                                                  //richiamo la funzione
        document.getElementById("viewWalkingUserButton").classList.remove("btn_select");
        document.getElementById("viewCarUserButton").classList.remove("btn_select");
        document.getElementById("viewAllUserButton").classList.add("btn_select");
    });


    //quando viene cliccato il pulsante per richiedere il cluster
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

                    //rimuovo i layer presenti
                    map.removeLayer(allUserLayer);
                    map.removeLayer(carUserLayer);
                    map.removeLayer(walkingUserLayer);
                    map.removeLayer(clusterLayer);
                    
                    //creo un array dei colori associati a ogni feature e lo inizializzo tutto a -1
                    var colorArray= new Array(100).fill("-1");
                    
                    //definisco le feature associata al cluster
                    var clusterFeatures = jsonData.map(item => {
                        //associo le coordinate
                        const coordinates = [item.longitudine, item.latitudine];            
                        
                        //creo un punto associato alle coordinate
                        const point = new ol.Feature({
                            geometry: new ol.geom.Point(ol.proj.fromLonLat(coordinates)),
                        });

                        //ottengo il cluster id
                        var cluster_id = item.CLUSTER_ID;

                        //se il cluster id è uguale a -1 = non è ancora stato associato un colore a quel cluster id
                        if(colorArray[cluster_id]==="-1"){
                            //associo un colore casuale a quel cluster
                            fillColor = 'rgba('+Math.floor(Math.random() * 256)+','+Math.floor(Math.random() * 256)+','+Math.floor(Math.random() * 256)+', 0.9)'
                            colorArray[cluster_id] = fillColor;
                        }
                        
                        //setto lo stile del punto associandogli il colore presente nell'array
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
                
                    // source associato alle feature
                    clusterSource = new ol.source.Vector({
                        features: clusterFeatures
                    });
                    
                    //layer associato ai cluster
                    clusterLayer = new ol.layer.Vector({
                        source: clusterSource
                    });
                

                    // Aggiungo il layer
                    map.addLayer(clusterLayer);
                    userVisualization = "CLUSTER";
                    document.getElementById("viewWalkingUserButton").classList.remove("btn_select");
                    document.getElementById("viewCarUserButton").classList.remove("btn_select");
                    document.getElementById("viewAllUserButton").classList.remove("btn_select");
                    document.getElementById("inputDatatoCluster").value="";
                })
                .catch(error => {
                    console.error("Errore:", error);
                });
    });


    //quando viene cliccato il pulsante per richiedere il calcolo del best_k col metodo elbow
    document.getElementById("viewElbowCluster").addEventListener("click", function() {
        fetch("http://localhost:5001/get_cluster_elbow", {
                method: "GET"
                })
                .then(response => response.json())
                .then(data => {
                    inputData = document.getElementById("inputDatatoCluster");              //prendo il riferimento alla textbox dove inserire il numero di cluster
                    inputData.value = data.best_k.toString();                               //aggiorno la label inserendo il best_k
                })
                .catch(error => {
                    console.error("Errore:", error);
                });
    });


    //funzione che aggiorna la visualizzazione per le varie modalità del menù a destra
    function updateMode(show, hidden1, hidden2){
        document.getElementById(hidden1).classList.add("hidden");
        document.getElementById(hidden2).classList.add("hidden");
        document.getElementById(show).classList.remove("hidden");

        document.getElementById(hidden1+"-mode").classList.remove("btn-outline-success");
        document.getElementById(hidden1+"-mode").classList.add("btn-sm");
        document.getElementById(hidden1+"-mode").classList.add("btn-outline-secondary");
        document.getElementById(hidden2+"-mode").classList.remove("btn-outline-success");
        document.getElementById(hidden2+"-mode").classList.add("btn-sm");
        document.getElementById(hidden2+"-mode").classList.add("btn-outline-secondary");

        document.getElementById(show+"-mode").classList.remove("btn-sm");
        document.getElementById(show+"-mode").classList.remove("btn-outline-secondary");
        document.getElementById(show+"-mode").classList.add("btn-outline-success");
    }



    document.getElementById("addgeofence-mode").addEventListener("click", function() {   
        updateMode("addgeofence", "cluster", "delete");
        addmode = 1;
        deletemode=0;
    });

    document.getElementById("cluster-mode").addEventListener("click", function() {
        updateMode("cluster", "addgeofence", "delete");
        selectedPoints.clear();
        addmode = 0;
        deletemode = 0;
    });


    document.getElementById("delete-mode").addEventListener("click", function() {
        updateMode("delete", "cluster", "addgeofence");
        selectedPoints.clear();
        addmode = 0;
        deletemode=1;
    });

});