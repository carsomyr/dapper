#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""A script for testing basic Dapper functionality.
"""

import subprocess
import sys
import time

from subprocess import Popen

def main():
    """The main method body.
    """

    subprocess.call(["make", "jars"])

    javacmd = ["java", "-ea", "-Xmx128M", "-cp", "dapper.jar"]

    processes = []
    processes.append(Popen(javacmd + ["dapper.ui.FlowManagerDriver", "--port", "12121", "--archive", "dapper-ex.jar", "ex.SimpleTest"]))

    time.sleep(2)

    nclients = 4

    for i in range(nclients):
        processes.append(Popen(javacmd + ["dapper.client.ClientDriver", "--host", "localhost:12121"]))

    while sys.stdin.read(1) != "\n":
        pass

    for i in range(nclients, -1, -1):
        processes[i].kill()
        processes[i].wait()

#

if __name__ == "__main__":
    sys.exit(main())
