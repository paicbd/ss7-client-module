package com.paicbd.module.ss7.layer.api.network;


import com.paicbd.smsc.exception.RTException;

public interface ILayer {

    String getName();

    void start() throws RTException;

    void stop();

}
