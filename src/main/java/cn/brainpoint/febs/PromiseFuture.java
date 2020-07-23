/**
* Copyright (c) 2020 Copyright bp All Rights Reserved.
* Author: brian.li
* Date: 2020-07-23 14:46
* Desc: 
*/
package cn.brainpoint.febs;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import cn.brainpoint.febs.exception.FebsRuntimeException;

public class PromiseFuture implements Future<Object> {
  private Promise<?> promisePri;
  private Lock lock;
  private Condition condition;

  public <T> PromiseFuture(Promise<T> promisePri, Lock lock, Condition condition) {
    this.promisePri = promisePri;
    this.lock = lock;
    this.condition = condition;
  }

  /**
   * PromiseFuture cannot be cancel.
   * 
   * @param mayInterruptIfRunning this value has no effect in this implementation
   *                              because interrupts are not used to control
   *                              processing.
   * 
   * @return {@code true} if this task is now cancelled
   */
  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    return Promise.STATUS_FULFILLED.equals(this.promisePri.getStatus());
  }

  @Override
  public Object get() throws ExecutionException {

    if (Promise.STATUS_FULFILLED.equals(this.promisePri.getStatus())) {
      Object ret = this.promisePri.getResult();
      return ret;
    }

    lock.lock();
    try {
      while (!Promise.STATUS_REJECTED.equals(this.promisePri.getStatus())) {
        condition.await();
        break;
      }

      Object ret = this.promisePri.getResult();
      if (ret instanceof Exception) {
        throw new ExecutionException((Exception) ret);
      }
      return ret;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new FebsRuntimeException(e);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Object get(long timeout, TimeUnit unit) throws ExecutionException, TimeoutException {

    if (Promise.STATUS_FULFILLED.equals(this.promisePri.getStatus())) {
      return this.promisePri.getResult();
    }

    lock.lock();
    try {
      boolean isNoTimeout = false;
      while (!Promise.STATUS_REJECTED.equals(this.promisePri.getStatus())) {
        isNoTimeout = condition.await(timeout, unit);
        break;
      }

      if (!isNoTimeout) {
        throw new TimeoutException("get promise result timeout: " + unit.toMillis(timeout) + "ms");
      } else {
        Object ret = this.promisePri.getResult();
        if (ret instanceof Exception) {
          throw new ExecutionException((Exception) ret);
        }
        return ret;
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new FebsRuntimeException(e);
    } finally {
      lock.unlock();
    }
  }
}