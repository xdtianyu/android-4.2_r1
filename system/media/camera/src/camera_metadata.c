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
#define _GNU_SOURCE // for fdprintf
#include <system/camera_metadata.h>
#include <cutils/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>

#define OK         0
#define ERROR      1
#define NOT_FOUND -ENOENT
/**
 * A single metadata entry, storing an array of values of a given type. If the
 * array is no larger than 4 bytes in size, it is stored in the data.value[]
 * array; otherwise, it can found in the parent's data array at index
 * data.offset.
 */
typedef struct camera_metadata_buffer_entry {
    uint32_t tag;
    size_t   count;
    union {
        size_t  offset;
        uint8_t value[4];
    } data;
    uint8_t  type;
    uint8_t  reserved[3];
} __attribute__((packed)) camera_metadata_buffer_entry_t;

/**
 * A packet of metadata. This is a list of entries, each of which may point to
 * its values stored at an offset in data.
 *
 * It is assumed by the utility functions that the memory layout of the packet
 * is as follows:
 *
 *   |-----------------------------------------------|
 *   | camera_metadata_t                             |
 *   |                                               |
 *   |-----------------------------------------------|
 *   | reserved for future expansion                 |
 *   |-----------------------------------------------|
 *   | camera_metadata_buffer_entry_t #0             |
 *   |-----------------------------------------------|
 *   | ....                                          |
 *   |-----------------------------------------------|
 *   | camera_metadata_buffer_entry_t #entry_count-1 |
 *   |-----------------------------------------------|
 *   | free space for                                |
 *   | (entry_capacity-entry_count) entries          |
 *   |-----------------------------------------------|
 *   | start of camera_metadata.data                 |
 *   |                                               |
 *   |-----------------------------------------------|
 *   | free space for                                |
 *   | (data_capacity-data_count) bytes              |
 *   |-----------------------------------------------|
 *
 * With the total length of the whole packet being camera_metadata.size bytes.
 *
 * In short, the entries and data are contiguous in memory after the metadata
 * header.
 */
struct camera_metadata {
    size_t                   size;
    uint32_t                 version;
    uint32_t                 flags;
    size_t                   entry_count;
    size_t                   entry_capacity;
    camera_metadata_buffer_entry_t *entries;
    size_t                   data_count;
    size_t                   data_capacity;
    uint8_t                 *data;
    void                    *user; // User set pointer, not copied with buffer
    uint8_t                  reserved[0];
};

/** Versioning information */
#define CURRENT_METADATA_VERSION 1

/** Flag definitions */
#define FLAG_SORTED 0x00000001

/** Tag information */

typedef struct tag_info {
    const char *tag_name;
    uint8_t     tag_type;
} tag_info_t;

#include "camera_metadata_tag_info.c"

const size_t camera_metadata_type_size[NUM_TYPES] = {
    [TYPE_BYTE]     = sizeof(uint8_t),
    [TYPE_INT32]    = sizeof(int32_t),
    [TYPE_FLOAT]    = sizeof(float),
    [TYPE_INT64]    = sizeof(int64_t),
    [TYPE_DOUBLE]   = sizeof(double),
    [TYPE_RATIONAL] = sizeof(camera_metadata_rational_t)
};

const char *camera_metadata_type_names[NUM_TYPES] = {
    [TYPE_BYTE]     = "byte",
    [TYPE_INT32]    = "int32",
    [TYPE_FLOAT]    = "float",
    [TYPE_INT64]    = "int64",
    [TYPE_DOUBLE]   = "double",
    [TYPE_RATIONAL] = "rational"
};

camera_metadata_t *allocate_camera_metadata(size_t entry_capacity,
                                            size_t data_capacity) {
    if (entry_capacity == 0) return NULL;

    size_t memory_needed = calculate_camera_metadata_size(entry_capacity,
                                                          data_capacity);
    void *buffer = malloc(memory_needed);
    return place_camera_metadata(buffer, memory_needed,
                                 entry_capacity,
                                 data_capacity);
}

