package seedu.address;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.stage.Stage;
import seedu.address.commons.core.Config;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.core.Version;
import seedu.address.commons.exceptions.DataConversionException;
import seedu.address.commons.util.ConfigUtil;
import seedu.address.commons.util.StringUtil;
import seedu.address.logic.Logic;
import seedu.address.logic.LogicManager;
import seedu.address.model.AddressBook;
import seedu.address.model.Calendar;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.ReadOnlyCalendar;
import seedu.address.model.ReadOnlyData;
import seedu.address.model.ReadOnlyUserList;
import seedu.address.model.ReadOnlyUserPrefs;
import seedu.address.model.UserPrefs;
import seedu.address.model.bio.UserList;
import seedu.address.model.record.UniqueRecordList;
import seedu.address.model.util.SampleDataUtil;
import seedu.address.model.util.SampleUserDataUtil;
import seedu.address.storage.AddressBookStorage;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonCalendarStorage;
import seedu.address.storage.JsonFoodListStorage;
import seedu.address.storage.JsonRecordListStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;
import seedu.address.storage.UserListStorage;
import seedu.address.storage.UserPrefsStorage;
import seedu.address.storage.bio.JsonUserListStorage;
import seedu.address.ui.Ui;
import seedu.address.ui.UiManager;
import seedu.sgm.model.food.UniqueFoodList;

/**
 * Runs the application.
 */
public class MainApp extends Application {

    public static final Version VERSION = new Version(0, 6, 0, true);

    private static final Logger logger = LogsCenter.getLogger(MainApp.class);

    private static final String LABEL_BIO_DATA_TYPE = "bio";
    private static final String LABEL_CALENDAR_DATA_TYPE = "calendar";
    private static final String LABEL_FOOD_DATA_TYPE = "food list";
    private static final String LABEL_RECORD_DATA_TYPE = "record list";

    protected Ui ui;
    protected Logic logic;
    protected Storage storage;
    protected Model model;
    protected Config config;

    @Override
    public void init() throws Exception {
        logger.info("=============================[ Initializing AddressBook ]===========================");
        super.init();

        AppParameters appParameters = AppParameters.parse(getParameters());
        config = initConfig(appParameters.getConfigPath());

        UserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(config.getUserPrefsFilePath());
        UserPrefs userPrefs = initPrefs(userPrefsStorage);
        AddressBookStorage addressBookStorage = new JsonAddressBookStorage(userPrefs.getAddressBookFilePath());
        UserListStorage userListStorage = new JsonUserListStorage(userPrefs.getUserListFilePath());
        JsonFoodListStorage jsonFoodListStorage = new JsonFoodListStorage(userPrefs.getFoodListFilePath());
        JsonRecordListStorage jsonRecordListStorage = new JsonRecordListStorage(userPrefs.getRecordListFilePath());
        JsonCalendarStorage jsonCalendarStorage = new JsonCalendarStorage(userPrefs.getEventListFilePath(),
                userPrefs.getReminderListFilePath());
        storage = new StorageManager(addressBookStorage, userPrefsStorage, userListStorage, jsonFoodListStorage,
                jsonRecordListStorage, jsonCalendarStorage);

        initLogging(config);

        Model model = initModelManager(userPrefs);
        this.model = model;
        logic = new LogicManager(this.model, storage);

        ui = new UiManager(logic);
    }

    /**
     * Returns a {@code ModelManager} with the data from {@code storage}'s address book and {@code userPrefs}. <br> The
     * data from the sample address book will be used instead if {@code storage}'s address book is not found, or an
     * empty address book will be used instead if errors occur when reading {@code storage}'s address book.
     */
    private Model initModelManager(ReadOnlyUserPrefs userPrefs) {
        ReadOnlyAddressBook initialData;
        ReadOnlyUserList initialUserData;
        UniqueFoodList foodList = new UniqueFoodList();
        foodList.setFoods(FOODS);
        UniqueRecordList initialRecordListData;
        ReadOnlyCalendar initialCalendar;

        initialData = (ReadOnlyAddressBook) getInitialData("Address Book",
                SampleDataUtil::getSampleAddressBook, AddressBook::new);
        initialUserData = (ReadOnlyUserList) getInitialData(LABEL_BIO_DATA_TYPE,
                SampleUserDataUtil::getSampleUserList, UserList::new);
        foodList = (UniqueFoodList) getInitialData(LABEL_FOOD_DATA_TYPE,
                SampleDataUtil::getSampleFoodList, UniqueFoodList::new);
        initialRecordListData = (UniqueRecordList) getInitialData(LABEL_RECORD_DATA_TYPE,
                SampleDataUtil::getSampleRecordList, UniqueRecordList::new);
        initialCalendar = (ReadOnlyCalendar) getInitialData(LABEL_CALENDAR_DATA_TYPE,
                SampleDataUtil::getSampleCalendar, Calendar::new);

        return new ModelManager(initialData, userPrefs, initialUserData, foodList, initialRecordListData,
                initialCalendar);
    }

    /**
     * Returns an optional containing read-only data types.
     * @param dataType String label of data type for which optional data is to be obtained.
     * @return Optional containing read-only data types.
     * @throws IOException
     * @throws DataConversionException
     */
    private Optional<? extends ReadOnlyData> getOptionalData(String dataType) throws IOException,
            DataConversionException {
        switch (dataType) {
        case LABEL_BIO_DATA_TYPE: return storage.readUserList();
        case LABEL_FOOD_DATA_TYPE: return storage.readFoodList();
        case LABEL_RECORD_DATA_TYPE: return storage.readRecordList();
        case LABEL_CALENDAR_DATA_TYPE: return storage.readCalendar();
        default: return storage.readAddressBook();
        }
    }

