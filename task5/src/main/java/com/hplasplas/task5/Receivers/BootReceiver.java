package com.hplasplas.task5.Receivers;

import com.hplasplas.task5.Services.NotificationService;
import com.starsoft.longRuningReceverUtil.Receivers.LongRunningReceiver;

/**
 * Created by StarkinDG on 27.02.2017.
 */

public class BootReceiver extends LongRunningReceiver {
    
    @Override
    public Class getServiceClass() {

        return NotificationService.class;
    }
}