package com.wanfadger.AdministrativeareaApi.shared.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;

public interface AdministrativeAreaDatePattern {
    DateTimeFormatter datePattern = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    DateTimeFormatter dateTimePattern = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss");
    DateTimeFormatter timePattern = DateTimeFormatter.ofPattern("hh:mm:ss");
    DateTimeFormatter HourMinutePattern = DateTimeFormatter.ofPattern("hh:mm");
    DateTimeFormatter dateTimePattern24 = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    DateTimeFormatter timePattern24 = DateTimeFormatter.ofPattern("HH:mm:ss");
    DateTimeFormatter HourMinutePattern24 = DateTimeFormatter.ofPattern("HH:mm");

    Predicate<LocalDate> NotWeekendPredicate = (weekendDate) -> !(weekendDate.getDayOfWeek().equals(DayOfWeek.SATURDAY) || weekendDate.getDayOfWeek().equals(DayOfWeek.SUNDAY));

}