    /**
     * Returns an object representing data of the given data type.
     * @param dataType String representing the type of initial data to be retrieved
     * @param sampleDataSupplier Supplier that creates a new sample data file upon execution.
     * @param dataObjectSupplier Supplier that creates a new data file upon execution.
     * @return Object representing data of the given data type.
     */
    private Object getInitialData(String dataType, Supplier<? extends Object> sampleDataSupplier,
                                        Supplier<? extends Object> dataObjectSupplier) {
        Object initialData;
        try {
            Optional<? extends ReadOnlyData> dataOptional = getOptionalData(dataType);
            if (!dataOptional.isPresent()) {
                logger.info(capitaliseFirstLetter(dataType) + " data file not found. Will be starting a sample "
                        + dataType + " data file");
                initialData = sampleDataSupplier.get();
            } else {
                initialData = dataOptional.get();
            }
        } catch (DataConversionException e) {
            logger.warning(dataType + "data file not in the correct format. Will be starting with an empty "
                    + dataType + " data file");
            initialData = dataObjectSupplier.get();
        } catch (IOException e) {
            logger.warning("Bio Data file not in the correct format. Will be starting with an empty "
                    + "user list containing no bio data");
            initialData = dataObjectSupplier.get();
            logger.warning("Bio Data file not in the correct format. Will be starting with an empty "
                + "user list containing no bio data");
            initialUserData = new UserList();
        } catch (IOException e) {
            logger.warning("Bio Data file not in the correct format. Will be starting with an empty "
                + "user list containing no bio data");
            initialUserData = new UserList();
        }
        return initialData;
    }

    private String capitaliseFirstLetter(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
        try {
            foodListOptional = storage.readFoodList();
            if (!foodListOptional.isPresent()) {
                logger.info("Food list data file not found. Will be starting a sample food list");
            }
            initialFoodListData = foodListOptional.orElseGet(SampleDataUtil::getSampleFoodList);
        } catch (DataConversionException e) {
            logger.warning("Food list data file is not in the correct format. Will be starting with an empty "
                + "food list");
            initialFoodListData = new UniqueFoodList();
        } catch (IOException e) {
            logger.warning("Problem while reading from the file. Will be starting with an empty food list");
            initialFoodListData = new UniqueFoodList();
        }

        return new ModelManager(initialData, userPrefs, initialUserData, initialFoodListData, initialRecordListData,
            initialCalendar);
    }


    private void initLogging(Config config) {
        LogsCenter.init(config);
    }

    /**
     * Returns a {@code Config} using the file at {@code configFilePath}. <br> The default file path {@code
     * Config#DEFAULT_CONFIG_FILE} will be used instead if {@code configFilePath} is null.
     */
    protected Config initConfig(Path configFilePath) {
        Config initializedConfig;
        Path configFilePathUsed;

        configFilePathUsed = Config.DEFAULT_CONFIG_FILE;

        if (configFilePath != null) {
            logger.info("Custom Config file specified " + configFilePath);
            configFilePathUsed = configFilePath;
        }

        logger.info("Using config file : " + configFilePathUsed);

        try {
            Optional<Config> configOptional = ConfigUtil.readConfig(configFilePathUsed);
            initializedConfig = configOptional.orElse(new Config());
        } catch (DataConversionException e) {
            logger.warning("Config file at " + configFilePathUsed + " is not in the correct format. "
                + "Using default config properties");
            initializedConfig = new Config();
        }

        //Update config file in case it was missing to begin with or there are new/unused fields
        try {
            ConfigUtil.saveConfig(initializedConfig, configFilePathUsed);
        } catch (IOException e) {
            logger.warning("Failed to save config file : " + StringUtil.getDetails(e));
        }
        return initializedConfig;
    }

    /**
     * Returns a {@code UserPrefs} using the file at {@code storage}'s user prefs file path, or a new {@code UserPrefs}
     * with default configuration if errors occur when reading from the file.
     */
    protected UserPrefs initPrefs(UserPrefsStorage storage) {
        Path prefsFilePath = storage.getUserPrefsFilePath();
        logger.info("Using prefs file : " + prefsFilePath);

        UserPrefs initializedPrefs;
        try {
            Optional<UserPrefs> prefsOptional = storage.readUserPrefs();
            initializedPrefs = prefsOptional.orElse(new UserPrefs());
        } catch (DataConversionException e) {
            logger.warning("UserPrefs file at " + prefsFilePath + " is not in the correct format. "
                + "Using default user prefs");
            initializedPrefs = new UserPrefs();
        } catch (IOException e) {
            logger.warning("Problem while reading from the file. Will be starting with an empty AddressBook");
            initializedPrefs = new UserPrefs();
        }

        //Update prefs file in case it was missing to begin with or there are new/unused fields
        try {
            storage.saveUserPrefs(initializedPrefs);
        } catch (IOException e) {
            logger.warning("Failed to save config file : " + StringUtil.getDetails(e));
        }

        return initializedPrefs;
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting AddressBook " + MainApp.VERSION);
        ui.start(primaryStage);
    }

    @Override
    public void stop() {
        logger.info("============================ [ Stopping Address Book ] =============================");
        try {
            storage.saveUserPrefs(model.getUserPrefs());
        } catch (IOException e) {
            logger.severe("Failed to save preferences " + StringUtil.getDetails(e));
        }
        logic.stopAllReminders();
    }
}
