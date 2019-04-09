package ru.zhelonkin.tgcontest.task;

import android.content.res.AssetManager;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.zhelonkin.tgcontest.model.Chart;
import ru.zhelonkin.tgcontest.model.ChartData;
import ru.zhelonkin.tgcontest.model.Result;
import ru.zhelonkin.tgcontest.model.deserializer.ChartParser;

public class GetChartDataTask extends AsyncTask<Void, Void, Result<ChartData>> {

    public interface Callback {
        void onSuccess(ChartData chartData);

        void onError(Throwable t);
    }

    private AssetManager mAssetManager;

    private Callback mCallback;

    public GetChartDataTask(AssetManager assetManager, Callback callback) {
        mAssetManager = assetManager;
        mCallback = callback;
    }

    public void unsubscribe() {
        mCallback = null;
    }

    @Override
    protected Result<ChartData> doInBackground(Void... voids) {
        try {
            List<Chart> charts = new ArrayList<>();
            charts.addAll(parseJSON(mAssetManager.open("overview_1.json")));
            charts.addAll(parseJSON(mAssetManager.open("overview_2.json")));
            charts.addAll(parseJSON(mAssetManager.open("overview_3.json")));
            charts.addAll(parseJSON(mAssetManager.open("overview_4.json")));
            charts.addAll(parseJSON(mAssetManager.open("overview_5.json")));
            return new Result<>(new ChartData(charts));
        } catch (IOException | JSONException e) {
            return new Result<>(e);
        }
    }

    @Override
    protected void onPostExecute(Result<ChartData> result) {
        if (mCallback == null) return;
        if (result.isSuccess()) {
            mCallback.onSuccess(result.getData());
        } else {
            mCallback.onError(result.getError());
        }
    }


    private List<Chart> parseJSON(InputStream is) throws IOException, JSONException {
        String data = readToString(is);
        ChartParser chartParser = new ChartParser();
        JSONArray array = null;
        try{
            array = new JSONArray(data);
        }catch (JSONException e){
            //do nothing
        }
        if(array !=null){
            List<Chart> result = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) {
                result.add(chartParser.parse(array.getJSONObject(i)));
            }
            return result;
        }
        return Collections.singletonList(chartParser.parse(new JSONObject(data)));


    }

    private String readToString(InputStream is) throws IOException {
        BufferedReader reader = null;
        try{
            reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder total = new StringBuilder();
            for (String line; (line = reader.readLine()) != null; ) {
                total.append(line).append('\n');
            }
            return total.toString();
        }finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //do nothing
                }
            }
        }

    }

}
