#include <sys/types.h>
#include <unistd.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <errno.h>
#include <pwd.h>
#include <grp.h>
#include <sys/mman.h>
#include <sys/mount.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <selinux/selinux.h>
#include <selinux/context.h>
#include <selinux/android.h>
#include <selinux/label.h>
#include <selinux/avc.h>
#include <private/android_filesystem_config.h>
#include "callbacks.h"
#include "selinux_internal.h"

/*
 * XXX Where should this configuration file be located?
 * Needs to be accessible by zygote and installd when
 * setting credentials for app processes and setting permissions
 * on app data directories.
 */
static char const * const seapp_contexts_file[] = {
	"/data/system/seapp_contexts",
	"/seapp_contexts",
	0 };

static const struct selinux_opt seopts[] = {
	{ SELABEL_OPT_PATH, "/data/system/file_contexts" },
	{ SELABEL_OPT_PATH, "/file_contexts" },
	{ 0, NULL } };

static const char *const sepolicy_file[] = {
        "/data/system/sepolicy",
        "/sepolicy",
        0 };

struct seapp_context {
	/* input selectors */
	char isSystemServer;
	char *user;
	size_t len;
	char prefix;
	char *seinfo;
	char *name;
	/* outputs */
	char *domain;
	char *type;
	char *level;
	char *sebool;
	char levelFromUid;
};

static int seapp_context_cmp(const void *A, const void *B)
{
	const struct seapp_context **sp1 = A, **sp2 = B;
	const struct seapp_context *s1 = *sp1, *s2 = *sp2;

	/* Give precedence to isSystemServer=true. */
	if (s1->isSystemServer != s2->isSystemServer)
		return (s1->isSystemServer ? -1 : 1);

	/* Give precedence to a specified user= over an unspecified user=. */
	if (s1->user && !s2->user)
		return -1;
	if (!s1->user && s2->user)
		return 1;

	if (s1->user) {
		/* Give precedence to a fixed user= string over a prefix. */
		if (s1->prefix != s2->prefix)
			return (s2->prefix ? -1 : 1);

		/* Give precedence to a longer prefix over a shorter prefix. */
		if (s1->prefix && s1->len != s2->len)
			return (s1->len > s2->len) ? -1 : 1;
	}

	/* Give precedence to a specified seinfo= over an unspecified seinfo=. */
	if (s1->seinfo && !s2->seinfo)
		return -1;
	if (!s1->seinfo && s2->seinfo)
		return 1;

	/* Give precedence to a specified name= over an unspecified name=. */
	if (s1->name && !s2->name)
		return -1;
	if (!s1->name && s2->name)
		return 1;

        /* Give precedence to a specified sebool= over an unspecified sebool=. */
        if (s1->sebool && !s2->sebool)
                return -1;
        if (!s1->sebool && s2->sebool)
                return 1;

	/* Anything else has equal precedence. */
	return 0;
}

static struct seapp_context **seapp_contexts = NULL;
static int nspec = 0;

