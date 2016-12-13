package org.jy.mycache.util;

import java.util.Map; 

public class StringUtils {

   public static final String EMPTY = "";
 
   public static boolean isBlank(String str)
   {
     if (isEmpty(str))
     {
       return true;
     }
     for (int i = 0; i < str.length(); ++i)
     {
       if (!(Character.isWhitespace(str.charAt(i))))
       {
         return false;
       }
     }
     return true;
   }







   public static boolean isNotBlank(String string)
   {
     return (!(isBlank(string)));
   }






   public static boolean contains(String baseString, String searchString)
   {
     if ((baseString == null) || (searchString == null))
     {
       return false;

     }
 
     return (baseString.contains(searchString));
   }











   public static String substringBefore(String str, String separator)
   {
     if ((isEmpty(str)) || (separator == null))
     {
       return str;
     }
     if (separator.length() == 0)
     {
       return "";
     }
     int pos = str.indexOf(separator);
     if (pos == -1)
     {
       return str;
     }
     return str.substring(0, pos);
   }

   public static String checkPrecondition(Map<String, String> map, String key)
   {
     return checkPrecondition((String)map.get(key), key);
   }









   public static String checkPrecondition(String value, String fieldName)
   {
     if (isBlank(value))
     {
       throw new RuntimeException(fieldName);
     }
 
     return value;
   }









   public static boolean isEmpty(String string)
   {
     return ((string == null) || (string.isEmpty()));
   }
 
}
