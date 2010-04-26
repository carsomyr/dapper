/*
 * Copyright (C) 2008-2010 The Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *     disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *     following disclaimer in the documentation and/or other materials provided with the distribution.
 *   * Neither the name of the author nor the names of any contributors may be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
