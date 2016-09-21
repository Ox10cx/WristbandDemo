import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Schema;

/**
 * Reference: http://www.aiuxian.com/article/p-2323437.html
 *			  http://www.cnblogs.com/lee0oo0/p/3483604.html
 *			  http://www.jcodecraeer.com/a/anzhuokaifa/androidkaifa/2014/1127/2069.html
 * */

public class main {
    private static final int DATA_BASE_VERSION = 1;
    private static final String GENERATE_PACKAGE_NAME = "com.realsil.android.wristbanddemo.greendao";
    private static final String GENERATE_PATH = ".\\app\\src\\main\\java-gen";


    public static void main(String[] args) throws Exception {
        // Database version, package name
        Schema schema = new Schema(DATA_BASE_VERSION, GENERATE_PACKAGE_NAME);

        // add table
        addSportData(schema);
        addSleepData(schema);

        // Create class
        new DaoGenerator().generateAll(schema, GENERATE_PATH);
    }

    /**
     * add a sport data table
     * @param schema
     */
    private static void addSportData(Schema schema) {
        Entity sportData = schema.addEntity("SportData");

        // set column
        sportData.addIdProperty().autoincrement();
        sportData.addIntProperty("year").notNull();
        sportData.addIntProperty("month").notNull();
        sportData.addIntProperty("day").notNull();
        sportData.addIntProperty("offset").notNull();
        sportData.addIntProperty("mode").notNull();
        sportData.addIntProperty("stepCount").notNull();
        sportData.addIntProperty("activeTime").notNull();
        sportData.addIntProperty("calory").notNull();
        sportData.addIntProperty("distance").notNull();

        sportData.addDateProperty("date");
    }

    /**
     * add a sleep data table
     * @param schema
     */
    private static void addSleepData(Schema schema) {
        Entity sleepData = schema.addEntity("SleepData");

        // set column
        sleepData.addIdProperty().autoincrement();
        sleepData.addIntProperty("year").notNull();
        sleepData.addIntProperty("month").notNull();
        sleepData.addIntProperty("day").notNull();
        sleepData.addIntProperty("minutes").notNull();
        sleepData.addIntProperty("mode").notNull();

        sleepData.addDateProperty("date");
    }
}