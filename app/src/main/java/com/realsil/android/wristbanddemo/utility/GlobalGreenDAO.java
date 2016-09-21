package com.realsil.android.wristbanddemo.utility;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.realsil.android.wristbanddemo.constant.ConstantParam;
import com.realsil.android.wristbanddemo.greendao.DaoMaster;
import com.realsil.android.wristbanddemo.greendao.DaoMaster.OpenHelper;
import com.realsil.android.wristbanddemo.greendao.DaoSession;
import com.realsil.android.wristbanddemo.greendao.SleepData;
import com.realsil.android.wristbanddemo.greendao.SleepDataDao;
import com.realsil.android.wristbanddemo.greendao.SportData;
import com.realsil.android.wristbanddemo.greendao.SportDataDao;

import de.greenrobot.dao.query.QueryBuilder;

public class GlobalGreenDAO {
	// Log
	private static final String TAG = GlobalGreenDAO.class.getSimpleName(); 
	private static final boolean D = true;
	
	// object
    private static GlobalGreenDAO mInstance;
    private static Context mAppContext;
    
	// data base object
	private static DaoMaster mDaoMaster;  
    private static DaoSession mDaoSession; 
    private static SQLiteDatabase mSqlDb;
    private static String DB_NAME = "wristband-db";

    // table object
    private SportDataDao mSportDataDao;
    private SleepDataDao mSleepDataDao;
    
    
	/** 
     * get DaoMaster 
     *  
     * @param context 
     * @return 
     */  
    public static DaoMaster getDaoMaster(Context context) {  
        if (mDaoMaster == null) {  
            OpenHelper helper = new DaoMaster.DevOpenHelper(context, DB_NAME, null);  
            mDaoMaster = new DaoMaster(helper.getWritableDatabase());  
        }  
        return mDaoMaster;  
    }  
      
    /** 
     * get DaoSession 
     *  
     * @param context 
     * @return 
     */  
    public static DaoSession getDaoSession(Context context) {  
        if (mDaoSession == null) {  
            if (mDaoMaster == null) {  
            	mDaoMaster = getDaoMaster(context);  
            }  
            mDaoSession = mDaoMaster.newSession();  
        }  
        return mDaoSession;  
    }  
    
    public static SQLiteDatabase getSQLDatebase(Context context) {  
	    if (mSqlDb == null) {  
	        if (mDaoMaster == null) {  
	        	mDaoMaster = getDaoMaster(context);  
	        }  
	        mSqlDb = mDaoMaster.getDatabase();  
	    }  
        return mSqlDb;     
    } 
    public static SQLiteDatabase getSQLDatebase() { 
        return mSqlDb;     
    } 
    
    // Global Green Dao only create one times
    public static void initial(Context context) {
    	if (mInstance == null) {
        	mInstance = new GlobalGreenDAO();  
        	if (mAppContext == null){  
        		mAppContext = context.getApplicationContext();  
        	}  
        	mInstance.mSqlDb = getSQLDatebase(context);
        	mInstance.mDaoSession = getDaoSession(context);
        	
        	mInstance.mSportDataDao = mInstance.mDaoSession.getSportDataDao();
        	mInstance.mSleepDataDao = mInstance.mDaoSession.getSleepDataDao();
        } 
    }
    
    public static GlobalGreenDAO getInstance() { 
        return mInstance;  
    }
	
	/**
	 * drop sport data table
	 * */
	public void dropSportTable() {
		mSportDataDao.dropTable(mDaoSession.getDatabase(), true);
	}
	
	/**
	 * drop sleep data table
	 * */
	public void dropSleepTable() {
		mSleepDataDao.dropTable(mDaoSession.getDatabase(), true);
	}

	
	/**
	 * drop all table
	 * */
	public void dropAllTable() {
		SportDataDao.dropTable(mDaoSession.getDatabase(), true);
        SleepDataDao.dropTable(mDaoSession.getDatabase(), true);
	}
	/**
	 * create all table
	 * */
    public void createAllTable() {
    	SportDataDao.createTable(mDaoSession.getDatabase(), true);
    	SleepDataDao.createTable(mDaoSession.getDatabase(), true);
	}
    
    
    /*
     * Sport Data Operator
     * */
    /** 
     * insert or update sport data 
     * @param sportData SportData object
     * @return insert or update
     */ 
    public long saveSportData(SportData sportData){
        if(D) Log.d(TAG, "saveSportData");
        Long id = getSportDataIdByDateAndOffset(sportData.getYear(), sportData.getMonth(), sportData.getDay(), sportData.getOffset());
        if(id != -1) {
            sportData.setId(id);
            if(D) Log.w(TAG, "have same offset date, only update.");
        }

        return mSportDataDao.insertOrReplace(sportData);
    }  

