cmake_minimum_required(VERSION "2.6")

project("Dapper")

include("../build/conf/common.cmake")

enable_language("CXX")

if(MINGW)
    enable_language("RC")
endif(MINGW)

aux_source_directory("src/dapper" SRC_DAPPER)
aux_source_directory("src/win32" SRC_WIN32)

include_directories("include/dapper" "include/win32")

# Add the 'buildandtest' build target.

add_executable("buildandtest" ${SRC_WIN32} "src/icon/icon.rc")
set_target_properties("buildandtest" PROPERTIES VERSION ${VERSION})
set_target_properties("buildandtest" PROPERTIES COMPILE_FLAGS "-Wall -Werror -pedantic -Wno-long-long")
set_target_properties("buildandtest" PROPERTIES LINK_FLAGS "-Wl,--add-stdcall-alias")
