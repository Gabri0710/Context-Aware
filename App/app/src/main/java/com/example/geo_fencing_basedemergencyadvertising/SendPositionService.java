package com.example.geo_fencing_basedemergencyadvertising;
import android.util.Log;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SendPositionService {
    @POST("/upload_location")
    Call<Void> uploadLocation(@Body LocationData locationData);

}
