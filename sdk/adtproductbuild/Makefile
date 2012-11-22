ADT_IDE_BUILD_LOG := out/host/eclipse/adtproduct/build/adtproduct.log
ADT_IDE_ARTIFACT_DIR := ../../out/host/eclipse/adtproduct/build/I.RcpBuild
ADT_IDE_RELEASE_DIR := ../../out/host/eclipse/adtproduct/release/

define release-ide
    srczip=$(ADT_IDE_ARTIFACT_DIR)/RcpBuild-$(1).$(2).zip && \
    dstdir=$(ADT_IDE_RELEASE_DIR)/android-ide-$(1).$(2) && \
    rm -rf $(V) $$dstdir && \
    mkdir -p $$dstdir && \
    unzip -q $$srczip -d $$dstdir && \
	if [[ $(1) == "macosx.cocoa" ]]; then eclipseini=eclipse.app/Contents/MacOS/eclipse.ini; else eclipseini=eclipse.ini; fi && \
	sed -i 's/org.eclipse.platform/com.android.ide.eclipse.adt.package.product/g' $$dstdir/eclipse/$$eclipseini && \
	sed -i -e 's/org.eclipse.platform.ide/com.android.ide.eclipse.adt.package.product/g' \
		   -e 's/org.eclipse.platform/com.android.ide.eclipse.adt.package/g' $$dstdir/eclipse/configuration/config.ini
endef


all:
	rm -f ../../$(ADT_IDE_BUILD_LOG) && mkdir -p ../../$(dir $(ADT_IDE_BUILD_LOG)) && \
		( java -jar ../../external/eclipse-basebuilder/basebuilder-3.6.2/org.eclipse.releng.basebuilder/plugins/org.eclipse.equinox.launcher_1.1.0.v20100507.jar \
			org.eclipse.equinox.launcher.Main \
			-application org.eclipse.ant.core.antRunner \
			-configuration ../../out/host/eclipse/adtproduct/build/configuration \
			-data ../../out/host/eclipse/adtproduct/workspace \
			2>&1 && \
		  mv -f ../../$(ADT_IDE_BUILD_LOG) ../../$(ADT_IDE_BUILD_LOG).1 ) \
		| tee ../../$(ADT_IDE_BUILD_LOG) \
		| sed '/SUCCESSFUL/d ; /\[java\]/!b label; s/\s\+\[java\]//; /^\s*$$/d; /Compiling/!d; :label /^\s*$$/d; s/^/ADT: /'; \
		if [[ -f ../../$(ADT_IDE_BUILD_LOG) ]]; then \
		  echo "ADT IDE build failed. Full log:" ; \
		  cat ../../$(ADT_IDE_BUILD_LOG) ; \
		  exit 1 ; \
		fi
	$(call release-ide,linux.gtk,x86_64) ; \
	$(call release-ide,macosx.cocoa,x86_64) ; \
	$(call release-ide,win32.win32,x86_64) ; \
