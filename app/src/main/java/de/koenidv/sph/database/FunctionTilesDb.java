package de.koenidv.sph.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import de.koenidv.sph.objects.FunctionTile;

public class FunctionTilesDb {

    private final DatabaseHelper dbhelper = DatabaseHelper.getInstance();

    private static FunctionTilesDb instance;

    private FunctionTilesDb() {
    }

    public static FunctionTilesDb getInstance() {
        if (FunctionTilesDb.instance == null) {
            FunctionTilesDb.instance = new FunctionTilesDb();
        }
        return FunctionTilesDb.instance;
    }


    public List<FunctionTile> getAllFunctions() {
        return fromCursor(dbhelper.getReadableDatabase()
                .rawQuery("SELECT * FROM tiles", null));
    }

    /**
     * Get a List of Tiles with a specific type
     *
     * @param type Feature type to query for
     * @return List of all Tiles in the db with the given type
     */
    public List<FunctionTile> getFunctionsByType(String type) {
        return fromCursor(dbhelper.getReadableDatabase()
                .rawQuery("SELECT * FROM tiles WHERE type = '" + type + "'", null));
    }

    /**
     * Get all FunctionTiles where type is not other
     */
    public List<FunctionTile> getSupportedFeatures() {
        return fromCursor(dbhelper.getReadableDatabase()
                .rawQuery("SELECT * FROM tiles WHERE type IS NOT 'other'", null));
    }

    /**
     * Check if a feature tile is supported
     */
    public boolean supports(String function) {
        Cursor cursor = dbhelper.getReadableDatabase().rawQuery(
                "SELECT name FROM tiles where type = \"" + function + "\"", null);
        boolean supported = cursor.moveToFirst();
        cursor.close();
        return supported;
    }

    //duringupdate
    /**
     * Check if a feature with name parameter is available
     */
    public List<FunctionTile> getSupportedFeatureName(String nm) {
        return fromCursor(dbhelper.getReadableDatabase()
                .rawQuery("SELECT * FROM tiles WHERE name = \"" + nm + "\"", null));
    }
    //duringupdate end

    /**
     * Adds or updates feature functionTiles in the database
     * Will override everything with the same name if it's not null
     *
     * @param functionTiles List of functionTiles to be added or updated
     */
    public void save(List<FunctionTile> functionTiles) {
        for (FunctionTile functionTile : functionTiles) {
            save(functionTile);
        }
    }


    public void save(FunctionTile functionTile) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("name", functionTile.getName());
        cv.put("location", functionTile.getLocation());
        if (functionTile.getType() != null) cv.put("type", functionTile.getType());
        cv.put("color", functionTile.getColor());
        cv.put("icon", functionTile.getIcon());

        Cursor cursor = db.rawQuery("SELECT * FROM tiles WHERE name = '" + functionTile.getName() + "'", null);
        if (cursor.getCount() == 0) {
            db.insert("tiles", null, cv);
        } else {
            db.update("tiles", cv, "name = '" + functionTile.getName() + "'", null);
        }
        cursor.close();
    }

    /**
     * Get a list of FunctionTiles from a Cursor
     */
    private List<FunctionTile> fromCursor(Cursor cursor) {
        List<FunctionTile> returnList = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                String location = cursor.getString(1);
                String tiletype = cursor.getString(2);
                String icon = cursor.getString(3);
                int color = cursor.getInt(4);

                FunctionTile newFunctionTile = new FunctionTile(name, location, tiletype, icon, color);

                returnList.add(newFunctionTile);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return returnList;
    }


}
