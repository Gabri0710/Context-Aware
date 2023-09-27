package com.example.geo_fencing_basedemergencyadvertising;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SendUserDataService {
    @POST("/upload_location")
    Call<Void> uploadData(@Body UserData userData);

}
