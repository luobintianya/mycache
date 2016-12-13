package org.jy.mycache.util.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FastHashMapConfig extends AbstractConfig<String>{

   private final Map<String, String> map;
 
   public FastHashMapConfig(Map<String, String> map)
   {
     this.map = new ConcurrentHashMap<String,String>((int)(map.size() / 0.75F) + 1, 0.75F, 64);
     this.map.putAll(map);
   }


   public Map<String, String> getAllParameters()
   {
     return Collections.unmodifiableMap(this.map);
   }


   public String getParameter(String key)
   {
     return ((String)this.map.get(key));
   }


   public Map<String, String> getParametersMatching(String keyRegExp, boolean stripMatchingKey)
   {
     Map<String,String> ret = null;
     Pattern pattern = Pattern.compile(keyRegExp, 2);
 
     for (Map.Entry<String,String> e : this.map.entrySet())
     {
       Matcher matcher = pattern.matcher((CharSequence)e.getKey());
       if (!(matcher.matches()))
         continue;
       if (ret == null)
       {
         ret = new HashMap<String,String>(this.map.size() + 1, 1.0F);
       }
       if (stripMatchingKey)
       {
         ret.put(matcher.group(1), (String)e.getValue());
       }
       else
       {
         ret.put((String)e.getKey(), (String)e.getValue());
       }
     }
 
     return ((ret != null) ? ret :  new HashMap<String,String>());
   }


   public String setParameter(String key, String value)
   {
     if (value != null)
     {
       String prev = (String)this.map.put(key, value);
       notifyListeners(key, prev, value);
       return prev;

     }
 
     return removeParameter(key);
   }



   public String removeParameter(String key)
   {
     String prev = (String)this.map.remove(key);
     notifyListeners(key, prev, null);
     return prev;
   }
 
}
