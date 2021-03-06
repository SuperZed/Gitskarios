package core.repository;

import core.datasource.CacheDataSource;
import core.datasource.CloudDataSource;
import core.datasource.SdkItem;
import rx.Observable;

public class GenericRepository<Request, Data> {
  private static final String TAG = GenericRepository.class.getSimpleName();
  private CacheDataSource<Request, Data> cache;
  private CloudDataSource<Request, Data> cloud;

  public GenericRepository(CacheDataSource<Request, Data> cache, CloudDataSource<Request, Data> cloud) {
    this.cache = cache;
    this.cloud = cloud;
  }

  public Observable<SdkItem<Data>> execute(final SdkItem<Request> request) {
    return execute(request, false);
  }

  public Observable<SdkItem<Data>> execute(final SdkItem<Request> request, boolean refresh) {
    checkRequest(request);
    Observable<SdkItem<Data>> cacheObs = Observable.empty();
    Observable<SdkItem<Data>> cloudObs = Observable.empty();
    if (cache != null) {
      cacheObs = cache.getData(request);
    }
    if (cacheObs == null) {
      cacheObs = Observable.empty();
    }
    if (cloud != null) {
      cloudObs = cloud.execute(request);
    }
    if (cloudObs == null) {
      cloudObs = Observable.empty();
    }
    cloudObs = cloudObs.onErrorResumeNext(fallbackApi() != null ? fallbackApi() : Observable.empty()).doOnNext(dataSdkItem -> {
      if (cache != null) {
        cache.saveData(request, dataSdkItem);
      }
    });
    if (refresh) {
      return cloudObs;
    } else {
      return Observable.concat(cacheObs, cloudObs).first();
    }
  }

  public void save(final Request request, Data dataSdkItem) {
    if (cache != null) {
      cache.saveData(new SdkItem<>(request), new SdkItem<>(dataSdkItem));
    }
  }

  private void checkRequest(SdkItem<Request> request) {
    if (request == null) {
      throw new IllegalArgumentException(TAG + " SdkItem<Request> request item could not be null");
    }
  }

  protected Observable<SdkItem<Data>> fallbackApi() {
    return null;
  }
}
