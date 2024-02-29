import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class TimerData(
    @SerializedName("status") var status: String,
    @SerializedName("timeLeft") var timeLeft: Long
) : Serializable

