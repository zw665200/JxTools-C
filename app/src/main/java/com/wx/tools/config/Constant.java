package com.wx.tools.config;


public class Constant {

    public static final String PRODUCT_ID = "5";
    public static boolean isDebug = true;
    public static String ROM = "";
    public static final String ROM_MIUI = "MIUI";
    public static final String ROM_EMUI = "EMUI";
    public static final String ROM_FLYME = "FLYME";
    public static final String ROM_OPPO = "OPPO";
    public static final String ROM_VIVO = "VIVO";
    public static final String ROM_OTHER = "OTHER";
    public static String CLIENT_TOKEN = "";
    public static String USER_NAME = "";
    public static Boolean ScanStop = false;

    //official "e0987005e07e80ef" 10
    //应用宝 "bef03da7cb82efd7" 11
    //VIVO应用商店 "46abc5e760a15230" 12

    //百度渠道A002 "ac2999198df31940"
    //百度A002商店 "fb031e0e61441dee"
    //积木鱼002 576ef02578a829ea
    //百度信息流002 90c0fa8b866fae73

    //百度渠道A05 ‘81e480bc75b866eb’ 05
    //百度商店A05 ‘1f2853e951e49f28’ 05s
    //积木鱼005 9bf5a208e64addc0 05j
    //百度渠道010 d625395d2b8a6dca
    //百度渠道012 fed142756c778acb
    //百度渠道013 501595d7eebfdfe5
    //萌内朵02 cfe1af538b94749f

    public static String CHANNEL_ID = "576ef02578a829ea";
    public static String WEBSITE = "";
    public static boolean OCPC = true;

    //com.wx.tool
//    public static long USER_ACTION_SET_ID = 14915;
//    public static String APP_SECRET_KEY = "711fe5a04ffcfbaa13241a0e0ddaf411";

    //com.wx.tools
    public static long USER_ACTION_SET_ID = 11823;
    public static String APP_SECRET_KEY = "b2a7330b44238c87d322de2fd0e0b706";

    //com.wxrecovery.com
//    public static long USER_ACTION_SET_ID = 13294;
//    public static String APP_SECRET_KEY = "7e72818baf927073b841cdcbcd80869c";

    //service_code
    public static String REC = "rec";
    public static String COM = "com";
    public static String REPL = "repl";
    public static String BILL = "billrec";
    public static String DELETE = "delete";

    public static String EXPORT_PATH = "/export/";
    public static String WX_HIGN_VERSION_PATH = "/Android/data/com.tencent.mm/";
    public static String MM_RESOURCE_PATH = "/Android/data/com.immomo.momo/";
    public static String SOUL_RESOURCE_PATH = "/Android/data/cn.soulapp.android/";
    public static String WX_PICTURE_PATH = "/Pictures/WeiXin/";
    public static String PICTURE_PATH = "/Pictures/";
    public static String DOWNLOAD_PATH = "/Download/";
    public static String DCIM_PATH = "/DCIM/";
    public static String WX_RESOURCE_PATH = "/tencent/";
    public static String WX_DOWNLOAD_PATH = "/Download/Weixin/";
    public static String QQ_RESOURCE_PATH = "/tencent/MobileQQ/";
    public static String QQ_HIGN_VERSION_PATH = "/Android/data/com.tencent.mobileqq/";
    public static String WX_BACKUP_NAME = "tencent";
    public static String FLYME_BACKUP_PATH = "/backup/";
    public static String BACKUP_PATH = "/aA123456在此/";
    public static String XM_BACKUP_PATH = "/MIUI/";
    public static String OPPO_BACKUP_PATH = "/backup/App/";
    public static String HW_BACKUP_NAME_TAR = "com.tencent.mm.tar";
    public static String HW_BACKUP_APP_DATA_TAR = "com.tencent.mm_appDataTar";
    public static String MZ_BACKUP_NAME_TAR = "com.tencent.mm.zip";
    public static String XM_BACKUP_NAME_BAK = "微信(com.tencent.mm).bak";
    public static String OPPO_BACKUP_NAME_TAR = "com.tencent.mm.tar";
    public static String VIVO_BACKUP_NAME_TAR = "5a656b0891e6321126f9b7da9137994c72220ce7";
    public static String HW_BACKUP_NAME_XML = "info.xml";
    public static String JX_BACKUP_PATH = "/backup/";
    public static String DB_NAME = "EnMicroMsg.db";
    public static String DB_FTS_NAME = "FTS5IndexMicroMsg_encrypt.db";
    public static String UIN_CONFIG_NAME = "system_config_prefs.xml";
    public static String COMPATIBLE_INFO_NAME = "CompatibleInfo.cfg";
    public static String HISTORY_INFO_NAME = "app_brand_global_sp.xml";
    public static String DENGTA_META_NAME = "DENGTA_META.xml";
    public static String AUTH_INFO_KEY_NAME = "auth_info_key_prefs.xml";
    public static Long CURRENT_BACKUP_TIME = 0L;
    public static String CURRENT_BACKUP_PATH = "";

    //db
    public static String NOT_FOUND_ACCOUNT = "没有找到账户";
    public static String NOT_FOUND_CONTACT = "没有找到联系人";
    public static String NOT_FOUND_MESSAGE = "没有找到聊天记录";

    //Realm
    public static String ROOM_DB_NAME = "EnMicroMsg";

    //IM
    public static String SDK_APP_ID = "132041";
    public static String SDK_APP_KEY = "1482210305025478#kefuchannelapp90871";
    public static String SDK_SERVICE_ID = "kefuchannelimid_451993";
    public static String SDK_DEFAULT_PASSWORD = "123456";

    //Notification
    public static String Notification_title = "消息提醒";
    public static String Notification_content = "您有新的客服消息";

    //Bugly
    public static String BUGLY_APPID = "e87838f9d8";

    //oss
    public static String END_POINT = "http://oss-cn-shenzhen.aliyuncs.com";
    public static String END_POINT_WITHOUT_HTTP = "oss-cn-shenzhen.aliyuncs.com";
    public static String BUCKET_NAME = "qlrecovery";

    //tencent Pay
    public static String TENCENT_APP_ID = "wxfcb054ac8618018e";
    public static String TENCENT_MINI_PROGRAM_APP_ID = "gh_72629534a52d";
}