    /** 
     * load all sport data 
     * @return all the sport data in table
     */ 
    public List<SportData> loadAllSportData() {
        List<SportData> sports = new ArrayList<SportData>();
        List<SportData> tmpSports = mSportDataDao.loadAll();
        int len = tmpSports.size();
        for (int i = len-1; i >=0; i--) {
        	sports.add(tmpSports.get(i));
        }
        return sports;
    }  
    /** 
     * load sport data by id
     * @return the sport data in table
     */ 
    public SportData loadSportData(long id) {
        return mSportDataDao.load(id);
    } 

    /** 
     * delete note 
     * @param sportData SportData object
     * @return insert or update
     */ 
    public void deleteSportData(SportData sportData) {
    	mSportDataDao.delete(sportData);
    }
    /**
     * delete all sport data
     */
    public void deleteAllSportData(){
        mSportDataDao.deleteAll();
    }

    /**
     * load special sport data by date
     *
     *
     * @return all the sport data in table
     */
    public List<SportData> loadSportDataByDate(int y, int m) {
        //List<SportData> sports = new ArrayList<SportData>();
        String yearColName = SportDataDao.Properties.Year.columnName;
        String monthColName = SportDataDao.Properties.Month.columnName;
        String dayColName = SportDataDao.Properties.Day.columnName;
        String dateColName = SportDataDao.Properties.Date.columnName;
        // Add offset check
        String offsetName = SportDataDao.Properties.Offset.columnName;

        String whereStr = "where " + yearColName + "=?"
                + " and " + monthColName + "=?"
                + " and " + offsetName + ">=?"
                + " and " + offsetName + "<=?";
        String orderBy = "ORDER BY " + dateColName + " ASC";
        String[] selectionArg = {String.valueOf(y)
                , String.valueOf(m)
                , String.valueOf(0)
                , String.valueOf(95)};
        List<SportData> tmpSports = mSportDataDao.queryRaw(whereStr + " " + orderBy, selectionArg);

        return tmpSports;
    }


    /**
     * load special sport data by date
     *
     *
     * @return all the sport data in table
     */
    public List<SportData> loadSportDataByDate(int y, int m, int d) {
        //List<SportData> sports = new ArrayList<SportData>();
        String yearColName = SportDataDao.Properties.Year.columnName;
        String monthColName = SportDataDao.Properties.Month.columnName;
        String dayColName = SportDataDao.Properties.Day.columnName;
        String dateColName = SportDataDao.Properties.Date.columnName;
        // Add offset check
        String offsetName = SportDataDao.Properties.Offset.columnName;

        String whereStr = "where " + yearColName + "=?"
                + " and " + monthColName + "=?"
                + " and " + dayColName + "=?"
                + " and " + offsetName + ">=?"
                + " and " + offsetName + "<=?";
        String orderBy = "ORDER BY " + dateColName + " ASC";
        String[] selectionArg = {String.valueOf(y)
                , String.valueOf(m)
                , String.valueOf(d)
                , String.valueOf(0)
                , String.valueOf(95)};
        List<SportData> tmpSports = mSportDataDao.queryRaw(whereStr + " " + orderBy, selectionArg);

        return tmpSports;
    }

