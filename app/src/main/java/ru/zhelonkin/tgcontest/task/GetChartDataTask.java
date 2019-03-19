package ru.zhelonkin.tgcontest.task;

import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import ru.zhelonkin.tgcontest.model.ChartData;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Result;
import ru.zhelonkin.tgcontest.model.deserializer.GraphParser;

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
            List<Graph> graphs = parseJSON(mAssetManager.open("chart_data.json"));
            return new Result<>(new ChartData(graphs));
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


    private List<Graph> parseJSON(InputStream is) throws IOException, JSONException {
        GraphParser graphParser = new GraphParser();
        JSONArray array = new JSONArray(readToString(is));
        List<Graph> result = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            result.add(graphParser.parse(array.getJSONObject(i)));
        }
        return result;
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
