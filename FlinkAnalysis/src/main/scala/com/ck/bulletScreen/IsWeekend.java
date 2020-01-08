package com.ck.bulletScreen;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class IsWeekend {
    public static Boolean isWeekend(String bDate) throws ParseException {
        DateFormat format1 = new SimpleDateFormat("yyyy/MM/dd");
        Date bdate = format1.parse(bDate);
        Calendar cal = Calendar.getInstance();
        cal.setTime(bdate);
        return  (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY);
    }
}