camera_metadata_t *place_camera_metadata(void *dst,
                                         size_t dst_size,
                                         size_t entry_capacity,
                                         size_t data_capacity) {
    if (dst == NULL) return NULL;
    if (entry_capacity == 0) return NULL;

    size_t memory_needed = calculate_camera_metadata_size(entry_capacity,
                                                          data_capacity);
    if (memory_needed > dst_size) return NULL;

    camera_metadata_t *metadata = (camera_metadata_t*)dst;
    metadata->version = CURRENT_METADATA_VERSION;
    metadata->flags = 0;
    metadata->entry_count = 0;
    metadata->entry_capacity = entry_capacity;
    metadata->entries = (camera_metadata_buffer_entry_t*)(metadata + 1);
    metadata->data_count = 0;
    metadata->data_capacity = data_capacity;
    metadata->size = memory_needed;
    if (metadata->data_capacity != 0) {
        metadata->data =
                (uint8_t*)(metadata->entries + metadata->entry_capacity);
    } else {
        metadata->data = NULL;
    }
    metadata->user = NULL;

    return metadata;
}
void free_camera_metadata(camera_metadata_t *metadata) {
    free(metadata);
}

size_t calculate_camera_metadata_size(size_t entry_count,
                                      size_t data_count) {
    size_t memory_needed = sizeof(camera_metadata_t);
    memory_needed += sizeof(camera_metadata_buffer_entry_t[entry_count]);
    memory_needed += sizeof(uint8_t[data_count]);
    return memory_needed;
}

size_t get_camera_metadata_size(const camera_metadata_t *metadata) {
    if (metadata == NULL) return ERROR;

    return metadata->size;
}

size_t get_camera_metadata_compact_size(const camera_metadata_t *metadata) {
    if (metadata == NULL) return ERROR;

    ptrdiff_t reserved_size = metadata->size -
            calculate_camera_metadata_size(metadata->entry_capacity,
                                           metadata->data_capacity);

    return calculate_camera_metadata_size(metadata->entry_count,
                                          metadata->data_count) + reserved_size;
}

size_t get_camera_metadata_entry_count(const camera_metadata_t *metadata) {
    return metadata->entry_count;
}

size_t get_camera_metadata_entry_capacity(const camera_metadata_t *metadata) {
    return metadata->entry_capacity;
}

size_t get_camera_metadata_data_count(const camera_metadata_t *metadata) {
    return metadata->data_count;
}

size_t get_camera_metadata_data_capacity(const camera_metadata_t *metadata) {
    return metadata->data_capacity;
}

camera_metadata_t* copy_camera_metadata(void *dst, size_t dst_size,
        const camera_metadata_t *src) {
    size_t memory_needed = get_camera_metadata_compact_size(src);

    if (dst == NULL) return NULL;
    if (dst_size < memory_needed) return NULL;

    // If copying a newer version of the structure, there may be additional
    // header fields we don't know about but need to copy
    ptrdiff_t reserved_size = src->size -
            calculate_camera_metadata_size(src->entry_capacity,
                                           src->data_capacity);

    camera_metadata_t *metadata = (camera_metadata_t*)dst;
    metadata->version = CURRENT_METADATA_VERSION;
    metadata->flags = src->flags;
    metadata->entry_count = src->entry_count;
    metadata->entry_capacity = src->entry_count;
    metadata->entries = (camera_metadata_buffer_entry_t*)
             ((uint8_t *)(metadata + 1) + reserved_size);
    metadata->data_count = src->data_count;
    metadata->data_capacity = src->data_count;
    metadata->data = (uint8_t *)(metadata->entries + metadata->entry_capacity);
    metadata->size = memory_needed;

    if (reserved_size > 0) {
        memcpy(metadata->reserved, src->reserved, reserved_size);
    }
    memcpy(metadata->entries, src->entries,
            sizeof(camera_metadata_buffer_entry_t[metadata->entry_count]));
    memcpy(metadata->data, src->data,
            sizeof(uint8_t[metadata->data_count]));
    metadata->user = NULL;

    return metadata;
}

