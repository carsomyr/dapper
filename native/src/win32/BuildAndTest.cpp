/*
 * This file is part of Dapper, the Distributed and Parallel Program Execution Runtime ("this library").
 * 
 * Copyright (C) 2008 Roy Liu, The Regents of the University of California
 * 
 * This library is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library. If not, see
 * http://www.gnu.org/licenses/.
 */

#include <BuildAndTest.hpp>

HANDLE BuildAndTest::exec(const TCHAR *cmd) {

    STARTUPINFO si;
    PROCESS_INFORMATION pi;

    ZeroMemory(&si, sizeof(si));
    si.cb = sizeof(si);
    ZeroMemory(&pi, sizeof(pi));

    // Start the child process.
    if (CreateProcess(NULL, //
            (TCHAR *) cmd, //
            NULL, //
            NULL, //
            FALSE, //
            0, //
            NULL, //
            NULL, //
            &si, //
            &pi) //
    ) {

        return pi.hProcess;

    } else {

        printf("Could not create child process '%s'.\n", cmd);

        return 0;
    }
}

int _tmain(int argc, TCHAR *argv[]) {

    HANDLE antHandle = BuildAndTest::exec("cmd /C java -Xmx128M " //
                "-cp build/ant-launcher.jar " //
                "org.apache.tools.ant.launch.Launcher jars");

    if (!antHandle) {

        printf("Could not execute Ant.\n");

        return 0;
    }

    WaitForSingleObject(antHandle, INFINITE);

    HANDLE handles[5];

    handles[0] = BuildAndTest::exec("cmd /C java -Xmx128M " //
                "-cp dapper.jar dapper.ui.FlowManagerDriver " //
                "--port 12121 " //
                "--archive dapper-ex.jar ex.SimpleTest");

    Sleep(2000);

    for (int i = 1; i < 5; i++) {
        handles[i] = BuildAndTest::exec("cmd /C java -Xmx128M " //
                    "-cp dapper.jar dapper.client.ClientDriver " //
                    "--host localhost:12121");
    }

    printf("\nPress ENTER to exit this test.\n");

    for (;;) {

        switch (getch()) {
        case '\r':
        case -1:
            goto end;
        }
    }

    end: for (int i = 0; i < 5; i++) {

        if (handles[i] != 0) {
            TerminateProcess(handles[i], 0);
        }
    }

    return 0;
}
