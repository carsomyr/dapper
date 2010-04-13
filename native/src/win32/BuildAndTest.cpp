/*
 * This file is part of Dapper, the Distributed and Parallel Program Execution Runtime ("this library").
 * 
 * Copyright (C) 2008-2010 The Regents of the University of California
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

// The number of clients to spawn.
static const int NCLIENTS = 4;

PROCESS_INFORMATION BuildAndTest::exec(const TCHAR *cmd) {

    STARTUPINFO si;
    PROCESS_INFORMATION pi;

    ZeroMemory(&si, sizeof(si));
    si.cb = sizeof(si);
    ZeroMemory(&pi, sizeof(pi));

    // Start the child process.
    if (!CreateProcess(NULL, //
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
        printf("Could not create child process '%s'.\n", cmd);
    }

    return pi;
}

int _tmain(int argc, TCHAR *argv[]) {

    PROCESS_INFORMATION antPI = BuildAndTest::exec("java -Xmx128M " //
                "-cp build/ant-launcher.jar " //
                "org.apache.tools.ant.launch.Launcher jars");

    if (antPI.hProcess == NULL) {

        printf("Could not execute Ant.\n");

        return 0;
    }

    WaitForSingleObject(antPI.hProcess, INFINITE);

    CloseHandle(antPI.hProcess);
    CloseHandle(antPI.hThread);

    //

    PROCESS_INFORMATION pis[NCLIENTS + 1];

    pis[0] = BuildAndTest::exec("java -Xmx128M " //
                "-cp dapper.jar dapper.ui.FlowManagerDriver " //
                "--port 12121 " //
                "--archive dapper-ex.jar ex.SimpleTest");

    Sleep(2000);

    for (int i = 1; i <= NCLIENTS; i++) {
        pis[i] = BuildAndTest::exec("java -Xmx128M " //
                    "-cp dapper.jar dapper.client.ClientDriver " //
                    "--host localhost:12121");
    }

    //

    printf("\nPress ENTER to exit this test.\n");

    for (;;) {

        switch (getchar()) {
        case '\n':
        case -1:
            goto end;
        }
    }

    end: for (int i = 0; i <= NCLIENTS; i++) {

        if (pis[i].hProcess != NULL) {

            TerminateProcess(pis[i].hProcess, 0);

            WaitForSingleObject(pis[i].hProcess, INFINITE);

            CloseHandle(pis[i].hProcess);
            CloseHandle(pis[i].hThread);
        }
    }

    return 0;
}
