package com.example.al.cameramarkersoverlay;

public interface ObservableWarning {
    void registerObserver(ObserverWarning o);
    void removeObserver(ObserverWarning o);
    void notifyObservers(boolean visible);
}
