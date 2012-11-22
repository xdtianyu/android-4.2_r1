/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.motorolamobility.preflighting.core.internal.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.motorolamobility.preflighting.core.source.model.PermissionGroups;

public final class MethodPermissionCSVReader
{

    private static final String ANDROID_PERMISSION_PREFIX = "android.permission.";

    /**
     * For tests
     * @param args
     */
    public static void main(String[] args)
    {
        File f =
                new File(
                        "C:\\motodev\\motodev\\android\\src\\plugins\\preflighting.core\\files\\method_permission_list_4.0.csv");
        try
        {
            readMapMethodToPermission(new InputStreamReader(new FileInputStream(f)));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    /**
     * Reads files\method_permission_list.csv and creates a map with 
     * key - classPackagePath.methodName
     * value - permission name (without android.permission) (it can use || to indicate conditions that are optional, that is interchangeable)
     * Case there is more than one line for a permission it indicates that it is a && condition
     * 
     * E.g.: If the file have the following declarations
     * android.accounts.AccountManager.method1,PERMISSION1
     * android.accounts.AccountManager.method1,PERMISSION2||PERMISSION3
     * 
     * It will return the following:
     * PERMISSION1 = required
     * PERMISSION2, PERMISSION3 = optional (one of them is required)
     * 
     * @param csv
     * @return
     * @throws IOException
     */
    public static Map<String, PermissionGroups> readMapMethodToPermission(
            InputStreamReader csvStream) throws IOException
    {
        Map<String, PermissionGroups> methodToPermission = new HashMap<String, PermissionGroups>();
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(csvStream);
            String line = reader.readLine();
            while (line != null)
            {
                String[] splitAux = line.split(",");
                if (splitAux.length >= 2)
                {
                    String classAndMethod = splitAux[0];
                    String permissionGroupAux = splitAux[1];
                    PermissionGroups permissionGroups = methodToPermission.get(classAndMethod);
                    if (permissionGroups == null)
                    {
                        //method not included yet - add new entry on map
                        permissionGroups = new PermissionGroups();
                        methodToPermission.put(classAndMethod, permissionGroups);
                    }
                    if ((permissionGroupAux != null) && permissionGroupAux.contains("||"))
                    {
                        //optional permissions - permissions should not contain spaces
                        String[] optionalPermissions = permissionGroupAux.split("\\|\\|");

                        for (int i = 0; i < optionalPermissions.length; i++)
                        {
                            String optionalPermission = optionalPermissions[i];
                            optionalPermissions[i] = ANDROID_PERMISSION_PREFIX + optionalPermission;
                        }

                        permissionGroups.getOptionalPermissions().addAll(
                                Arrays.asList(optionalPermissions));
                    }
                    else
                    {
                        //required permission
                        permissionGroups.getRequiredPermissions().add(
                                ANDROID_PERMISSION_PREFIX + permissionGroupAux);
                    }

                }
                line = reader.readLine();
            }
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (Exception e)
                {
                    //Do Nothing.
                }
            }
        }

        return methodToPermission;
    }

    /**
     * Method that prints the mapping into a file.
     * Used to check if the mapping is being properly constructed.
     * @param methodToPermission
     */
    @SuppressWarnings("unused")
    private static void saveToFile(Map<String, PermissionGroups> methodToPermission)
    {
        File f = new File("C:\\methodToPermission.csv");
        OutputStreamWriter writer = null;

        try
        {
            writer = new OutputStreamWriter(new FileOutputStream(f));

            Set<String> classes = methodToPermission.keySet();

            for (String cls : classes)
            {
                PermissionGroups permissions = methodToPermission.get(cls);

                for (String permission : permissions.getRequiredPermissions())
                {
                    writer.append(cls + "," + permission.substring(permission.lastIndexOf('.') + 1)
                            + "\n");
                }

                if (!permissions.getOptionalPermissions().isEmpty())
                {
                    writer.append(cls + ",");
                    boolean first = true;
                    for (String permission : permissions.getOptionalPermissions())
                    {
                        writer.append(first ? permission.substring(permission.lastIndexOf('.') + 1)
                                : "||" + permission.substring(permission.lastIndexOf('.') + 1));
                        first = false;
                    }
                    writer.append('\n');
                }
            }
            writer.flush();

        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (writer != null)
            {
                try
                {
                    writer.close();
                }
                catch (IOException e)
                {
                    //Do Nothing.
                }
            }
        }
    }
}
