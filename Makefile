include build/*.mk

#------------------------------------------------------------------------------#
# Declare variables.                                                           #
#------------------------------------------------------------------------------#

ANT					= java -cp 'build/ant-launcher.jar' \
					org.apache.tools.ant.launch.Launcher

# Java Sources

JSRCS				= $(wildcard src*/*/*.java) \
					$(wildcard src*/*/*/*.java) \
					$(wildcard src*/*/*/*/*.java)

# C++ Headers, Sources, and Objects

CSRCS				= $(wildcard native/src/*/*.cpp)
CHEADERS			= $(wildcard native/include/*/*.hpp)

# Tokens

BUILD_TOKEN			= .bin
CHECKSTYLE_TOKEN	= .checkstyle
DOXYDOC_TOKEN		= .doxydoc
JAVADOC_TOKEN		= .javadoc
PUBLISH_TOKEN		= .publish

# Macros

LIB_DIR				= native/$(OS)$(WORD_SIZE)

MAKE_BUILD_AND_TEST	= \
	$(MAKE) -C native buildandtest \
	&& cp $(LIB_DIR)/buildandtest.exe .

#------------------------------------------------------------------------------#
# Make the high level targets.                                                 #
#------------------------------------------------------------------------------#

.PHONY: all win32 java jars javadoc doxydoc checkstyle publish \
	clean clean_win32 distclean

all: java

#------------------------------------------------------------------------------#
# Make the native libraries and executables.                                   #
#------------------------------------------------------------------------------#

# Windows 32-Bit Cross-Compile

win32: OS = Windows
win32: LIB_PREFIX =
win32: LIB_SUFFIX = dll
win32: buildandtest.exe

buildandtest.exe: $(BUILD_TOKEN) $(CSRCS) $(CHEADERS)
	$(MAKE_BUILD_AND_TEST)

#------------------------------------------------------------------------------#
# Make the Java classes.                                                       #
#------------------------------------------------------------------------------#

java: $(BUILD_TOKEN)

$(BUILD_TOKEN): $(JSRCS)
	$(ANT) build

#------------------------------------------------------------------------------#
# Make Jars.                                                                   #
#------------------------------------------------------------------------------#

jars: dapper.jar dapper-ex.jar

dapper.jar dapper-ex.jar: $(BUILD_TOKEN)
	$(ANT) jars
	touch $@

#------------------------------------------------------------------------------#
# Make the Javadoc.                                                            #
#------------------------------------------------------------------------------#

javadoc: $(JAVADOC_TOKEN)

$(JAVADOC_TOKEN): $(JSRCS)
	$(ANT) javadoc

#------------------------------------------------------------------------------#
# Make the Doxygen native documentation.                                       #
#------------------------------------------------------------------------------#

doxydoc: $(DOXYDOC_TOKEN)

$(DOXYDOC_TOKEN): $(CSRCS) $(CHEADERS)
	$(MAKE) -C native doxygen
	touch $@

#------------------------------------------------------------------------------#
# Run Checkstyle.                                                              #
#------------------------------------------------------------------------------#

checkstyle: $(CHECKSTYLE_TOKEN)

$(CHECKSTYLE_TOKEN): $(JSRCS) $(CSRCS) $(CHEADERS)
	$(ANT) checkstyle

#------------------------------------------------------------------------------#
# Publish Jars.                                                                #
#------------------------------------------------------------------------------#

publish: $(PUBLISH_TOKEN)

$(PUBLISH_TOKEN): $(JSRCS)
	$(ANT) ivy-publish

#------------------------------------------------------------------------------#
# Clean the distribution.                                                      #
#------------------------------------------------------------------------------#

clean: clean_win32
	rm -rf doxydoc
	rm -f $(DOXYDOC_TOKEN) *.exe
	$(ANT) clean
	$(MAKE) -C native clean

clean_win32: OS = Windows
clean_win32:
	$(MAKE) -C native clean

distclean: clean
	$(ANT) distclean
