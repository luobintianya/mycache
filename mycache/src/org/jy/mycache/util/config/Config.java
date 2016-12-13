package org.jy.mycache.util.config;

import java.util.HashMap;
import java.util.Map;

import org.jy.mycache.util.Registry;

public class Config {
 
   public static enum AppServer
   {
     oc4j, jboss, geronimo, websphere, orion, weblogic, tomcat, standalone;
   } 

   public static abstract interface Params
   {
     public static final String CLUSTERMODE = "clustermode".intern();
     public static final String CLUSTER_ID = "cluster.id".intern();
     public static final String CLUSTER_MAXID = "cluster.maxid".intern();

     public static final String MAIL_FROM = "mail.from".intern();

     public static final String MAIL_REPLYTO = "mail.replyto".intern();

     public static final String MAIL_SMTP_SERVER = "mail.smtp.server".intern();

     public static final String MAIL_SMTP_PORT = "mail.smtp.port".intern();

     public static final String MAIL_SMTP_USER = "mail.smtp.user".intern();

     public static final String MAIL_SMTP_PASSWORD = "mail.smtp.password".intern();

     public static final String MAIL_POP3_BEFORESMTP = "mail.pop3.beforesmtp".intern();

     public static final String MAIL_POP3_SERVER = "mail.pop3.server".intern();

     public static final String MAIL_POP3_USER = "mail.pop3.user".intern();

     public static final String MAIL_POP3_PASSWORD = "mail.pop3.password".intern();

     public static final String MAIL_FROM_JNDI = "mail.fromJNDI".intern();

     public static final String MAIL_USE_TLS = "mail.use.tls".intern();

     public static final String CRONJOB_MAIL_SUBJECT_SUCCESS = "cronjob.mail.subject.success".intern();

     public static final String CRONJOB_MAIL_SUBJECT_FAIL = "cronjob.mail.subject.fail".intern();

     public static final String BYPASS_HYBRIS_RECOMMENDATIONS = "bypass.hybris.recommendations".intern();
   }

   
   public static abstract interface SystemSpecificParams
   {
     public static final String DB_USERNAME = "db.username".intern();
     public static final String DB_PASSWORD = "db.password".intern();
     public static final String DB_URL = "db.url".intern();
     public static final String DB_DRIVER = "db.driver".intern();
     public static final String DB_TABLEPREFIX = "db.tableprefix".intern();
     public static final String DB_POOL_FROMJNDI = "db.pool.fromJNDI".intern();
     public static final String EXTENSIONS = "allowed.extensions".intern();
     public static final String LOCALE = "locale".intern();
     public static final String TIME_ZONE = "timezone".intern();
     public static final String DB_FACTORY = "db.factory".intern();
     public static final String DB_CUSTOM_PARAM = "db.connectionparam";
   }

 



   public static Map<String, String> getParametersByPattern(String pattern)
   {
     Map<String, String> origParams = getAllParameters();
     Map<String, String> params = new HashMap<String, String>(origParams);
     for (String key : origParams.keySet())
     {
       if (key.startsWith(pattern))
         continue;
       params.remove(key);
     }
 
     return params;
   }
 

   public static Map<String, String> getAllParameters()
   {
     return Registry.getConfig().getAllParameters();
   }
 


   public static String getParameter(String key)
   {
     return Registry.getConfig().getParameter(key);
   }








   public static String getString(String key, String def)
   {
     return Registry.getConfig().getString(key, def);
   }

   public static char getChar(String key, char def) throws IndexOutOfBoundsException
   {
     return Registry.getConfig().getChar(key, def);
   }

   public static boolean getBoolean(String key, boolean def)
   {
     return Registry.getConfig().getBoolean(key, def);
   }

   public static int getInt(String key, int def) throws NumberFormatException
   {
     return Registry.getConfig().getInt(key, def);
   }

   public static long getLong(String key, long def) throws NumberFormatException
   {
     return Registry.getConfig().getLong(key, def);
   }

   public static double getDouble(String key, double def) throws NumberFormatException
   {
     return Registry.getConfig().getDouble(key, def);
   }









   public static void setParameter(String key, String value)
   {
     Registry.getConfig().setParameter(key, value);
   }



 






   @Deprecated
   public static boolean itemCacheIsolationActivated()
   {
     return true;
   }




















   public static String trim(String value, char[] ignore)
   {
     char[] characters = value.toCharArray();
     int count = characters.length;
 
     int limit = count + 0;
     if ((count == 0) || ((characters[0] > ' ') && (characters[(limit - 1)] > ' ')))
     {
       return value;
     }
     int begin = 0;
     char c;
     do
     {
       if (begin == limit)
       {
         return "";
       }
       c = characters[(begin++)];
     }
     while ((c <= ' ') && (!(contains(c, ignore))));
 
     int end = limit;

     do
     {
       c = characters[(--end)];
     }
     while ((c <= ' ') && (!(contains(c, ignore))));
 
     return value.substring(begin - 0 - 1, end - 0 + 1);
   }

   private static boolean contains(char c, char[] list)
   {
     for (int i = 0; i < list.length; ++i)
     {
       if (c == list[i])
       {
         return true;
       }
     }
     return false;
   }
 
}
