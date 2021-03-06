package client;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by lenovo on 2017/11/13.
 */

public class MyRetroFitClient {
    private static final int DEFAULT_TIMEOUT = 20;
    private  ApiService apiService;
    private static OkHttpClient okHttpClient;
    public static String baseUrl = API.BASE_PATH;
    private static Context mContext;

    private static Retrofit retrofit;
    private Cache cache = null;
    private File httpCacheDirectory;

    private static OkHttpClient.Builder httpClient =
            new OkHttpClient.Builder()
                    .addNetworkInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS))
                    .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
    private static Class<? extends Class> aClass;
    private static Retrofit.Builder builder =
            new Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .baseUrl(baseUrl);


    //默认baseUrl的单例
    private static class SingletonHolder {
        private static MyRetroFitClient INSTANCE = new MyRetroFitClient(
                mContext);
    }
    public static MyRetroFitClient getInstance(Context context) {
        if (context != null) {
            mContext = context;
        }
        return SingletonHolder.INSTANCE;
    }

    //自己提供url
    public static MyRetroFitClient getInstance(Context context, String url) {
        if (context != null) {
            mContext = context;
        }

        return new MyRetroFitClient(context, url);
    }

    //不仅有url还有headers
    public static MyRetroFitClient getInstance(Context context, String url, Map<String, String> headers) {
        if (context != null) {
            mContext = context;
        }
        return new MyRetroFitClient(context, url, headers);
    }


    private MyRetroFitClient(Context context) {

        this(context, baseUrl, null);
    }

    private MyRetroFitClient(Context context, String url) {

        this(context, url, null);
    }

    private MyRetroFitClient(Context context, String url, Map<String, String> headers) {
        //url为空，则默认使用baseUrl
        if (TextUtils.isEmpty(url)) {
            url = baseUrl;
        }
        //缓存地址
        if (httpCacheDirectory == null) {
            httpCacheDirectory = new File(mContext.getCacheDir(), "app_cache");
        }

        try {
            if (cache == null) {
                cache = new Cache(httpCacheDirectory, 10 * 1024 * 1024);
            }
        } catch (Exception e) {
            Log.e("OKHttp", "Could not create http cache", e);
        }
        //okhttp创建了
        okHttpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(
                        new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .cache(cache)
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .build();
        //retrofit创建了
        retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(okHttpClient)
                .baseUrl(url)
                .build();

    }
    //处理线程调度的变换
    ObservableTransformer schedulersTransformer = new ObservableTransformer() {
        @Override
        public ObservableSource apply(Observable upstream) {
            return ((Observable) upstream).subscribeOn(Schedulers.io())
                    .unsubscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
        }
    };

    public  void gets(String url,Observer<?> observer) {
        apiService.getHome()
                .compose(schedulersTransformer)
                .subscribe(observer);
    }

    /**
     * 创建默认url的api类
     * @return ApiManager
     */
    public MyRetroFitClient createBaseApi() {
        apiService = create(ApiService.class);
        return this;
    }

    /**
     * create you ApiService
     * Create an implementation of the API endpoints defined by the {@code service} interface.
     */
    public  <T> T create(final Class<T> service) {
        if (service == null) {
            throw new RuntimeException("Api service is null!");
        }
        return retrofit.create(service);
    }
}
