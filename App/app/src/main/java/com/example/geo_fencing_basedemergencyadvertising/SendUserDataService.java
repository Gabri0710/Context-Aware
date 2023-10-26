package com.example.geo_fencing_basedemergencyadvertising;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SendUserDataService {
    //invio posizione al backend
    @POST("/upload_location")
    Call<Void> uploadData(@Body UserData userData);

}
