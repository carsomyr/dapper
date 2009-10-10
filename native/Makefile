CMAKE			= cmake
CMAKE_TOOLCHAIN	= ../../build/conf/mingw.cmake

#------------------------------------------------------------------------------#
# Declare variables.                                                           #
#------------------------------------------------------------------------------#

# Windows Icons

ICON_PNGS		= $(patsubst src/icon/icon_%.svg, icon_%.png, \
				$(wildcard src/icon/icon_*.svg))
ICON_ICO		= include/win32/icon.ico

# CMake Flags

include ../src/dapper/project.properties

CMAKE_FLAGS		= -DCMAKE_BUILD_TYPE="Release"

ifdef build.version

CMAKE_FLAGS		+= -DVERSION="$(build.version)"

endif

ifdef WORD_SIZE

CMAKE_FLAGS		+= -DM="$(WORD_SIZE)"

endif

ifeq ($(OS), Windows)

CMAKE_FLAGS		+= -DCMAKE_TOOLCHAIN_FILE=$(CMAKE_TOOLCHAIN)

endif

#------------------------------------------------------------------------------#
# Make the high level targets.                                                 #
#------------------------------------------------------------------------------#

.PHONY: all cmake BuildAndTest doxygen clean

all: BuildAndTest

#------------------------------------------------------------------------------#
# Make the native libraries and executables.                                   #
#------------------------------------------------------------------------------#

cmake: $(OS)$(WORD_SIZE)/Makefile

$(OS)$(WORD_SIZE)/Makefile: CMakeLists.txt
	mkdir -p $(OS)$(WORD_SIZE)/
	cd $(OS)$(WORD_SIZE)/ && $(CMAKE) ../ $(CMAKE_FLAGS)

BuildAndTest: cmake $(ICON_ICO)
	$(MAKE) -C $(OS)$(WORD_SIZE)/ BuildAndTest

doxygen: cmake
	$(MAKE) -C $(OS)$(WORD_SIZE)/ doxygen

#------------------------------------------------------------------------------#
# Make the icon stack.                                                         #
#------------------------------------------------------------------------------#

$(ICON_PNGS): icon_%.png: src/icon/icon_%.svg
	inkscape --without-gui \
		--file=$< \
		--export-png=$(CURDIR)/$@ \
		--export-width=$*

$(ICON_ICO): $(ICON_PNGS)
	gimp -i -b - < ../build/conf/icon.scm > /dev/null
	mv icon.ico $(ICON_ICO)

#------------------------------------------------------------------------------#
# Clean the distribution.                                                      #
#------------------------------------------------------------------------------#

clean:
	rm -rf $(OS)$(WORD_SIZE)/
	rm -f *.png $(ICON_ICO)
