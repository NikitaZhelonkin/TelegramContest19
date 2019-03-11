package ru.zhelonkin.tgcontest.model.deserializer;

import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Line;
import ru.zhelonkin.tgcontest.model.PointL;

public class GraphDeserializer implements JsonDeserializer<Graph> {


    @Override
    public Graph deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Gson gson = new Gson();
        JsonObject jsonObject = json.getAsJsonObject();
        JsonElement columns = jsonObject.get("columns");
        JsonElement types = jsonObject.get("types");
        JsonElement names = jsonObject.get("names");
        JsonElement colors = jsonObject.get("colors");

        JsonArray columnsArray = columns.getAsJsonArray();

        Map<String, List<Long>> columnMap = new HashMap<>();

        for (int i = 0; i < columnsArray.size(); i++) {
            JsonArray values = columnsArray.get(i).getAsJsonArray();
            List<Long> valuesList = new ArrayList<>(values.size() - 1);
            String name = values.get(0).getAsString();
            for (int j = 1; j < values.size(); j++) {
                valuesList.add(values.get(j).getAsLong());
            }
            columnMap.put(name, valuesList);
        }

        Type mapType = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> typeMap = gson.fromJson(types, mapType);
        Map<String, String> namesMap = gson.fromJson(names, mapType);
        Map<String, String> colorsMap = gson.fromJson(colors, mapType);


        List<Line> lines = new ArrayList<>();
        List<Long> xValues = columnMap.get("x");

        for (String key : typeMap.keySet()) {
            if ("line".equals(typeMap.get(key))) {
                List<Long> yValues = columnMap.get(key);
                String name = namesMap.get(key);
                String color = colorsMap.get(key);

                if (xValues != null && yValues != null && name != null && color != null) {
                    lines.add(new Line(mergeAsPoints(xValues, yValues), name, Color.parseColor(color)));
                } else {
                    Log.e(GraphDeserializer.class.getSimpleName(), "Invalid graph data");
                }
            }
        }
        return new Graph(lines);
    }

    private PointL[] mergeAsPoints(List<Long> xValues, List<Long> yValues) {
        if (xValues.size() != yValues.size())
            throw new JsonParseException("x and y values have different sizes");

        PointL[] points = new PointL[xValues.size()];
        for (int i = 0; i < xValues.size(); i++) {
            points[i] = new PointL(xValues.get(i), yValues.get(i));
        }
        return points;
    }
}
