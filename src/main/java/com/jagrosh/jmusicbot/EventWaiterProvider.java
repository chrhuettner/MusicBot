package com.jagrosh.jmusicbot;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

public class EventWaiterProvider {
    private static EventWaiter eventWaiter;

    public static EventWaiter getInstance(){
        if(eventWaiter == null){
            eventWaiter = new EventWaiter();
        }
        return eventWaiter;
    }
}