    /**
     * load special sport data by date
     *
     *
     * @return all the sport data in table
     */
    public Long getSportDataIdByDateAndOffset(int y, int m, int d, int offset) {
        //List<SportData> sports = new ArrayList<SportData>();
        String yearColName = SportDataDao.Properties.Year.columnName;
        String monthColName = SportDataDao.Properties.Month.columnName;
        String dayColName = SportDataDao.Properties.Day.columnName;
        // Add offset check
        String offsetName = SportDataDao.Properties.Offset.columnName;

        String whereStr = "where " + yearColName + "=?"
                + " and " + monthColName + "=?"
                + " and " + dayColName + "=?"
                + " and " + offsetName + "=?";
        String[] selectionArg = {String.valueOf(y)
                , String.valueOf(m)
                , String.valueOf(d)
                , String.valueOf(offset)};
        List<SportData> tmpSports = mSportDataDao.queryRaw(whereStr, selectionArg);

        if(tmpSports == null
                || tmpSports.size() == 0) {
            return -1L;
        }

        return tmpSports.get(0).getId();
    }

    /**
     * load special sleep data by date
     * This method is use to calculate the 18:00 PM - 10:00 AM
     *
     *
     * @return all the sleep data in table
     */
    public List<SportData> loadSportDataByDate(Date date) {
        QueryBuilder qb = mSportDataDao.queryBuilder();
        qb.where(SportDataDao.Properties.Date.ge(date));
        qb.orderAsc(SportDataDao.Properties.Date);
        List<SportData> tmpSports = qb.list();

        return tmpSports;
    }

    /*
     * Sleep Data Operator
     * */
    
    /** 
     * insert or update sleep data 
     * @param sleepData SleepData object
     * @return insert or update
     */ 
    public long saveSleepData(SleepData sleepData){
        if(D) Log.d(TAG, "saveSleepData");

        Long id = getSleepDataIdByDateAndMintues(sleepData.getYear(), sleepData.getMonth(), sleepData.getDay(), sleepData.getMinutes());
        if(id != -1) {
            sleepData.setId(id);
            if(D) Log.w(TAG, "have same offset date, only update.");
        }

        return mSleepDataDao.insertOrReplace(sleepData);  
    }  

    /** 
     * load all sleep data 
     * @return all the sleep data in table
     */ 
    public List<SleepData> loadAllSleepData() {
        List<SleepData> sleeps = new ArrayList<SleepData>();
        List<SleepData> tmpSleeps = mSleepDataDao.loadAll();
        int len = tmpSleeps.size();
        for (int i = len-1; i >=0; i--) {
        	sleeps.add(tmpSleeps.get(i));
        }
        return sleeps;
    }  
    /** 
     * load sleep data by id
     * @return the sleep data in table
     */ 
    public SleepData loadSleepData(long id) {
        return mSleepDataDao.load(id);
    } 

    /** 
     * delete note 
     * @param sleepData SleepData object
     * @return insert or update
     */ 
    public void deleteSleepData(SleepData sleepData) {
    	mSleepDataDao.delete(sleepData);
    }
    /**
     * delete all note
     * @return insert or update
     */
    public void deleteAllSleepData() {
        mSleepDataDao.deleteAll();
    }

    /**
     * load special sleep data by date
     * This method is use to calculate the 18:00 PM - 10:00 AM
     *
     *
     * @return all the sleep data in table
     */
    public List<SleepData> loadSleepDataByDateSpec(int y, int m, int d) {
        Calendar c1 = Calendar.getInstance();
        c1.set(y, m - 1, d);// here need decrease 1 of month
        c1.add(Calendar.DATE, -1);
        int yesterdayYear = c1.get(Calendar.YEAR);
        int yesterdayMonth = c1.get(Calendar.MONTH) + 1;
        int yesterdayDay = c1.get(Calendar.DATE);
        if(D) Log.d(TAG, "loadSleepDataByDateSpec, y: " + y + ", m: " + m + ", d: " + d
                + ", yesterdayYear: " + yesterdayYear + ", yesterdayMonth: " + yesterdayMonth
                + ", yesterdayDay: " + yesterdayDay);

        //List<SleepData> sleeps = new ArrayList<SleepData>();
        String yearColName = SleepDataDao.Properties.Year.columnName;
        String monthColName = SleepDataDao.Properties.Month.columnName;
        String dayColName = SleepDataDao.Properties.Day.columnName;
        String dateColName = SleepDataDao.Properties.Date.columnName;

        String whereStr = "where (" + yearColName + "=?"
                + " and " + monthColName + "=?"
                + " and " + dayColName + "=?)"
                + " or (" + yearColName + "=?"
                + " and " + monthColName + "=?"
                + " and " + dayColName + "=?)";
        String orderBy = "ORDER BY " + dateColName + " ASC";
        String[] selectionArg = {String.valueOf(y)
                , String.valueOf(m)
                , String.valueOf(d)
                , String.valueOf(yesterdayYear)
                , String.valueOf(yesterdayMonth)
                , String.valueOf(yesterdayDay)};
        List<SleepData> tmpSleeps = mSleepDataDao.queryRaw(whereStr + " " + orderBy, selectionArg);

        return tmpSleeps;
    }

