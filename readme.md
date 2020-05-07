The common libraries in stream function.

- [how to use](#how-to-use)
- [Thread pool](#thread-pool)
- [Promise](#promise)
- [Net](#net)

## How to use

```js
import cn.brainpoint.febs;

Net.fetch(...)
```


### config

```js
// Initial with thread pool config.
Febs.init(new Febs.ThreadPoolCfg(
                        2, 
                        4, 
                        20000, 
                        new ArrayBlockingQueue<>(20),
                        new ThreadPoolExecutor.AbortPolicy())
         );
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

- `.then` 或 `.fail` 语句块中可以不返回数据或返回任意类型的对象, 返回的内容将被下一个链条捕获为参数.

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
promise.then(res->{  })
       .fail(e->{  })  // same as javascript catch()
       .finish(()->{}) // same as javascript finally()
       .execute();  // execute promise.

/**
 * 触发异步操作后, 如果当前线程会销毁, 则需join等待.
 */
Promise.join(promise);
```

### return promise.

```js
promise.then(res->{
            // this nest promise cannot to call execute().
            return new Promise((resolve, reject)->{
                ...
            });
        })
        .then(res->{
        });
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
       .then(res->{
        })
       .execute();

/**
 * 触发异步操作后, 如果当前线程会销毁, 则需join等待.
 */
Promise.join(promise);
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
promise.then((Integer res)->{ 
            // ...
        })
       .execute();  // execute promise.
```

### Uncaught Exception Handler

未设置fail的promise, 如果发生异常, 将由此处理异常.

```js
Promise.setUncaughtExceptionHandler(e->{
  // handle error.
});
```

## Net

使用fetch的方式进行网络请求

### Get text content.

```js
Net.fetch("https://xxxx")
        // get text content.
        .then(res->{ return res.text(); })
        // print content.
        .then(res->{
            System.out.print(res);
        })
        // If exception cause.
        .fail((e)->{
            System.err.print(e.getMessage());
        })
        .execute();
```

### Get binary content.

```js
Net.fetch("https://xxxx")
        // get blob content.
        .then(res->{ return res.blob(); })
        // print content.
        .then((res)->{
            BufferedReader in = (BufferedReader)res;
            char buf[] = new char[1024];

            while (in.read(buf, 0, buf.length) != -1) {
                System.out.printf("%s",  Arrays.toString(buf));
                Arrays.fill(buf, '\0');
            }

            // important to call close().
            in.close();
        })
        // If exception cause.
        .fail((e)->{
            System.err.print(e.getMessage());
        })
        .execute();
```


### Get response headers.

```js
Net.fetch("https://xxxx")
        // get response status code.
        .then(res->{
            // code.
            System.out.print(res.statusCode);
            // message.
            System.out.print(res.statusMsg);
            
            return res;
        })
        // get response headers.
        .then(res->{
            Set<String> keySet = res.headers.keySet();
            Iterator<String> it1 = keySet.iterator();
            while(it1.hasNext()){
                String Key = it1.next();
                System.out.print("header: " + Key);
                List<String> values = res.headers.get(Key);
                System.out.print(values);
            }
        })
        // If exception cause.
        .fail((e)->{
            System.err.print(e.getMessage());
        })
        .execute();
```