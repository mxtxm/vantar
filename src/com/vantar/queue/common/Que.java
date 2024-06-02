package com.vantar.queue.common;

import com.vantar.queue.rabbit.Rabbit;
import com.vantar.service.Services;


public class Que {

    public enum Engine implements Services.DataSources  {
        RABBIT,
    }

    public static Rabbit rabbit;
}
