package finsim.common;

public final class Constants {

    /**
     * PORTS
     */
    public static final int TEST_VMS_PORT = 8080;

    public static final int INSTRUMENT_VMS_PORT = 8092;

    public static final int ORDER_VMS_PORT = 8082;
    
    public static final int MARKET_VMS_PORT = 8084;

    public static final int CLIENT_VMS_PORT = 8085;

    public static final int PORTFOLIO_VMS_PORT = 8086;

    /**
     * INPUTS
     */
    public static final String UPDATE_TEST_ITEM = "update_test_item";
    
    public static final String CREATE_INSTRUMENT = "create_instrument";
    public static final String UPDATE_INSTRUMENT = "update_instrument";
    public static final String DELIST_INSTRUMENT = "delist_instrument";
    
    public static final String CREATE_ORDER = "create_order";
    public static final String UPDATE_ORDER = "update_order";
    public static final String CANCEL_ORDER = "cancel_order";
    public static final String FILL_ORDER = "fill_order";

    public static final String CREATE_CLIENT = "create_client";
    public static final String UPDATE_CLIENT = "update_client";
    public static final String CREATE_ACCOUNT = "create_account";
    public static final String UPDATE_ACCOUNT = "update_account";
    
    public static final String CREATE_TRANSFER = "create_transfer";

    public static final String MARKET_FILL = "market_fill";
    
    // Market service inputs
    public static final String ORDER_ROUTED = "order_routed";

    /**
     * EVENTS
     */
    public static final String TEST_ITEM_UPDATED = "test_item_updated";
    
    public static final String INSTRUMENT_CREATED = "instrument_created";
    public static final String INSTRUMENT_CHANGED = "instrument_changed";
    public static final String INSTRUMENT_DELISTED = "instrument_delisted";
    
    public static final String ORDER_CREATED = "order_created";
    public static final String ORDER_CHANGED = "order_changed";
    public static final String ORDER_CANCELLED = "order_cancelled";
    public static final String ORDER_FILLED = "order_filled";
    public static final String ORDER_POSITION_CREATED = "order_position_created";

    public static final String CLIENT_CREATED = "client_created";
    public static final String CLIENT_CHANGED = "client_changed";
    public static final String ACCOUNT_CREATED = "account_created";
    public static final String ACCOUNT_CHANGED = "account_changed";

    public static final String TRANSFER_CREATED = "transfer_created";
    public static final String POSITION_MODIFIED = "position_modified";

    public static final String ORDER_HANDLED = "order_handled";
}