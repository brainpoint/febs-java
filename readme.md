The common libraries.

- [how to use](#how-to-use)
- [Promise](#promise)
- [Thread pool](#thread-pool)
- [Net](#net)

## How to use

```js
import cn.brainpoint.febs;

// can to init.
Febs.init();
```


## Thread pool

可以在初始化时传入对应的线程池参数, 进行线程池构造. 此后可以按如下方式使用线程池.

```js
try {
    Future<Object> future 
                    = Febs.getExecutorService.submit(()->{
                        return "any";
                    });
    Object result = future.get();
} catch (ExecutionException e) {
    e.printStackTrace();
} catch (InterruptedException e) {
    e.printStackTrace();
} catch (Exception e) {
    e.printStackTrace();
}
```

## Promise

与js es6中promise一样的使用方式. 内部使用了线程池, 使用异步操作进行效率.

```js
/**
 * 创建promise对象.
 */
Promise promise = new Promise((IResolve resolve, IReject reject)-> { 
                                resolve.execute(...); 
                            });

/**
 * 使用; then/fail 操作必须返回一个值
 * 在所有的链完成后, 调用execute执行promise.
 */
promise.then(res->{ return null; })
       .fail(e->{ return null; })  // same as javascript catch()
       .finish(()->{}) // same as javascript finally()
       .execute();  // execute promise.

/**
 * 触发异步操作后, 如果当前线程会销毁, 则需join等待.
 */
Promise.join(promise);
```

### all
```js
/**
 * 创建promise数组.
 */
Promise[] promiseArr = {...};

/**
 * 执行.
 */
Promise promise = Promise.all(promiseArr)
       .then(res->{ return null; })
       .execute();

/**
 * 触发异步操作后, 如果当前线程会销毁, 则需join等待.
 */
Promise.join(promise);
```

### Uncaught Exception Handler

未设置fail的promise, 如果发生异常, 将由此处理异常.

```js
Promise.setUncaughtExceptionHandler(e->{
  // handle error.
});
```


### join

promise创建之后, 会自动在全局保存一份实例直到异步完成. 但是如果需要手动等待异步完成, 可以调用join接口.

```js
Promise.join(promise);
```

### template

可以使用模板的方式,指定第一次then的参数类型

```js
/**
 * 创建promise对象.
 */
Promise<Integer> promise = new Promise<Integer>((IResolve<Integer> resolve, IReject reject)-> { 
                                resolve.execute(2); 
                            });

/**
 * 第一个then的参数类型为指定的模板类型.
 */
promise.then((Integer res)->{ return null; })
       .execute();  // execute promise.
```

## Net

使用fetch的方式进行网络请求

```js
Net.fetch(new Request(...))
   .then(Response->{...})
   .execute();
```

