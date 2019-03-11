package ru.zhelonkin.tgcontest.task;

import android.content.res.AssetManager;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.List;

import ru.zhelonkin.tgcontest.model.ChartData;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Result;
import ru.zhelonkin.tgcontest.model.deserializer.GraphDeserializer;

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
        Reader reader = null;
        try {
            reader = new InputStreamReader(mAssetManager.open("chart_data.json"));
            Gson gson = new Gson().newBuilder().registerTypeAdapter(Graph.class, new GraphDeserializer()).create();
            Type type = new TypeToken<List<Graph>>() {}.getType();
            List<Graph> graphs = gson.fromJson(reader, type);
            return new Result<>(new ChartData(graphs));
        } catch (IOException e) {
            return new Result<>(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //do nothing
                }
            }
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


}
