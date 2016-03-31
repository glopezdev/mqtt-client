package com.amchealth.callback;

public abstract class Event<S> {
  public abstract void onEmit(S... data);
}