int selinux_android_seapp_context_reload(void)
{
	FILE *fp = NULL;
	char line_buf[BUFSIZ];
	char *token;
	unsigned lineno;
	struct seapp_context *cur;
	char *p, *name = NULL, *value = NULL, *saveptr;
	size_t len;
	int i = 0, ret;

	while ((fp==NULL) && seapp_contexts_file[i])
		fp = fopen(seapp_contexts_file[i++], "r");

	if (!fp) {
		selinux_log(SELINUX_ERROR, "%s:  could not open any seapp_contexts file", __FUNCTION__);
		return -1;
	}

	nspec = 0;
	while (fgets(line_buf, sizeof line_buf - 1, fp)) {
		p = line_buf;
		while (isspace(*p))
			p++;
		if (*p == '#' || *p == 0)
			continue;
		nspec++;
	}

	seapp_contexts = calloc(nspec, sizeof(struct seapp_context *));
	if (!seapp_contexts)
		goto oom;

	rewind(fp);
	nspec = 0;
	lineno = 1;
	while (fgets(line_buf, sizeof line_buf - 1, fp)) {
		len = strlen(line_buf);
		if (line_buf[len - 1] == '\n')
			line_buf[len - 1] = 0;
		p = line_buf;
		while (isspace(*p))
			p++;
		if (*p == '#' || *p == 0)
			continue;

		cur = calloc(1, sizeof(struct seapp_context));
		if (!cur)
			goto oom;

		token = strtok_r(p, " \t", &saveptr);
		if (!token)
			goto err;

		while (1) {
			name = token;
			value = strchr(name, '=');
			if (!value)
				goto err;
			*value++ = 0;

			if (!strcasecmp(name, "isSystemServer")) {
				if (!strcasecmp(value, "true"))
					cur->isSystemServer = 1;
				else if (!strcasecmp(value, "false"))
					cur->isSystemServer = 0;
				else {
					goto err;
				}
			} else if (!strcasecmp(name, "user")) {
				cur->user = strdup(value);
				if (!cur->user)
					goto oom;
				cur->len = strlen(cur->user);
				if (cur->user[cur->len-1] == '*')
					cur->prefix = 1;
			} else if (!strcasecmp(name, "seinfo")) {
				cur->seinfo = strdup(value);
				if (!cur->seinfo)
					goto oom;
			} else if (!strcasecmp(name, "name")) {
				cur->name = strdup(value);
				if (!cur->name)
					goto oom;
			} else if (!strcasecmp(name, "domain")) {
				cur->domain = strdup(value);
				if (!cur->domain)
					goto oom;
			} else if (!strcasecmp(name, "type")) {
				cur->type = strdup(value);
				if (!cur->type)
					goto oom;
			} else if (!strcasecmp(name, "levelFromUid")) {
				if (!strcasecmp(value, "true"))
					cur->levelFromUid = 1;
				else if (!strcasecmp(value, "false"))
					cur->levelFromUid = 0;
				else {
					goto err;
				}
			} else if (!strcasecmp(name, "level")) {
				cur->level = strdup(value);
				if (!cur->level)
					goto oom;
			} else if (!strcasecmp(name, "sebool")) {
				cur->sebool = strdup(value);
				if (!cur->sebool)
					goto oom;
			} else
				goto err;

			token = strtok_r(NULL, " \t", &saveptr);
			if (!token)
				break;
		}

		seapp_contexts[nspec] = cur;
		nspec++;
		lineno++;
	}

	qsort(seapp_contexts, nspec, sizeof(struct seapp_context *),
	      seapp_context_cmp);

#if DEBUG
	{
		int i;
		for (i = 0; i < nspec; i++) {
			cur = seapp_contexts[i];
			selinux_log(SELINUX_INFO, "%s:  isSystemServer=%s user=%s seinfo=%s name=%s sebool=%s -> domain=%s type=%s level=%s levelFromUid=%s",
			__FUNCTION__,
			cur->isSystemServer ? "true" : "false", cur->user,
			cur->seinfo, cur->name, cur->sebool, cur->domain,
			cur->type, cur->level,
			cur->levelFromUid ? "true" : "false");
		}
	}
#endif

	ret = 0;

out:
	fclose(fp);
	return ret;

err:
	selinux_log(SELINUX_ERROR, "%s:  Error reading %s, line %u, name %s, value %s\n",
		    __FUNCTION__, seapp_contexts_file[i - 1], lineno, name, value);
	ret = -1;
	goto out;
oom:
	selinux_log(SELINUX_ERROR, 
		    "%s:  Out of memory\n", __FUNCTION__);
	ret = -1;
	goto out;
}


static void seapp_context_init(void)
{
        selinux_android_seapp_context_reload();
}

static pthread_once_t once = PTHREAD_ONCE_INIT;