int append_camera_metadata(camera_metadata_t *dst,
        const camera_metadata_t *src) {
    if (dst == NULL || src == NULL ) return ERROR;

    if (dst->entry_capacity < src->entry_count + dst->entry_count) return ERROR;
    if (dst->data_capacity < src->data_count + dst->data_count) return ERROR;

    memcpy(dst->entries + dst->entry_count, src->entries,
            sizeof(camera_metadata_buffer_entry_t[src->entry_count]));
    memcpy(dst->data + dst->data_count, src->data,
            sizeof(uint8_t[src->data_count]));
    if (dst->data_count != 0) {
        unsigned int i;
        for (i = dst->entry_count;
             i < dst->entry_count + src->entry_count;
             i++) {
            camera_metadata_buffer_entry_t *entry = dst->entries + i;
            if ( camera_metadata_type_size[entry->type] * entry->count > 4 ) {
                entry->data.offset += dst->data_count;
            }
        }
    }
    if (dst->entry_count == 0) {
        // Appending onto empty buffer, keep sorted state
        dst->flags |= src->flags & FLAG_SORTED;
    } else if (src->entry_count != 0) {
        // Both src, dst are nonempty, cannot assume sort remains
        dst->flags &= ~FLAG_SORTED;
    } else {
        // Src is empty, keep dst sorted state
    }
    dst->entry_count += src->entry_count;
    dst->data_count += src->data_count;

    return OK;
}

camera_metadata_t *clone_camera_metadata(const camera_metadata_t *src) {
    int res;
    if (src == NULL) return NULL;
    camera_metadata_t *clone = allocate_camera_metadata(
        get_camera_metadata_entry_count(src),
        get_camera_metadata_data_count(src));
    if (clone != NULL) {
        res = append_camera_metadata(clone, src);
        if (res != OK) {
            free_camera_metadata(clone);
            clone = NULL;
        }
    }
    return clone;
}

size_t calculate_camera_metadata_entry_data_size(uint8_t type,
        size_t data_count) {
    if (type >= NUM_TYPES) return 0;
    size_t data_bytes = data_count *
            camera_metadata_type_size[type];
    return data_bytes <= 4 ? 0 : data_bytes;
}

static int add_camera_metadata_entry_raw(camera_metadata_t *dst,
        uint32_t tag,
        uint8_t  type,
        const void *data,
        size_t data_count) {

    if (dst == NULL) return ERROR;
    if (dst->entry_count == dst->entry_capacity) return ERROR;
    if (data == NULL) return ERROR;

    size_t data_bytes =
            calculate_camera_metadata_entry_data_size(type, data_count);

    camera_metadata_buffer_entry_t *entry = dst->entries + dst->entry_count;
    entry->tag = tag;
    entry->type = type;
    entry->count = data_count;

    if (data_bytes == 0) {
        memcpy(entry->data.value, data,
                data_count * camera_metadata_type_size[type] );
    } else {
        entry->data.offset = dst->data_count;
        memcpy(dst->data + entry->data.offset, data, data_bytes);
        dst->data_count += data_bytes;
    }
    dst->entry_count++;
    dst->flags &= ~FLAG_SORTED;
    return OK;
}

int add_camera_metadata_entry(camera_metadata_t *dst,
        uint32_t tag,
        const void *data,
        size_t data_count) {

    int type = get_camera_metadata_tag_type(tag);
    if (type == -1) {
        ALOGE("%s: Unknown tag %04x.", __FUNCTION__, tag);
        return ERROR;
    }

    return add_camera_metadata_entry_raw(dst,
            tag,
            type,
            data,
            data_count);
}

static int compare_entry_tags(const void *p1, const void *p2) {
    uint32_t tag1 = ((camera_metadata_buffer_entry_t*)p1)->tag;
    uint32_t tag2 = ((camera_metadata_buffer_entry_t*)p2)->tag;
    return  tag1 < tag2 ? -1 :
            tag1 == tag2 ? 0 :
            1;
}

int sort_camera_metadata(camera_metadata_t *dst) {
    if (dst == NULL) return ERROR;
    if (dst->flags & FLAG_SORTED) return OK;

    qsort(dst->entries, dst->entry_count,
            sizeof(camera_metadata_buffer_entry_t),
            compare_entry_tags);
    dst->flags |= FLAG_SORTED;

    return OK;
}

