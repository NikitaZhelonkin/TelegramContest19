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

import ru.zhelonkin.tgcontest.model.Chart;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Point;

public class ChartParser {

    public Chart parse(JSONObject jsonObject) throws JSONException {
        JSONArray columns = jsonObject.getJSONArray("columns");
        JSONObject types = jsonObject.getJSONObject("types");
        JSONObject names = jsonObject.getJSONObject("names");
        JSONObject colors = jsonObject.getJSONObject("colors");

        Map<String, List<Long>> columnMap = new HashMap<>();

        List<String> columnsArray = new ArrayList<>();

        for (int i = 0; i < columns.length(); i++) {
            JSONArray values = columns.getJSONArray(i);
            List<Long> valuesList = new ArrayList<>(values.length() - 1);
            String name = values.getString(0);
            for (int j = 1; j < values.length(); j++) {
                valuesList.add(values.getLong(j));
            }
            columnMap.put(name, valuesList);
            columnsArray.add(name);
        }

        Map<String, String> typeMap = parseAsMap(types);
        Map<String, String> namesMap = parseAsMap(names);
        Map<String, String> colorsMap = parseAsMap(colors);

        List<Graph> graphs = new ArrayList<>();
        List<Long> xValues = columnMap.get("x");

        for(String column:columnsArray){
            String type = typeMap.get(column);
            if (!"x".equals(type)) {
                List<Long> yValues = columnMap.get(column);
                String name = namesMap.get(column);
                String color = colorsMap.get(column);

                if (xValues != null && yValues != null && name != null && color != null) {
                    graphs.add(new Graph(mergeAsPoints(xValues, yValues), type, name, Color.parseColor(color)));
                } else {
                    Log.e(ChartParser.class.getSimpleName(), "Invalid graph data");
                }
            }
        }
        boolean yScaled = jsonObject.optBoolean("y_scaled");
        boolean stacked = jsonObject.optBoolean("stacked");
        boolean percentage = jsonObject.optBoolean("percentage");
        return new Chart(graphs, xValues, yScaled, stacked, percentage);

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

    private Point[] mergeAsPoints(List<Long> xValues, List<Long> yValues) throws JSONException {
        if (xValues.size() != yValues.size())
            throw new JSONException("x and y values have different sizes");

        Point[] points = new Point[xValues.size()];
        for (int i = 0; i < xValues.size(); i++) {
            points[i] = new Point(xValues.get(i), yValues.get(i));
        }
        return points;
    }
}