#define SEAPP_TYPE 1
#define SEAPP_DOMAIN 2
static int seapp_context_lookup(int kind,
				uid_t uid,
				int isSystemServer,
				const char *seinfo,
				const char *pkgname,
				context_t ctx)
{
	const char *username = NULL;
	char *end = NULL;
	struct passwd *pw;
	struct seapp_context *cur;
	int i;
	size_t n;
	uid_t appid = 0;

	appid = uid % AID_USER;
	if (appid < AID_APP) {
		for (n = 0; n < android_id_count; n++) {
			if (android_ids[n].aid == appid) {
				username = android_ids[n].name;
				break;
			}
		}
		if (!username)
			goto err;
	} else if (appid < AID_ISOLATED_START) {
		username = "app_";
		appid -= AID_APP;
	} else {
		username = "isolated";
		appid -= AID_ISOLATED_START;
	}

	if (appid >= MLS_CATS)
		goto err;

	for (i = 0; i < nspec; i++) {
		cur = seapp_contexts[i];

		if (cur->isSystemServer != isSystemServer)
			continue;

		if (cur->user) {
			if (cur->prefix) {
				if (strncasecmp(username, cur->user, cur->len-1))
					continue;
			} else {
				if (strcasecmp(username, cur->user))
					continue;
			}
		}

		if (cur->seinfo) {
			if (!seinfo || strcasecmp(seinfo, cur->seinfo))
				continue;
		}

		if (cur->name) {
			if (!pkgname || strcasecmp(pkgname, cur->name))
				continue;
		}

		if (kind == SEAPP_TYPE && !cur->type)
			continue;
		else if (kind == SEAPP_DOMAIN && !cur->domain)
			continue;

		if (cur->sebool) {
			int value = security_get_boolean_active(cur->sebool);
			if (value == 0)
				continue;
			else if (value == -1) {
				selinux_log(SELINUX_ERROR, \
				"Could not find boolean: %s ", cur->sebool);
				goto err;
			}
		}

		if (kind == SEAPP_TYPE) {
			if (context_type_set(ctx, cur->type))
				goto oom;
		} else if (kind == SEAPP_DOMAIN) {
			if (context_type_set(ctx, cur->domain))
				goto oom;
		}

		if (cur->levelFromUid) {
			char level[255];
			snprintf(level, sizeof level, "%s:c%lu",
				 context_range_get(ctx), appid);
			if (context_range_set(ctx, level))
				goto oom;
		} else if (cur->level) {
			if (context_range_set(ctx, cur->level))
				goto oom;
		}

		break;
	}

	if (kind == SEAPP_DOMAIN && i == nspec) {
		/*
		 * No match.
		 * Fail to prevent staying in the zygote's context.
		 */
		selinux_log(SELINUX_ERROR,
			    "%s:  No match for app with uid %d, seinfo %s, name %s\n",
			    __FUNCTION__, uid, seinfo, pkgname);

		if (security_getenforce() == 1)
			goto err;
	}

	return 0;
err:
	return -1;
oom:
	return -2;
}

int selinux_android_setfilecon2(const char *pkgdir,
				const char *pkgname,
				const char *seinfo,
				uid_t uid)
{
	char *orig_ctx_str = NULL, *ctx_str;
	context_t ctx = NULL;
	int rc;

	if (is_selinux_enabled() <= 0)
		return 0;

	__selinux_once(once, seapp_context_init);

	rc = getfilecon(pkgdir, &ctx_str);
	if (rc < 0)
		goto err;

	ctx = context_new(ctx_str);
	orig_ctx_str = ctx_str;
	if (!ctx)
		goto oom;

	rc = seapp_context_lookup(SEAPP_TYPE, uid, 0, seinfo, pkgname, ctx);
	if (rc == -1)
		goto err;
	else if (rc == -2)
		goto oom;

	ctx_str = context_str(ctx);
	if (!ctx_str)
		goto oom;

	rc = security_check_context(ctx_str);
	if (rc < 0)
		goto err;

	if (strcmp(ctx_str, orig_ctx_str)) {
		rc = setfilecon(pkgdir, ctx_str);
		if (rc < 0)
			goto err;
	}

	rc = 0;
out:
	freecon(orig_ctx_str);
	context_free(ctx);
	return rc;
err:
	selinux_log(SELINUX_ERROR, "%s:  Error setting context for pkgdir %s, uid %d: %s\n",
		    __FUNCTION__, pkgdir, uid, strerror(errno));
	rc = -1;
	goto out;
oom:
	selinux_log(SELINUX_ERROR, "%s:  Out of memory\n", __FUNCTION__);
	rc = -1;
	goto out;
}

int selinux_android_setfilecon(const char *pkgdir,
			       const char *pkgname,
			       uid_t uid)
{
	return selinux_android_setfilecon2(pkgdir, pkgname, NULL, uid);
}

int selinux_android_setcontext(uid_t uid,
			       int isSystemServer,
			       const char *seinfo,
			       const char *pkgname)
{
	char *orig_ctx_str = NULL, *ctx_str;
	context_t ctx = NULL;
	int rc;

	if (is_selinux_enabled() <= 0)
		return 0;

	__selinux_once(once, seapp_context_init);
	
	rc = getcon(&ctx_str);
	if (rc)
		goto err;

	ctx = context_new(ctx_str);
	orig_ctx_str = ctx_str;
	if (!ctx)
		goto oom;

	rc = seapp_context_lookup(SEAPP_DOMAIN, uid, isSystemServer, seinfo, pkgname, ctx);
	if (rc == -1)
		goto err;
	else if (rc == -2)
		goto oom;

	ctx_str = context_str(ctx);
	if (!ctx_str)
		goto oom;

	rc = security_check_context(ctx_str);
	if (rc < 0)
		goto err;

	if (strcmp(ctx_str, orig_ctx_str)) {
		rc = setcon(ctx_str);
		if (rc < 0)
			goto err;
	}

	rc = 0;
out:
	freecon(orig_ctx_str);
	context_free(ctx);
	avc_netlink_close();
	return rc;
err:
	if (isSystemServer)
		selinux_log(SELINUX_ERROR,
			    "%s:  Error setting context for system server: %s\n",
			    __FUNCTION__, strerror(errno));
	else 
		selinux_log(SELINUX_ERROR,
			    "%s:  Error setting context for app with uid %d, seinfo %s: %s\n",
			    __FUNCTION__, uid, seinfo, strerror(errno));

	rc = -1;
	goto out;
oom:
	selinux_log(SELINUX_ERROR, "%s:  Out of memory\n", __FUNCTION__);
	rc = -1;
	goto out;
}

