
# Febs

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/cn.brainpoint/febs/badge.svg)](https://maven-badges.herokuapp.com/maven-central/cn.brainpoint/febs/)
[![License](https://img.shields.io/github/license/brainpoint/febs-java)](https://opensource.org/licenses/MIT)

Febs is a common libraries in fluent API. Most api is like javascript.

- [How to use](#how-to-use)
- [Asynchronous in ThreadPool](#Asynchronous-in-ThreadPool)
- [Asynchronous in Promise](#Asynchronous-in-Promise)
- [Network transfer in Fetch](#Network-transfer-in-Fetch)
- [Utilities](#Utilities)

## How to use

maven config.

```html
<dependency>
    <groupId>cn.brainpoint</groupId>
    <artifactId>febs</artifactId>
    <version>0.0.3</version>
</dependency>
```


```js
import cn.brainpoint.febs;

Febs.Net.fetch("https://xxx")
    // get response status code.
    .then(res->{
        // code.
        System.out.print(res.statusCode);
        // message.
        System.out.print(res.statusMsg);
        
        // get text content.
        return res.text();
    })
    .then(content->{

    });
```


### config

It can initial with thread pool config. Thread pool will affect performance of promise.

```js
// Initial with thread pool config.
Febs.init(new Febs.ThreadPoolCfg(
                        2, 
                        4, 
                        20000, 
                        new LinkedBlockingQueue<>(),
                        new ThreadPoolExecutor.AbortPolicy())
         );
```

## Asynchronous in ThreadPool

Use `getExecutorService` api to get a asynchronous work item.

```js
try {
    Future<Object> future 
                    = Febs.getExecutorService.submit(()->{
                        // do anything in this thread.
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

## Asynchronous in Promise

Febs promise like javascript promise api, use chain list way to do asynchronous work.

- `.then`: same as js-es6 promise`.then` chain.
- `.fail`: same as js-es6 promise`.catch` chain.
- `.finish`: same as js promise`.finally` chain.
- `.execute`: It must be call to activate promise in Febs promise.

### Base scene

```js
/**
 * Make a promise object.
 */
Promise promise = new Promise((IResolve resolve, IReject reject)-> { 

                                // call this set status to 'fulfilled'
                                resolve.execute(retVal); 

                                // call this set status to 'rejected'
                                reject.execute(new Exception(""));
                            });

/**
 * chain.
 */
promise.then(res->{  })
       .then(()->{ return 1; })
       .then(res1->{  })
       .fail(e->{  })  // same as javascript catch()
       .finish(()->{}) // same as javascript finally()
       .execute();  // activate promise.

/**
 * Block until promise finish, if you want to wait.
 */
Promise.join(promise);
```

### return another promise object in chain.

```js
promise.then(res->{
            // this nest promise cannot call execute().
            return new Promise((resolve, reject)->{
                ...
            });
        })
        .then(res->{
        })
        .execute();
```

### all

```js
/**
 * Promise object array.
 * !Warning: All promise object cannot call execute() funciton.
 */
Promise[] promiseArr = {...};

/**
 * execute all promise object.
 */
Promise promise = Promise.all(promiseArr)
       .then(res->{
            // all promise done.
        })
        .fail(e->{
            // if some promise rejected.
        })
       .execute();
```

### join

It will store promise object in global until promise finish, after promise object is created. We can call Promise.join to wait promise finish.

```js
IPromise promiseObj = new Promise((resolve, reject)->{ resolve.execute(); });
Promise.join(promiseObj);
```

### template

The `then` and `fail` chain can return a object to next chain. The data type of return value is unkonw, we can use template to spacify a data type.

e.g.

```js
// Spacify a data type.
Promise<Integer> promise = new Promise<Integer>((IResolve<Integer> resolve, IReject reject)-> { 
                                resolve.execute(2); 
                            });

// use the data type.
promise.then((Integer res)->{ 
            // ...
        })
       .execute();  // execute promise.
```

### Uncaught Exception Handler

Some promise object will catch exception use this method, if it have't call `.fail()`

```js
Promise.setUncaughtExceptionHandler(e->{
  // handle error.
});
```

## Network transfer in Fetch

The network transfer in fetch style

### Get text content.

```js
import cn.brainpoint.febs;

Febs.Net.fetch("https://xxxx")
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
import cn.brainpoint.febs;

Febs.Net.fetch("https://xxxx")
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

> IMPORTANT: close BufferedReader.


### Get response headers.

```js
import cn.brainpoint.febs;

Febs.Net.fetch("https://xxxx")
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

### Set request parameter 

```js
import cn.brainpoint.febs;

Febs.Net.fetch(new Requset(
                    url,
                    body,
                    method,
                    headers,
                    timeout,
                ))
        // get blob content.
        .then(res->{ return res.blob(); })
        .execute();
```

## Utilities

### sleep

Use `sleep` API to schedule tasks.

```js
import cn.brainpoint.febs;

Febs.Utils.sleep(1000)
        .then(()->{
            System.out.print("after 1000ms.");
        })
        .execute();


Febs.Utils.sleep(1000)
        .then(res->{
            System.out.print("after 1000ms.");
            return Febs.Utils.sleep(2000);
        })
        .then(res->{
            System.out.print("after 2000ms.");
        })
        .execute();
```
