/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ide.common.resources;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.resources.configuration.Configurable;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.common.resources.configuration.LanguageQualifier;
import com.android.ide.common.resources.configuration.RegionQualifier;
import com.android.io.IAbstractFile;
import com.android.io.IAbstractFolder;
import com.android.io.IAbstractResource;
import com.android.resources.FolderTypeRelationship;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Base class for resource repository.
 *
 * A repository is both a file representation of a resource folder and a representation
 * of the generated resources, organized by type.
 *
 * {@link #getResourceFolder(IAbstractFolder)} and {@link #getSourceFiles(ResourceType, String, FolderConfiguration)}
 * give access to the folders and files of the resource folder.
 *
 * {@link #getResources(ResourceType)} gives access to the resources directly.
 *
 */
public abstract class ResourceRepository {

    protected final Map<ResourceFolderType, List<ResourceFolder>> mFolderMap =
        new EnumMap<ResourceFolderType, List<ResourceFolder>>(ResourceFolderType.class);

    protected final Map<ResourceType, Map<String, ResourceItem>> mResourceMap =
            new EnumMap<ResourceType, Map<String, ResourceItem>>(
            ResourceType.class);

    private final Map<Map<String, ResourceItem>, Collection<ResourceItem>> mReadOnlyListMap =
            new IdentityHashMap<Map<String, ResourceItem>, Collection<ResourceItem>>();

    private final boolean mFrameworkRepository;

    protected final IntArrayWrapper mWrapper = new IntArrayWrapper(null);

    /**
     * Makes a resource repository
     * @param isFrameworkRepository whether the repository is for framework resources.
     */
    protected ResourceRepository(boolean isFrameworkRepository) {
        mFrameworkRepository = isFrameworkRepository;
    }

    public boolean isFrameworkRepository() {
        return mFrameworkRepository;
    }

    /**
     * Adds a Folder Configuration to the project.
     * @param type The resource type.
     * @param config The resource configuration.
     * @param folder The workspace folder object.
     * @return the {@link ResourceFolder} object associated to this folder.
     */
    private ResourceFolder add(
            @NonNull ResourceFolderType type,
            @NonNull FolderConfiguration config,
            @NonNull IAbstractFolder folder) {
        // get the list for the resource type
        List<ResourceFolder> list = mFolderMap.get(type);

        if (list == null) {
            list = new ArrayList<ResourceFolder>();

            ResourceFolder cf = new ResourceFolder(type, config, folder, this);
            list.add(cf);

            mFolderMap.put(type, list);

            return cf;
        }

        // look for an already existing folder configuration.
        for (ResourceFolder cFolder : list) {
            if (cFolder.mConfiguration.equals(config)) {
                // config already exist. Nothing to be done really, besides making sure
                // the IAbstractFolder object is up to date.
                cFolder.mFolder = folder;
                return cFolder;
            }
        }

        // If we arrive here, this means we didn't find a matching configuration.
        // So we add one.
        ResourceFolder cf = new ResourceFolder(type, config, folder, this);
        list.add(cf);

        return cf;
    }

    /**
     * Removes a {@link ResourceFolder} associated with the specified {@link IAbstractFolder}.
     * @param type The type of the folder
     * @param removedFolder the IAbstractFolder object.
     * @param context the scanning context
     * @return the {@link ResourceFolder} that was removed, or null if no matches were found.
     */
    @Nullable
    public ResourceFolder removeFolder(
            @NonNull ResourceFolderType type,
            @NonNull IAbstractFolder removedFolder,
            @Nullable ScanningContext context) {
        // get the list of folders for the resource type.
        List<ResourceFolder> list = mFolderMap.get(type);

        if (list != null) {
            int count = list.size();
            for (int i = 0 ; i < count ; i++) {
                ResourceFolder resFolder = list.get(i);
                IAbstractFolder folder = resFolder.getFolder();
                if (removedFolder.equals(folder)) {
                    // we found the matching ResourceFolder. we need to remove it.
                    list.remove(i);

                    // remove its content
                    resFolder.dispose(context);

                    return resFolder;
                }
            }
        }

        return null;
    }

    /**
     * Returns true if this resource repository contains a resource of the given
     * name.
     *
     * @param url the resource URL
     * @return true if the resource is known
     */
    public boolean hasResourceItem(@NonNull String url) {
        assert url.startsWith("@") || url.startsWith("?") : url;

        int typeEnd = url.indexOf('/', 1);
        if (typeEnd != -1) {
            int nameBegin = typeEnd + 1;

            // Skip @ and @+
            int typeBegin = url.startsWith("@+") ? 2 : 1; //$NON-NLS-1$

            int colon = url.lastIndexOf(':', typeEnd);
            if (colon != -1) {
                typeBegin = colon + 1;
            }
            String typeName = url.substring(typeBegin, typeEnd);
            ResourceType type = ResourceType.getEnum(typeName);
            if (type != null) {
                String name = url.substring(nameBegin);
                return hasResourceItem(type, name);
            }
        }

        return false;
    }

    /**
     * Returns true if this resource repository contains a resource of the given
     * name.
     *
     * @param type the type of resource to look up
     * @param name the name of the resource
     * @return true if the resource is known
     */
    public boolean hasResourceItem(@NonNull ResourceType type, @NonNull String name) {
        Map<String, ResourceItem> map = mResourceMap.get(type);

        if (map != null) {

            ResourceItem resourceItem = map.get(name);
            if (resourceItem != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns a {@link ResourceItem} matching the given {@link ResourceType} and name. If none
     * exist, it creates one.
     *
     * @param type the resource type
     * @param name the name of the resource.
     * @return A resource item matching the type and name.
     */
    @NonNull
    protected ResourceItem getResourceItem(@NonNull ResourceType type, @NonNull String name) {
        // looking for an existing ResourceItem with this type and name
        ResourceItem item = findDeclaredResourceItem(type, name);

        // create one if there isn't one already, or if the existing one is inlined, since
        // clearly we need a non inlined one (the inline one is removed too)
        if (item == null || item.isDeclaredInline()) {
            ResourceItem oldItem = item != null && item.isDeclaredInline() ? item : null;

            item = createResourceItem(name);

            Map<String, ResourceItem> map = mResourceMap.get(type);

            if (map == null) {
                if (isFrameworkRepository()) {
                    // Pick initial size for the maps. Also change the load factor to 1.0
                    // to avoid rehashing the whole table when we (as expected) get near
                    // the known rough size of each resource type map.
                    int size;
                    switch (type) {
                        // Based on counts in API 16. Going back to API 10, the counts
                        // are roughly 25-50% smaller (e.g. compared to the top 5 types below
                        // the fractions are 1107 vs 1734, 831 vs 1508, 895 vs 1255,
                        // 733 vs 1064 and 171 vs 783.
                        case PUBLIC:           size = 1734; break;
                        case DRAWABLE:         size = 1508; break;
                        case STRING:           size = 1255; break;
                        case ATTR:             size = 1064; break;
                        case STYLE:             size = 783; break;
                        case ID:                size = 347; break;
                        case DECLARE_STYLEABLE: size = 210; break;
                        case LAYOUT:            size = 187; break;
                        case COLOR:             size = 120; break;
                        case ANIM:               size = 95; break;
                        case DIMEN:              size = 81; break;
                        case BOOL:               size = 54; break;
                        case INTEGER:            size = 52; break;
                        case ARRAY:              size = 51; break;
                        case PLURALS:            size = 20; break;
                        case XML:                size = 14; break;
                        case INTERPOLATOR :      size = 13; break;
                        case ANIMATOR:            size = 8; break;
                        case RAW:                 size = 4; break;
                        case MENU:                size = 2; break;
                        case MIPMAP:              size = 2; break;
                        case FRACTION:            size = 1; break;
                        default:
                            size = 2;
                    }
                    map = new HashMap<String, ResourceItem>(size, 1.0f);
                } else {
                    map = new HashMap<String, ResourceItem>();
                }
                mResourceMap.put(type, map);
            }

            map.put(item.getName(), item);

            if (oldItem != null) {
                map.remove(oldItem.getName());

            }
        }

        return item;
    }

    /**
     * Creates a resource item with the given name.
     * @param name the name of the resource
     * @return a new ResourceItem (or child class) instance.
     */
    @NonNull
    protected abstract ResourceItem createResourceItem(@NonNull String name);

    /**
     * Processes a folder and adds it to the list of existing folders.
     * @param folder the folder to process
     * @return the ResourceFolder created from this folder, or null if the process failed.
     */
    @Nullable
    public ResourceFolder processFolder(@NonNull IAbstractFolder folder) {
        // split the name of the folder in segments.
        String[] folderSegments = folder.getName().split(SdkConstants.RES_QUALIFIER_SEP);

        // get the enum for the resource type.
        ResourceFolderType type = ResourceFolderType.getTypeByName(folderSegments[0]);

        if (type != null) {
            // get the folder configuration.
            FolderConfiguration config = FolderConfiguration.getConfig(folderSegments);

            if (config != null) {
                return add(type, config, folder);
            }
        }

        return null;
    }

    /**
     * Returns a list of {@link ResourceFolder} for a specific {@link ResourceFolderType}.
     * @param type The {@link ResourceFolderType}
     */
    @Nullable
    public List<ResourceFolder> getFolders(@NonNull ResourceFolderType type) {
        return mFolderMap.get(type);
    }

    @NonNull
    public List<ResourceType> getAvailableResourceTypes() {
        List<ResourceType> list = new ArrayList<ResourceType>();

        // For each key, we check if there's a single ResourceType match.
        // If not, we look for the actual content to give us the resource type.

        for (ResourceFolderType folderType : mFolderMap.keySet()) {
            List<ResourceType> types = FolderTypeRelationship.getRelatedResourceTypes(folderType);
            if (types.size() == 1) {
                // before we add it we check if it's not already present, since a ResourceType
                // could be created from multiple folders, even for the folders that only create
                // one type of resource (drawable for instance, can be created from drawable/ and
                // values/)
                if (list.contains(types.get(0)) == false) {
                    list.add(types.get(0));
                }
            } else {
                // there isn't a single resource type out of this folder, so we look for all
                // content.
                List<ResourceFolder> folders = mFolderMap.get(folderType);
                if (folders != null) {
                    for (ResourceFolder folder : folders) {
                        Collection<ResourceType> folderContent = folder.getResourceTypes();

                        // then we add them, but only if they aren't already in the list.
                        for (ResourceType folderResType : folderContent) {
                            if (list.contains(folderResType) == false) {
                                list.add(folderResType);
                            }
                        }
                    }
                }
            }
        }

        return list;
    }

    /**
     * Returns a list of {@link ResourceItem} matching a given {@link ResourceType}.
     * @param type the type of the resource items to return
     * @return a non null collection of resource items
     */
    @NonNull
    public Collection<ResourceItem> getResourceItemsOfType(@NonNull ResourceType type) {
        Map<String, ResourceItem> map = mResourceMap.get(type);

        if (map == null) {
            return Collections.emptyList();
        }

        Collection<ResourceItem> roList = mReadOnlyListMap.get(map);
        if (roList == null) {
            roList = Collections.unmodifiableCollection(map.values());
            mReadOnlyListMap.put(map, roList);
        }

        return roList;
    }

    /**
     * Returns whether the repository has resources of a given {@link ResourceType}.
     * @param type the type of resource to check.
     * @return true if the repository contains resources of the given type, false otherwise.
     */
    public boolean hasResourcesOfType(@NonNull ResourceType type) {
        Map<String, ResourceItem> items = mResourceMap.get(type);
        return (items != null && items.size() > 0);
    }

    /**
     * Returns the {@link ResourceFolder} associated with a {@link IAbstractFolder}.
     * @param folder The {@link IAbstractFolder} object.
     * @return the {@link ResourceFolder} or null if it was not found.
     */
    @Nullable
    public ResourceFolder getResourceFolder(@NonNull IAbstractFolder folder) {
        Collection<List<ResourceFolder>> values = mFolderMap.values();

        if (values.isEmpty()) { // This shouldn't be necessary, but has been observed
            try {
                loadResources(folder.getParentFolder());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (List<ResourceFolder> list : values) {
            for (ResourceFolder resFolder : list) {
                IAbstractFolder wrapper = resFolder.getFolder();
                if (wrapper.equals(folder)) {
                    return resFolder;
                }
            }
        }

        return null;
    }

    /**
     * Returns the {@link ResourceFile} matching the given name, {@link ResourceFolderType} and
     * configuration.
     * <p/>This only works with files generating one resource named after the file (for instance,
     * layouts, bitmap based drawable, xml, anims).
     * @return the matching file or <code>null</code> if no match was found.
     */
    @Nullable
    public ResourceFile getMatchingFile(@NonNull String name, @NonNull ResourceFolderType type,
            @NonNull FolderConfiguration config) {
        // get the folders for the given type
        List<ResourceFolder> folders = mFolderMap.get(type);

        // look for folders containing a file with the given name.
        ArrayList<ResourceFolder> matchingFolders = new ArrayList<ResourceFolder>(folders.size());

        // remove the folders that do not have a file with the given name.
        for (int i = 0 ; i < folders.size(); i++) {
            ResourceFolder folder = folders.get(i);

            if (folder.hasFile(name) == true) {
                matchingFolders.add(folder);
            }
        }

        // from those, get the folder with a config matching the given reference configuration.
        Configurable match = config.findMatchingConfigurable(matchingFolders);

        // do we have a matching folder?
        if (match instanceof ResourceFolder) {
            // get the ResourceFile from the filename
            return ((ResourceFolder)match).getFile(name);
        }

        return null;
    }

    /**
     * Returns the list of source files for a given resource.
     * Optionally, if a {@link FolderConfiguration} is given, then only the best
     * match for this config is returned.
     *
     * @param type the type of the resource.
     * @param name the name of the resource.
     * @param referenceConfig an optional config for which only the best match will be returned.
     *
     * @return a list of files generating this resource or null if it was not found.
     */
    @Nullable
    public List<ResourceFile> getSourceFiles(@NonNull ResourceType type, @NonNull String name,
            @Nullable FolderConfiguration referenceConfig) {

        Collection<ResourceItem> items = getResourceItemsOfType(type);

        for (ResourceItem item : items) {
            if (name.equals(item.getName())) {
                if (referenceConfig != null) {
                    Configurable match = referenceConfig.findMatchingConfigurable(
                            item.getSourceFileList());

                    if (match instanceof ResourceFile) {
                        return Collections.singletonList((ResourceFile) match);
                    }

                    return null;
                }
                return item.getSourceFileList();
            }
        }

        return null;
    }

    /**
     * Returns the resources values matching a given {@link FolderConfiguration}.
     *
     * @param referenceConfig the configuration that each value must match.
     * @return a map with guaranteed to contain an entry for each {@link ResourceType}
     */
    @NonNull
    public Map<ResourceType, Map<String, ResourceValue>> getConfiguredResources(
            @NonNull FolderConfiguration referenceConfig) {
        return doGetConfiguredResources(referenceConfig);
    }

    /**
     * Returns the resources values matching a given {@link FolderConfiguration} for the current
     * project.
     *
     * @param referenceConfig the configuration that each value must match.
     * @return a map with guaranteed to contain an entry for each {@link ResourceType}
     */
    @NonNull
    protected final Map<ResourceType, Map<String, ResourceValue>> doGetConfiguredResources(
            @NonNull FolderConfiguration referenceConfig) {

        Map<ResourceType, Map<String, ResourceValue>> map =
            new EnumMap<ResourceType, Map<String, ResourceValue>>(ResourceType.class);

        for (ResourceType key : ResourceType.values()) {
            // get the local results and put them in the map
            map.put(key, getConfiguredResource(key, referenceConfig));
        }

        return map;
    }

    /**
     * Returns the sorted list of languages used in the resources.
     */
    @NonNull
    public SortedSet<String> getLanguages() {
        SortedSet<String> set = new TreeSet<String>();

        Collection<List<ResourceFolder>> folderList = mFolderMap.values();
        for (List<ResourceFolder> folderSubList : folderList) {
            for (ResourceFolder folder : folderSubList) {
                FolderConfiguration config = folder.getConfiguration();
                LanguageQualifier lang = config.getLanguageQualifier();
                if (lang != null) {
                    set.add(lang.getShortDisplayValue());
                }
            }
        }

        return set;
    }

    /**
     * Returns the sorted list of regions used in the resources with the given language.
     * @param currentLanguage the current language the region must be associated with.
     */
    @NonNull
    public SortedSet<String> getRegions(@NonNull String currentLanguage) {
        SortedSet<String> set = new TreeSet<String>();

        Collection<List<ResourceFolder>> folderList = mFolderMap.values();
        for (List<ResourceFolder> folderSubList : folderList) {
            for (ResourceFolder folder : folderSubList) {
                FolderConfiguration config = folder.getConfiguration();

                // get the language
                LanguageQualifier lang = config.getLanguageQualifier();
                if (lang != null && lang.getShortDisplayValue().equals(currentLanguage)) {
                    RegionQualifier region = config.getRegionQualifier();
                    if (region != null) {
                        set.add(region.getShortDisplayValue());
                    }
                }
            }
        }

        return set;
    }

    /**
     * Loads the resources from a resource folder.
     * <p/>
     *
     * @param rootFolder The folder to read the resources from. This is the top level
     * resource folder (res/)
     * @throws IOException
     */
    public void loadResources(@NonNull IAbstractFolder rootFolder)
            throws IOException {
        ScanningContext context = new ScanningContext(this);

        IAbstractResource[] files = rootFolder.listMembers();
        for (IAbstractResource file : files) {
            if (file instanceof IAbstractFolder) {
                IAbstractFolder folder = (IAbstractFolder) file;
                ResourceFolder resFolder = processFolder(folder);

                if (resFolder != null) {
                    // now we process the content of the folder
                    IAbstractResource[] children = folder.listMembers();

                    for (IAbstractResource childRes : children) {
                        if (childRes instanceof IAbstractFile) {
                            resFolder.processFile((IAbstractFile) childRes,
                                    ResourceDeltaKind.ADDED, context);
                        }
                    }
                }
            }
        }
    }


    protected void removeFile(@NonNull Collection<ResourceType> types,
            @NonNull ResourceFile file) {
        for (ResourceType type : types) {
            removeFile(type, file);
        }
    }

    protected void removeFile(@NonNull ResourceType type, @NonNull ResourceFile file) {
        Map<String, ResourceItem> map = mResourceMap.get(type);
        if (map != null) {
            Collection<ResourceItem> values = map.values();
            for (ResourceItem item : values) {
                item.removeFile(file);
            }
        }
    }

    /**
     * Returns a map of (resource name, resource value) for the given {@link ResourceType}.
     * <p/>The values returned are taken from the resource files best matching a given
     * {@link FolderConfiguration}.
     * @param type the type of the resources.
     * @param referenceConfig the configuration to best match.
     */
    @NonNull
    private Map<String, ResourceValue> getConfiguredResource(@NonNull ResourceType type,
            @NonNull FolderConfiguration referenceConfig) {

        // get the resource item for the given type
        Map<String, ResourceItem> items = mResourceMap.get(type);
        if (items == null) {
            return new HashMap<String, ResourceValue>();
        }

        // create the map
        HashMap<String, ResourceValue> map = new HashMap<String, ResourceValue>(items.size());

        for (ResourceItem item : items.values()) {
            ResourceValue value = item.getResourceValue(type, referenceConfig,
                    isFrameworkRepository());
            if (value != null) {
                map.put(item.getName(), value);
            }
        }

        return map;
    }


    /**
     * Cleans up the repository of resource items that have no source file anymore.
     */
    public void postUpdateCleanUp() {
        // Since removed files/folders remove source files from existing ResourceItem, loop through
        // all resource items and remove the ones that have no source files.

        Collection<Map<String, ResourceItem>> maps = mResourceMap.values();
        for (Map<String, ResourceItem> map : maps) {
            Set<String> keySet = map.keySet();
            Iterator<String> iterator = keySet.iterator();
            while (iterator.hasNext()) {
                String name = iterator.next();
                ResourceItem resourceItem = map.get(name);
                if (resourceItem.hasNoSourceFile()) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Looks up an existing {@link ResourceItem} by {@link ResourceType} and name. This
     * ignores inline resources.
     * @param type the Resource Type.
     * @param name the Resource name.
     * @return the existing ResourceItem or null if no match was found.
     */
    @Nullable
    private ResourceItem findDeclaredResourceItem(@NonNull ResourceType type,
            @NonNull String name) {
        Map<String, ResourceItem> map = mResourceMap.get(type);

        if (map != null) {
            ResourceItem resourceItem = map.get(name);
            if (resourceItem != null && !resourceItem.isDeclaredInline()) {
                return resourceItem;
            }
        }

        return null;
    }
}