int get_camera_metadata_entry(camera_metadata_t *src,
        size_t index,
        camera_metadata_entry_t *entry) {
    if (src == NULL || entry == NULL) return ERROR;
    if (index >= src->entry_count) return ERROR;

    camera_metadata_buffer_entry_t *buffer_entry = src->entries + index;

    entry->index = index;
    entry->tag = buffer_entry->tag;
    entry->type = buffer_entry->type;
    entry->count = buffer_entry->count;
    if (buffer_entry->count *
            camera_metadata_type_size[buffer_entry->type] > 4) {
        entry->data.u8 = src->data + buffer_entry->data.offset;
    } else {
        entry->data.u8 = buffer_entry->data.value;
    }
    return OK;
}

int find_camera_metadata_entry(camera_metadata_t *src,
        uint32_t tag,
        camera_metadata_entry_t *entry) {
    if (src == NULL) return ERROR;

    uint32_t index;
    if (src->flags & FLAG_SORTED) {
        // Sorted entries, do a binary search
        camera_metadata_buffer_entry_t *search_entry = NULL;
        camera_metadata_buffer_entry_t key;
        key.tag = tag;
        search_entry = bsearch(&key,
                src->entries,
                src->entry_count,
                sizeof(camera_metadata_buffer_entry_t),
                compare_entry_tags);
        if (search_entry == NULL) return NOT_FOUND;
        index = search_entry - src->entries;
    } else {
        // Not sorted, linear search
        for (index = 0; index < src->entry_count; index++) {
            if (src->entries[index].tag == tag) {
                break;
            }
        }
        if (index == src->entry_count) return NOT_FOUND;
    }

    return get_camera_metadata_entry(src, index,
            entry);
}

int find_camera_metadata_ro_entry(const camera_metadata_t *src,
        uint32_t tag,
        camera_metadata_ro_entry_t *entry) {
    return find_camera_metadata_entry((camera_metadata_t*)src, tag,
            (camera_metadata_entry_t*)entry);
}


int delete_camera_metadata_entry(camera_metadata_t *dst,
        size_t index) {
    if (dst == NULL) return ERROR;
    if (index >= dst->entry_count) return ERROR;

    camera_metadata_buffer_entry_t *entry = dst->entries + index;
    size_t data_bytes = calculate_camera_metadata_entry_data_size(entry->type,
            entry->count);

    if (data_bytes > 0) {
        // Shift data buffer to overwrite deleted data
        uint8_t *start = dst->data + entry->data.offset;
        uint8_t *end = start + data_bytes;
        size_t length = dst->data_count - entry->data.offset - data_bytes;
        memmove(start, end, length);

        // Update all entry indices to account for shift
        camera_metadata_buffer_entry_t *e = dst->entries;
        size_t i;
        for (i = 0; i < dst->entry_count; i++) {
            if (calculate_camera_metadata_entry_data_size(
                    e->type, e->count) > 0 &&
                    e->data.offset > entry->data.offset) {
                e->data.offset -= data_bytes;
            }
            ++e;
        }
        dst->data_count -= data_bytes;
    }
    // Shift entry array
    memmove(entry, entry + 1,
            sizeof(camera_metadata_buffer_entry_t) *
            (dst->entry_count - index - 1) );
    dst->entry_count -= 1;

    return OK;
}

