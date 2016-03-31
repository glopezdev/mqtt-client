package com.amchealth.callback;

public abstract class Callback<E,S> {
  public abstract void call(E error, S... args);
}