package ru.zhelonkin.tgcontest.formatter;

import java.util.HashMap;
import java.util.Map;

public class CachingFormatter implements Formatter {

    private Map<Long, String> mCache = new HashMap<>();

    private Formatter mDelegate;

    public CachingFormatter(Formatter formatter){
        mDelegate = formatter;
    }
    @Override
    public String format(long value) {
        if(mCache.containsKey(value)){
            return mCache.get(value);
        }
        String result = mDelegate.format(value);
        mCache.put(value, result);
        return result;
    }
}