int update_camera_metadata_entry(camera_metadata_t *dst,
        size_t index,
        const void *data,
        size_t data_count,
        camera_metadata_entry_t *updated_entry) {
    if (dst == NULL) return ERROR;
    if (index >= dst->entry_count) return ERROR;

    camera_metadata_buffer_entry_t *entry = dst->entries + index;

    size_t data_bytes =
            calculate_camera_metadata_entry_data_size(entry->type,
                    data_count);
    size_t entry_bytes =
            calculate_camera_metadata_entry_data_size(entry->type,
                    entry->count);
    if (data_bytes != entry_bytes) {
        // May need to shift/add to data array
        if (dst->data_capacity < dst->data_count + data_bytes - entry_bytes) {
            // No room
            return ERROR;
        }
        if (entry_bytes != 0) {
            // Remove old data
            uint8_t *start = dst->data + entry->data.offset;
            uint8_t *end = start + entry_bytes;
            size_t length = dst->data_count - entry->data.offset - entry_bytes;
            memmove(start, end, length);
            dst->data_count -= entry_bytes;

            // Update all entry indices to account for shift
            camera_metadata_buffer_entry_t *e = dst->entries;
            size_t i;
            for (i = 0; i < dst->entry_count; i++) {
                if (calculate_camera_metadata_entry_data_size(
                        e->type, e->count) > 0 &&
                        e->data.offset > entry->data.offset) {
                    e->data.offset -= entry_bytes;
                }
                ++e;
            }
        }

        if (data_bytes != 0) {
            // Append new data
            entry->data.offset = dst->data_count;

            memcpy(dst->data + entry->data.offset, data, data_bytes);
            dst->data_count += data_bytes;
        }
    } else if (data_bytes != 0) {
        // data size unchanged, reuse same data location
        memcpy(dst->data + entry->data.offset, data, data_bytes);
    }

    if (data_bytes == 0) {
        // Data fits into entry
        memcpy(entry->data.value, data,
                data_count * camera_metadata_type_size[entry->type]);
    }

    entry->count = data_count;

    if (updated_entry != NULL) {
        get_camera_metadata_entry(dst,
                index,
                updated_entry);
    }

    return OK;
}

int set_camera_metadata_user_pointer(camera_metadata_t *dst, void* user) {
    if (dst == NULL) return ERROR;
    dst->user = user;
    return OK;
}

int get_camera_metadata_user_pointer(camera_metadata_t *dst, void** user) {
    if (dst == NULL) return ERROR;
    *user = dst->user;
    return OK;
}

static const vendor_tag_query_ops_t *vendor_tag_ops = NULL;

const char *get_camera_metadata_section_name(uint32_t tag) {
    uint32_t tag_section = tag >> 16;
    if (tag_section >= VENDOR_SECTION && vendor_tag_ops != NULL) {
        return vendor_tag_ops->get_camera_vendor_section_name(
            vendor_tag_ops,
            tag);
    }
    if (tag_section >= ANDROID_SECTION_COUNT) {
        return NULL;
    }
    return camera_metadata_section_names[tag_section];
}

const char *get_camera_metadata_tag_name(uint32_t tag) {
    uint32_t tag_section = tag >> 16;
    if (tag_section >= VENDOR_SECTION && vendor_tag_ops != NULL) {
        return vendor_tag_ops->get_camera_vendor_tag_name(
            vendor_tag_ops,
            tag);
    }
    if (tag_section >= ANDROID_SECTION_COUNT ||
        tag >= camera_metadata_section_bounds[tag_section][1] ) {
        return NULL;
    }
    uint32_t tag_index = tag & 0xFFFF;
    return tag_info[tag_section][tag_index].tag_name;
}

int get_camera_metadata_tag_type(uint32_t tag) {
    uint32_t tag_section = tag >> 16;
    if (tag_section >= VENDOR_SECTION && vendor_tag_ops != NULL) {
        return vendor_tag_ops->get_camera_vendor_tag_type(
            vendor_tag_ops,
            tag);
    }
    if (tag_section >= ANDROID_SECTION_COUNT ||
            tag >= camera_metadata_section_bounds[tag_section][1] ) {
        return -1;
    }
    uint32_t tag_index = tag & 0xFFFF;
    return tag_info[tag_section][tag_index].tag_type;
}

int set_camera_metadata_vendor_tag_ops(const vendor_tag_query_ops_t *query_ops) {
    vendor_tag_ops = query_ops;
    return OK;
}

static void print_data(int fd, const uint8_t *data_ptr, int type, int count,
        int indentation);

void dump_camera_metadata(const camera_metadata_t *metadata,
        int fd,
        int verbosity) {
    dump_indented_camera_metadata(metadata, fd, verbosity, 0);
}

