package awais.instagrabber.repositories.serializers;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import awais.instagrabber.repositories.responses.Caption;

public class CaptionDeserializer implements JsonDeserializer<Caption> {

    private static final String TAG = CaptionDeserializer.class.getSimpleName();

    @Override
    public Caption deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        final Caption caption = new Gson().fromJson(json, Caption.class);
        final JsonObject jsonObject = json.getAsJsonObject();
        if (jsonObject.has("pk")) {
            JsonElement elem = jsonObject.get("pk");
            if (elem != null && !elem.isJsonNull()) {
                if (!elem.isJsonPrimitive()) return caption;
                String pkString = elem.getAsString();
                if (pkString.contains("_")) {
                    pkString = pkString.substring(0, pkString.indexOf("_"));
                }
                try {
                    caption.setPk(pkString);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "deserialize: ", e);
                }
            }
        }
        return caption;
    }
}
