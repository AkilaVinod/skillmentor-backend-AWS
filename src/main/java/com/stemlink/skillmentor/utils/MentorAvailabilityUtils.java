package com.stemlink.skillmentor.utils;

import com.stemlink.skillmentor.entities.Mentor;
import com.stemlink.skillmentor.entities.MentorAvailability;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class MentorAvailabilityUtils {

    private MentorAvailabilityUtils() {
    }

    public static List<MentorAvailability> buildDefaultWeekdayAvailability(Mentor mentor) {
        List<MentorAvailability> slots = new ArrayList<>();

        for (DayOfWeek day : List.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
        )) {
            MentorAvailability availability = new MentorAvailability();
            availability.setMentor(mentor);
            availability.setDayOfWeek(day);
            availability.setStartTime(LocalTime.of(9, 0));
            availability.setEndTime(LocalTime.of(17, 0));
            slots.add(availability);
        }

        return slots;
    }
}
