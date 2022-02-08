package de.koenidv.sph.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.koenidv.sph.objects.Holiday;

//  Extended by StKl DEC-2022
public class HolidaysDb {
    SQLiteDatabase writable = DatabaseHelper.getInstance().getWritableDatabase();

    /**
     * save holiday to database
     * will replace existing with the same id
     *
     * @param holiday holiday to save
     */
    public void save(Holiday holiday) {
        ContentValues cv = new ContentValues();

        cv.put("id", holiday.getId());
        cv.put("start", holiday.getStart().getTime() / 1000);
        cv.put("endtime", holiday.getEnd().getTime() / 1000);
        cv.put("name", holiday.getName());
        cv.put("year", holiday.getYear());

        writable.replace("holidays", null, cv);
    }

    /**
     * get list of future holidays
     *
     * @return List<Holiday>
     */
    public List<Holiday> getFuture() {
        return getFromCursor(writable.rawQuery(
                "SELECT * FROM holidays WHERE start > " + new Date().getTime() / 1000 + " ORDER BY start ASC",
                null));
    }

    /**
     * Get the next holiday entry
     */
    public Holiday getNext(Date dt) {
        List<Holiday> nextHoliday = getFromCursor(writable.rawQuery(
                "SELECT * FROM holidays WHERE start > " + /*new Date()*/dt.getTime() / 1000
                        + " LIMIT 1", null));
        if (nextHoliday.isEmpty()) return null;
        else return nextHoliday.get(0);
    }

    /**
     * Get holiday entry in case we are in vacation at the moment
     */
    public Holiday getCurrent(Date dt) {
        //Example: SELECT * FROM Customers WHERE Country='Mexico' AND City='México D.F.' LIMIT 1;
        List<Holiday> currentHoliday = getFromCursor(writable.rawQuery(
                "SELECT * FROM holidays WHERE start <= " + /*new Date()*/dt.getTime() / 1000
                        + " AND endtime >= " + /*new Date()*/dt.getTime() / 1000 + " LIMIT 1", null));
        if (currentHoliday.isEmpty()) return null;
        else return currentHoliday.get(0);
    }

    /**
     * get list of holidays from cursor
     *
     * @param cursor Cursor pointing to a holidays table
     * @return List<Holiday>
     */
    private List<Holiday> getFromCursor(Cursor cursor) {
        ArrayList<Holiday> returnList = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                returnList.add(new Holiday(
                        cursor.getString(0),
                        new Date(cursor.getLong(1) * 1000),
                        new Date(cursor.getLong(2) * 1000),
                        cursor.getString(3),
                        cursor.getString(4)
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return returnList;
    }
}
