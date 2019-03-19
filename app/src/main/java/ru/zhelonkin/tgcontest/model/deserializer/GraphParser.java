package ru.zhelonkin.tgcontest.model.deserializer;

import android.graphics.Color;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Line;
import ru.zhelonkin.tgcontest.model.PointL;

public class GraphParser {

    public Graph parse(JSONObject jsonObject) throws JSONException {
        JSONArray columns = jsonObject.getJSONArray("columns");
        JSONObject types = jsonObject.getJSONObject("types");
        JSONObject names = jsonObject.getJSONObject("names");
        JSONObject colors = jsonObject.getJSONObject("colors");

        Map<String, List<Long>> columnMap = new HashMap<>();

        for (int i = 0; i < columns.length(); i++) {
            JSONArray values = columns.getJSONArray(i);
            List<Long> valuesList = new ArrayList<>(values.length() - 1);
            String name = values.getString(0);
            for (int j = 1; j < values.length(); j++) {
                valuesList.add(values.getLong(j));
            }
            columnMap.put(name, valuesList);
        }

        Map<String, String> typeMap = parseAsMap(types);
        Map<String, String> namesMap = parseAsMap(names);
        Map<String, String> colorsMap = parseAsMap(colors);

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
                    Log.e(GraphParser.class.getSimpleName(), "Invalid graph data");
                }
            }
        }
        return new Graph(lines);

    }

    private Map<String, String> parseAsMap(JSONObject object) {
        Iterator<String> keys = object.keys();
        HashMap<String, String> map = new HashMap<>();
        while (keys.hasNext()) {
            String key = keys.next();
            map.put(key, object.optString(key));
        }
        return map;
    }

    private PointL[] mergeAsPoints(List<Long> xValues, List<Long> yValues) throws JSONException {
        if (xValues.size() != yValues.size())
            throw new JSONException("x and y values have different sizes");

        PointL[] points = new PointL[xValues.size()];
        for (int i = 0; i < xValues.size(); i++) {
            points[i] = new PointL(xValues.get(i), yValues.get(i));
        }
        return points;
    }
}
