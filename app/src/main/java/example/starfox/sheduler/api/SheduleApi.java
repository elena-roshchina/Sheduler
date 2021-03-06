package example.starfox.sheduler.api;

import java.util.List;

import example.starfox.sheduler.ScheduleModel;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SheduleApi {
    @GET("/get/sched.php")
    Call<List<ScheduleModel>> getData(@Query("session") String session);
}