    /**
     * load special sleep data by date
     * This method is use to calculate the 18:00 PM - 10:00 AM
     *
     *
     * @return all the sleep data in table
     */
    public List<SleepData> loadSleepDataByDate(Date date) {
        QueryBuilder qb = mSleepDataDao.queryBuilder();
        qb.where(SleepDataDao.Properties.Date.ge(date));
        qb.orderAsc(SleepDataDao.Properties.Date);
        List<SleepData> tmpSleeps = qb.list();

        return tmpSleeps;
    }

    /**
     * load special sleep data by date
     *
     *
     * @return all the sleep data in table
     */
    public List<SleepData> loadSleepDataByDate(int y, int m, int d) {
        //List<SleepData> sleeps = new ArrayList<SleepData>();
        String yearColName = SleepDataDao.Properties.Year.columnName;
        String monthColName = SleepDataDao.Properties.Month.columnName;
        String dayColName = SleepDataDao.Properties.Day.columnName;
        String dateColName = SleepDataDao.Properties.Date.columnName;

        String whereStr = "where " + yearColName + "=?"
                + " and " + monthColName + "=?"
                + " and " + dayColName + "=?";
        String orderBy = "ORDER BY " + dateColName + " ASC";
        String[] selectionArg = {String.valueOf(y)
                , String.valueOf(m)
                , String.valueOf(d)};
        List<SleepData> tmpSleeps = mSleepDataDao.queryRaw(whereStr + " " + orderBy, selectionArg);

        return tmpSleeps;
    }

    /**
     * load special sleep data by date
     *
     *
     * @return all the sleep data in table
     */
    public Long getSleepDataIdByDateAndMintues(int y, int m, int d, int min) {
        //List<SleepData> sleeps = new ArrayList<SleepData>();
        String yearColName = SleepDataDao.Properties.Year.columnName;
        String monthColName = SleepDataDao.Properties.Month.columnName;
        String dayColName = SleepDataDao.Properties.Day.columnName;
        String minutesColName = SleepDataDao.Properties.Minutes.columnName;

        String whereStr = "where " + yearColName + "=?"
                + " and " + monthColName + "=?"
                + " and " + dayColName + "=?"
                + " and " + minutesColName + "=?";
        String[] selectionArg = {String.valueOf(y)
                , String.valueOf(m)
                , String.valueOf(d)
                , String.valueOf(min)};
        List<SleepData> tmpSleeps = mSleepDataDao.queryRaw(whereStr, selectionArg);

        if(tmpSleeps == null
                || tmpSleeps.size() == 0) {
            return -1L;
        }

        return tmpSleeps.get(0).getId();
    }

    /**
     * load special sleep data by date
     *
     *
     * @return all the sleepdata in table
     */
    public List<SleepData> loadSleepDataByDate(int y, int m) {
        //List<SleepData> sleeps = new ArrayList<SleepData>();
        String yearColName = SleepDataDao.Properties.Year.columnName;
        String monthColName = SleepDataDao.Properties.Month.columnName;
        String dayColName = SleepDataDao.Properties.Day.columnName;
        String dateColName = SleepDataDao.Properties.Date.columnName;

        String whereStr = "where " + yearColName + "=?"
                + " and " + monthColName + "=?";
        String orderBy = "ORDER BY " + dateColName + " ASC";
        String[] selectionArg = {String.valueOf(y)
                , String.valueOf(m)};
        List<SleepData> tmpSleeps = mSleepDataDao.queryRaw(whereStr + " " + orderBy, selectionArg);

        return tmpSleeps;
    }
}
