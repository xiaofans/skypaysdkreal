package com.jpgk.hardwaresdk.utils;


import com.jpgk.hardwaresdk.hardwarelogger.VendingMachineLogger;

public class DLogger {

    public static void log(String tag,String message){
        VendingMachineLogger logger =   null;//App.Companion.getInstance().getLogger();
        if (logger != null){
            logger.log("PAY",tag,message);
        }
    }

    public static void log(String message){
        VendingMachineLogger logger =   null;//App.Companion.getInstance().getLogger();
        if (logger != null){
            logger.log("PAY","PAY",message);
        }
    }

}
