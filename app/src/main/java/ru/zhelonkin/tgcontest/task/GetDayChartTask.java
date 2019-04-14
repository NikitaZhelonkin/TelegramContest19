package ru.zhelonkin.tgcontest.task;

import android.content.res.AssetManager;
import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import ru.zhelonkin.tgcontest.model.Chart;
import ru.zhelonkin.tgcontest.model.Result;
import ru.zhelonkin.tgcontest.model.deserializer.ChartParser;
import ru.zhelonkin.tgcontest.utils.IOUtils;

public class GetDayChartTask extends AsyncTask<Void, Void, Result<Chart>> {

    public interface Callback {
        void onSuccess(Chart chart);

        void onError(Throwable t);
    }

    private AssetManager mAssetManager;

    private GetDayChartTask.Callback mCallback;

    private String mPath;

    public GetDayChartTask(AssetManager assetManager,String path, GetDayChartTask.Callback callback) {
        mAssetManager = assetManager;
        mCallback = callback;
        mPath = path;
    }

    public void unsubscribe() {
        mCallback = null;
    }

    @Override
    protected Result<Chart> doInBackground(Void... voids) {
        try {
            return new Result<>(parseJSON (mAssetManager.open(mPath)));
        } catch (IOException | JSONException e) {
            return new Result<>(e);
        }
    }

    @Override
    protected void onPostExecute(Result<Chart> result) {
        if (mCallback == null) return;
        if (result.isSuccess()) {
            mCallback.onSuccess(result.getData());
        } else {
            mCallback.onError(result.getError());
        }
    }

    private Chart parseJSON(InputStream is) throws IOException, JSONException {
        String data = IOUtils.readToString(is);
        ChartParser chartParser = new ChartParser();
        return chartParser.parse(new JSONObject(data));
    }

}