void dump_indented_camera_metadata(const camera_metadata_t *metadata,
        int fd,
        int verbosity,
        int indentation) {
    if (metadata == NULL) {
        fdprintf(fd, "%*sDumping camera metadata array: Not allocated\n",
                indentation, "");
        return;
    }
    unsigned int i;
    fdprintf(fd,
            "%*sDumping camera metadata array: %d / %d entries, "
            "%d / %d bytes of extra data.\n", indentation, "",
            metadata->entry_count, metadata->entry_capacity,
            metadata->data_count, metadata->data_capacity);
    fdprintf(fd, "%*sVersion: %d, Flags: %08x\n",
            indentation + 2, "",
            metadata->version, metadata->flags);
    for (i=0; i < metadata->entry_count; i++) {
        camera_metadata_buffer_entry_t *entry = metadata->entries + i;

        const char *tag_name, *tag_section;
        tag_section = get_camera_metadata_section_name(entry->tag);
        if (tag_section == NULL) {
            tag_section = "unknownSection";
        }
        tag_name = get_camera_metadata_tag_name(entry->tag);
        if (tag_name == NULL) {
            tag_name = "unknownTag";
        }
        const char *type_name;
        if (entry->type >= NUM_TYPES) {
            type_name = "unknown";
        } else {
            type_name = camera_metadata_type_names[entry->type];
        }
        fdprintf(fd, "%*s%s.%s (%05x): %s[%d]\n",
             indentation + 2, "",
             tag_section,
             tag_name,
             entry->tag,
             type_name,
             entry->count);

        if (verbosity < 1) continue;

        if (entry->type >= NUM_TYPES) continue;

        size_t type_size = camera_metadata_type_size[entry->type];
        uint8_t *data_ptr;
        if ( type_size * entry->count > 4 ) {
            if (entry->data.offset >= metadata->data_count) {
                ALOGE("%s: Malformed entry data offset: %d (max %d)",
                        __FUNCTION__,
                        entry->data.offset,
                        metadata->data_count);
                continue;
            }
            data_ptr = metadata->data + entry->data.offset;
        } else {
            data_ptr = entry->data.value;
        }
        int count = entry->count;
        if (verbosity < 2 && count > 16) count = 16;

        print_data(fd, data_ptr, entry->type, count, indentation);
    }
}

static void print_data(int fd, const uint8_t *data_ptr,
        int type, int count, int indentation) {
    static int values_per_line[NUM_TYPES] = {
        [TYPE_BYTE]     = 16,
        [TYPE_INT32]    = 4,
        [TYPE_FLOAT]    = 8,
        [TYPE_INT64]    = 2,
        [TYPE_DOUBLE]   = 4,
        [TYPE_RATIONAL] = 2,
    };
    size_t type_size = camera_metadata_type_size[type];

    int lines = count / values_per_line[type];
    if (count % values_per_line[type] != 0) lines++;

    int index = 0;
    int j, k;
    for (j = 0; j < lines; j++) {
        fdprintf(fd, "%*s[", indentation + 4, "");
        for (k = 0;
             k < values_per_line[type] && count > 0;
             k++, count--, index += type_size) {

            switch (type) {
                case TYPE_BYTE:
                    fdprintf(fd, "%hhu ",
                            *(data_ptr + index));
                    break;
                case TYPE_INT32:
                    fdprintf(fd, "%d ",
                            *(int32_t*)(data_ptr + index));
                    break;
                case TYPE_FLOAT:
                    fdprintf(fd, "%0.2f ",
                            *(float*)(data_ptr + index));
                    break;
                case TYPE_INT64:
                    fdprintf(fd, "%lld ",
                            *(int64_t*)(data_ptr + index));
                    break;
                case TYPE_DOUBLE:
                    fdprintf(fd, "%0.2f ",
                            *(float*)(data_ptr + index));
                    break;
                case TYPE_RATIONAL: {
                    int32_t numerator = *(int32_t*)(data_ptr + index);
                    int32_t denominator = *(int32_t*)(data_ptr + index + 4);
                    fdprintf(fd, "(%d / %d) ",
                            numerator, denominator);
                    break;
                }
                default:
                    fdprintf(fd, "??? ");
            }
        }
        fdprintf(fd, "]\n");
    }
}
