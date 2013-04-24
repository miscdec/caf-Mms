package com.android.mms.ui;

import android.util.Log;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectHelper
{
  private static final String TAG = "ReflectHelper";

  public static Object callDeclaredMethod(Object paramObject, String paramString1, String paramString2, Class<?>[] paramArrayOfClass, Object[] paramArrayOfObject)
  {
    Object localObject1 = null;
    try
    {
      Method localMethod = Class.forName(paramString1).getDeclaredMethod(paramString2, paramArrayOfClass);
      localMethod.setAccessible(true);
      Object localObject2 = localMethod.invoke(paramObject, paramArrayOfObject);
      localObject1 = localObject2;
      return localObject1;
    }
    catch (NoSuchMethodException localNoSuchMethodException)
    {
      String str1 = localNoSuchMethodException.getMessage();
    }
    catch (Exception localException)
    {
      String str2 = localException.getMessage();
    }
      return localObject1;
  }

  public static Object callDeclaredMethod(Object paramObject, String paramString, Class<?>[] paramArrayOfClass, Object[] paramArrayOfObject)
  {
    Class localClass = paramObject.getClass();
    Object localObject1 = null;
    try
    {
      Method localMethod = localClass.getDeclaredMethod(paramString, paramArrayOfClass);
      localMethod.setAccessible(true);
      Object localObject2 = localMethod.invoke(paramObject, paramArrayOfObject);
      localObject1 = localObject2;
      return localObject1;
    }
    catch (NoSuchMethodException localNoSuchMethodException)
    {
      String str1 = localNoSuchMethodException.getMessage();
    }
    catch (Exception localException)
    {
      String str2 = localException.getMessage();
    }
      return localObject1;
  }

  public static Object callMethod(Object paramObject, String paramString, Class<?>[] paramArrayOfClass, Object[] paramArrayOfObject)
  {
    Class localClass = paramObject.getClass();
    Log.d("Reflect", "localClass +++++++++"+localClass.toString());
    Object localObject1 = null;
    try
    {
      Object localObject2 = localClass.getMethod(paramString, paramArrayOfClass).invoke(paramObject, paramArrayOfObject);
      localObject1 = localObject2;
      return localObject1;
    }
    catch (NoSuchMethodException localNoSuchMethodException)
    {
      String str1 = localNoSuchMethodException.getMessage();
    }
    catch (Exception localException)
    {
      String str2 = localException.getMessage();
    }
      return localObject1;
  }

  public static Object callStaticMethod(Class<?> paramClass, String paramString, Class<?>[] paramArrayOfClass, Object[] paramArrayOfObject)
  {
    Object localObject1 = null;
    try
    {
      Method localMethod = paramClass.getDeclaredMethod(paramString, paramArrayOfClass);
      localMethod.setAccessible(true);
      Object localObject2 = localMethod.invoke(null, paramArrayOfObject);
      localObject1 = localObject2;
    }
    catch (NoSuchMethodException localNoSuchMethodException)
    {
      String str1 = localNoSuchMethodException.getMessage();
    }
    catch (Exception localException)
    {
      String str2 = localException.getMessage();
    }
      return localObject1;
  }

  public static Object callStaticMethod(String paramString1, String paramString2, Class<?>[] paramArrayOfClass, Object[] paramArrayOfObject)
  {
    Object localObject1;
    Object localObject2;
    try
    {
      localObject1 = callStaticMethod(Class.forName(paramString1), paramString2, paramArrayOfClass, paramArrayOfObject);
      localObject2 = localObject1;
    }
    catch (ClassNotFoundException localClassNotFoundException)
    {
      String str = localClassNotFoundException.getMessage();
      localObject2 = null;
    }
      return localObject2;
  }

  public static boolean classSupported(String paramString)
  {
    Class localClass;
    boolean i;
    try
    {
      localClass = Class.forName(paramString);
      i = true;
    }
    catch (ClassNotFoundException localClassNotFoundException)
    {
      i = false;
    }
      return i;
  }

  public static Object getDeclaredFieldValue(Object paramObject, String paramString)
  {
    Class localClass = paramObject.getClass();
    Object localObject1 = null;
    try
    {
      Field localField = localClass.getDeclaredField(paramString);
      localField.setAccessible(true);
      Object localObject2 = localField.get(paramObject);
      localObject1 = localObject2;
    }
    catch (NoSuchFieldException localNoSuchFieldException)
    {
      String str1 = Log.getStackTraceString(localNoSuchFieldException);
    }
    catch (Exception localException)
    {
      String str2 = Log.getStackTraceString(localException);
    }
      return localObject1;
  }

  public static Object getDeclaredFieldValue(Object paramObject, String paramString1, String paramString2)
  {
    Object localObject1 = null;
    try
    {
      Field localField = Class.forName(paramString1).getDeclaredField(paramString2);
      localField.setAccessible(true);
      Object localObject2 = localField.get(paramObject);
      localObject1 = localObject2;
    }
    catch (NoSuchFieldException localNoSuchFieldException)
    {
      String str1 = localNoSuchFieldException.getMessage();
    }
    catch (ClassNotFoundException localClassNotFoundException)
    {
      String str2 = localClassNotFoundException.getMessage();
    }
    catch (Exception localException)
    {
      String str3 = localException.getMessage();
    }
      return localObject1;
  }

  public static Object getFieldValue(Object paramObject, String paramString)
  {
    Class localClass = paramObject.getClass();
    Object localObject1 = null;
    try
    {
      Object localObject2 = localClass.getField(paramString).get(paramObject);
      localObject1 = localObject2;
    }
    catch (NoSuchFieldException localNoSuchFieldException)
    {
      String str1 = localNoSuchFieldException.getMessage();
    }
    catch (Exception localException)
    {
      String str2 = localException.getMessage();
    }
      return localObject1;
  }

  public static Object getStaticFieldValue(Class<?> paramClass, String paramString)
  {
    Object localObject1 = null;
    try
    {
      Field localField = paramClass.getDeclaredField(paramString);
      localField.setAccessible(true);
      Object localObject2 = localField.get(null);
      localObject1 = localObject2;
    }
    catch (NoSuchFieldException localNoSuchFieldException)
    {
      String str1 = localNoSuchFieldException.getMessage();
    }
    catch (Exception localException)
    {
      String str2 = localException.getMessage();
    }
      return localObject1;
  }

  public static Object getStaticFieldValue(String paramString1, String paramString2)
  {
    Object localObject1;
    Object localObject2;
    try
    {
      localObject1 = getStaticFieldValue(Class.forName(paramString1), paramString2);
      localObject2 = localObject1;
    }
    catch (ClassNotFoundException localClassNotFoundException)
    {
      String str = localClassNotFoundException.getMessage();
      localObject2 = null;
    }
      return localObject2;
  }
/*
  public static boolean methodSupported(Object paramObject, String paramString, Class<?>[] paramArrayOfClass)
  {
    return methodSupported(paramObject.getClass().getName(), paramString, paramArrayOfClass);
  }

  public static boolean methodSupported(String paramString1, String paramString2, Class<?>[] paramArrayOfClass)
  {
      boolean i = false;
    try
    {
      Class localClass1 = Class.forName(paramString1);
      Class localClass2 = localClass1;
    }
    catch (ClassNotFoundException localClassNotFoundException)
    {
      try
      {
        Method localMethod = localClass2.getDeclaredMethod(paramString2, paramArrayOfClass);
        i = true;
      }
      catch (SecurityException localSecurityException)
      {
        localSecurityException.printStackTrace();
        i = false;
      }
      catch (NoSuchMethodException localNoSuchMethodException)
      {
        while (true)
        {
          localNoSuchMethodException.printStackTrace();
          i = false;
        }
        localClassNotFoundException = localClassNotFoundException;
        i = false;
      }
    }
        return i;
  }
*/
  public static Object newInstance(Class<?> paramClass)
  {
    Object localObject1;
    Object localObject2;
    try
    {
      localObject1 = paramClass.newInstance();
      localObject2 = localObject1;
    }
    catch (IllegalAccessException localIllegalAccessException)
    {
      localIllegalAccessException.printStackTrace();
      localObject2 = null;
    }
    catch (InstantiationException localInstantiationException)
    {
      while (true)
        localInstantiationException.printStackTrace();
    }
      return localObject2;
  }

  public static Object newInstance(Class<?> paramClass, Class<?>[] paramArrayOfClass, Object[] paramArrayOfObject)
  {
    Object localObject1 = null;
    try
    {
      Constructor localConstructor = paramClass.getDeclaredConstructor(paramArrayOfClass);
      localConstructor.setAccessible(true);
      Object localObject2 = localConstructor.newInstance(paramArrayOfObject);
      localObject1 = localObject2;
    }
    catch (NoSuchMethodException localNoSuchMethodException)
    {
      localNoSuchMethodException.printStackTrace();
    }
    catch (Exception localException)
    {
      localException.printStackTrace();
    }
      return localObject1;
  }

  public static Object newInstance(String paramString)
  {
    Object localObject1;
    Object localObject2;
    try
    {
      localObject1 = newInstance(Class.forName(paramString));
      localObject2 = localObject1;
    }
    catch (ClassNotFoundException localClassNotFoundException)
    {
      localClassNotFoundException.printStackTrace();
      localObject2 = null;
    }
      return localObject2;
  }

  public static Object newInstance(String paramString, Class<?>[] paramArrayOfClass, Object[] paramArrayOfObject)
  {
    Object localObject1;
    Object localObject2;
    try
    {
      localObject1 = newInstance(Class.forName(paramString), paramArrayOfClass, paramArrayOfObject);
      localObject2 = localObject1;
    }
    catch (ClassNotFoundException localClassNotFoundException)
    {
      localClassNotFoundException.printStackTrace();
      localObject2 = null;
    }
      return localObject2;
  }

  public static void setDeclaredFieldValue(Class<?> paramClass, Object paramObject1, String paramString1, String paramString2, Object paramObject2)
  {
    Field localField1;
    try
    {
      localField1 = paramClass.getDeclaredField(paramString1);
      localField1.setAccessible(true);
      Object localObject = localField1.get(paramObject1);
      Field localField2 = localField1.getType().getDeclaredField(paramString2);
      localField2.setAccessible(true);
      localField2.set(localObject, paramObject2);
      return;
    }
    catch (NoSuchFieldException localNoSuchFieldException)
    {
      localNoSuchFieldException.printStackTrace();
      return;
    }
    catch (Exception localException)
    {
      localException.printStackTrace();
      return;
    }
  }

  public static void setDeclaredFieldValue(Object paramObject1, String paramString, Object paramObject2)
  {
    Class localClass = paramObject1.getClass();
    try
    {
      Field localField = localClass.getDeclaredField(paramString);
      localField.setAccessible(true);
      localField.set(paramObject1, paramObject2);
      return;
    }
    catch (NoSuchFieldException localNoSuchFieldException)
    {
      localNoSuchFieldException.printStackTrace();
      return;
    }
    catch (Exception localException)
    {
      localException.printStackTrace();
    }
  }

  public static void setDeclaredFieldValue(Object paramObject1, String paramString1, String paramString2, Object paramObject2)
  {
    Field localField;
    try
    {
      localField = Class.forName(paramString1).getDeclaredField(paramString2);
      localField.setAccessible(true);
      localField.set(paramObject1, paramObject2);
      return;
    }
    catch (ClassNotFoundException localClassNotFoundException)
    {
      localClassNotFoundException.printStackTrace();
      return;
    }
    catch (NoSuchFieldException localNoSuchFieldException)
    {
      localNoSuchFieldException.printStackTrace();
      return;
    }
    catch (Exception localException)
    {
      localException.printStackTrace();
    }
  }

  public static void setFieldValue(Object paramObject1, String paramString, Object paramObject2)
  {
    Class localClass = paramObject1.getClass();
    try
    {
      localClass.getField(paramString).set(paramObject1, paramObject2);
      return;
    }
    catch (NoSuchFieldException localNoSuchFieldException)
    {
      String str1 = localNoSuchFieldException.getMessage();
      return;
    }
    catch (Exception localException)
    {
      String str2 = localException.getMessage();
    }
  }

  public static void setStaticFieldValue(Class<?> paramClass, String paramString, Object paramObject)
  {
    Field localField;
    try
    {
      localField = paramClass.getDeclaredField(paramString);
      localField.setAccessible(true);
      localField.set(null, paramObject);
      return;
    }
    catch (NoSuchFieldException localNoSuchFieldException)
    {
      localNoSuchFieldException.printStackTrace();
      return;
    }
    catch (Exception localException)
    {
      localException.printStackTrace();
    }
  }

  public static void setStaticFieldValue(String paramString1, String paramString2, Object paramObject)
  {
    try
    {
      setStaticFieldValue(Class.forName(paramString1), paramString2, paramObject);
      return;
    }
    catch (ClassNotFoundException localClassNotFoundException)
    {
      localClassNotFoundException.printStackTrace();
    }
  }
}