static struct selabel_handle *sehandle = NULL;

static struct selabel_handle *file_context_open(void)
{
	struct selabel_handle *h;
	int i = 0;

	h = NULL;
	while ((h == NULL) && seopts[i].value) {
		h = selabel_open(SELABEL_CTX_FILE, &seopts[i], 1);
		i++;
	}

	if (!h)
		selinux_log(SELINUX_ERROR, "%s: Error getting sehandle label (%s)\n",
			    __FUNCTION__, strerror(errno));
	return h;
}

static void file_context_init(void)
{
	sehandle = file_context_open();
}

static pthread_once_t fc_once = PTHREAD_ONCE_INIT;

int selinux_android_restorecon(const char *pathname)
{

	__selinux_once(fc_once, file_context_init);

	int ret;

	if (!sehandle)
		goto bail;

	struct stat sb;

	if (lstat(pathname, &sb) < 0)
		goto err;

	char *oldcontext, *newcontext;

	if (lgetfilecon(pathname, &oldcontext) < 0)
		goto err;

	if (selabel_lookup(sehandle, &newcontext, pathname, sb.st_mode) < 0)
		goto err;

	if (strcmp(newcontext, "<<none>>") && strcmp(oldcontext, newcontext))
		if (lsetfilecon(pathname, newcontext) < 0)
			goto err;

	ret = 0;
out:
	if (oldcontext)
		freecon(oldcontext);
	if (newcontext)
		freecon(newcontext);

	return ret;

err:
	selinux_log(SELINUX_ERROR,
		    "%s:  Error restoring context for %s (%s)\n",
		    __FUNCTION__, pathname, strerror(errno));

bail:
	ret = -1;
	goto out;
}


struct selabel_handle* selinux_android_file_context_handle(void)
{
        return file_context_open();
}

int selinux_android_reload_policy(void)
{
	char path[PATH_MAX];
	int fd = -1, rc;
	struct stat sb;
	void *map = NULL;
	int i = 0;

	while (fd < 0 && sepolicy_file[i]) {
		snprintf(path, sizeof(path), "%s",
			sepolicy_file[i]);
		fd = open(path, O_RDONLY);
		i++;
	}
	if (fd < 0) {
		selinux_log(SELINUX_ERROR, "SELinux:  Could not open sepolicy:  %s\n",
				strerror(errno));
		return -1;
	}
	if (fstat(fd, &sb) < 0) {
		selinux_log(SELINUX_ERROR, "SELinux:  Could not stat %s:  %s\n",
				path, strerror(errno));
		close(fd);
		return -1;
	}
	map = mmap(NULL, sb.st_size, PROT_READ, MAP_PRIVATE, fd, 0);
	if (map == MAP_FAILED) {
		selinux_log(SELINUX_ERROR, "SELinux:  Could not map %s:  %s\n",
			path, strerror(errno));
		close(fd);
		return -1;
	}

	rc = security_load_policy(map, sb.st_size);
	if (rc < 0) {
		selinux_log(SELINUX_ERROR, "SELinux:  Could not load policy:  %s\n",
			strerror(errno));
		munmap(map, sb.st_size);
		close(fd);
		return -1;
	}

	munmap(map, sb.st_size);
	close(fd);
	selinux_log(SELINUX_INFO, "SELinux: Loaded policy from %s\n", path);

	return 0;
}

int selinux_android_load_policy(void)
{
	mkdir(SELINUXMNT, 0755);
	if (mount("selinuxfs", SELINUXMNT, "selinuxfs", 0, NULL)) {
		if (errno == ENODEV) {
			/* SELinux not enabled in kernel */
			return -1;
		}
		selinux_log(SELINUX_ERROR,"SELinux:  Could not mount selinuxfs:  %s\n",
				strerror(errno));
		return -1;
	}
	set_selinuxmnt(SELINUXMNT);

	return selinux_android_reload_policy();
}
